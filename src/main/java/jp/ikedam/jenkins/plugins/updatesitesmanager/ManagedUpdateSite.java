/*
 * The MIT License
 * 
 * Copyright (c) 2013 IKEDA Yasuyuki
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package jp.ikedam.jenkins.plugins.updatesitesmanager;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;

import jenkins.model.Jenkins;

import hudson.Extension;
import hudson.util.FormValidation;

import org.apache.commons.io.FileUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * @author yasuke
 *
 */
public class ManagedUpdateSite extends DescribedUpdateSite
{
    private static final long serialVersionUID = -714713790690982048L;
    
    private String caCertificate;
    
    /**
     * @return the caCertificate
     */
    public String getCaCertificate()
    {
        return caCertificate;
    }
    
    public boolean isUseCaCertificate()
    {
        return getCaCertificate() != null;
    }
    
    private boolean disabled;
    
    /**
     * @return the disabled
     */
    @Override
    public boolean isDisabled()
    {
        return disabled;
    }
    
    private String note;
    
    /**
     * @return the note
     */
    @Override
    public String getNote()
    {
        return note;
    }
    
    protected File getCaCertificateFile()
    {
        File caCertificateDir = new File(Jenkins.getInstance().getRootDir(), "update-center-rootCAs");
        if(!caCertificateDir.exists())
        {
            assert(caCertificateDir.mkdir());
        }
        
        return new File(caCertificateDir, String.format("tmp-ManageUpdateSite-%s.crt", getId()));
    }
    
    @DataBoundConstructor
    public ManagedUpdateSite(
            String id,
            String url,
            boolean useCaCertificate,
            String caCertificate,
            String note,
            boolean disabled
    )
    {
        super(id, url);
        this.caCertificate = useCaCertificate?caCertificate:null;
        this.note = note;
        this.disabled = disabled;
    }
    
    @DataBoundConstructor
    public ManagedUpdateSite()
    {
        this("", "", false, null, "", false);
    }
    
    @Override
    public FormValidation doPostBack(StaplerRequest req) throws IOException,
            GeneralSecurityException
    {
        if(isUseCaCertificate())
        {
            return doPostBackWithCaCertificate(req);
        }
        
        return super.doPostBack(req);
    }
    
    private synchronized FormValidation doPostBackWithCaCertificate(StaplerRequest req)
            throws IOException, GeneralSecurityException
    {
        File caFile = getCaCertificateFile();
        try
        {
            FileUtils.writeStringToFile(getCaCertificateFile(), getCaCertificate());
            return super.doPostBack(req);
        }
        finally
        {
            if(caFile.exists())
            {
                caFile.delete();
            }
        }
    }
    
    @Override
    public FormValidation doVerifySignature() throws IOException
    {
        if(isUseCaCertificate())
        {
            return doVerifySignatureWithCaCertificate();
        }
        
        return super.doVerifySignature();
    }
    
    private synchronized FormValidation doVerifySignatureWithCaCertificate() throws IOException
    {
        File caFile = getCaCertificateFile();
        try
        {
            FileUtils.writeStringToFile(getCaCertificateFile(), getCaCertificate());
            return super.doVerifySignature();
        }
        finally
        {
            if(caFile.exists())
            {
                caFile.delete();
            }
        }
    }
    
    @Extension
    static public class DescriptorImpl extends DescribedUpdateSite.Descriptor
    {
        @Override
        public String getDisplayName()
        {
            return Messages.ManagedUpdateSite_DisplayName();
        }
        
        @Override
        public String getDescription()
        {
            return Messages.ManagedUpdateSite_Description();
        }
    };
}
