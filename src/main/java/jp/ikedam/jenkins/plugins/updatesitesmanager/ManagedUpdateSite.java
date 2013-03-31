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
 * Extended UpdateSite to be managed in UpdateSitesManager.
 * 
 * ManagedUpdateSite provides following features.
 * <ul>
 *   <li>can switch enabled/disabled.</li>
 *   <li>have a note field.</li>
 *   <li>can set a CA certificate for the signature of the site.</li>
 * </ul>
 * 
 * The CA certificate is written to a temporary file under ${JENKINS_HOME}/update-center-rootCAs/ .
 * I think it is a better implementation to override UpdateSite#verifySignature,
 * but it is private method.
 * Re-implementing doPostBack results to re-implement whole the UpdateSite.
 */
public class ManagedUpdateSite extends DescribedUpdateSite
{
    private static final long serialVersionUID = -714713790690982048L;
    
    private String caCertificate;
    
    /**
     * Returns the CA certificate to verify the signature.
     * 
     * This is useful when the UpdateSite is signed with a self-signed private key.
     * 
     * @return the CA certificate
     */
    public String getCaCertificate()
    {
        return caCertificate;
    }
    
    /**
     * Set the CA certificate to verify the signature.
     * 
     * Mainly for testing Purpose.
     */
    public void setCaCertificate(String caCertificate)
    {
        this.caCertificate = caCertificate;
    }
    
    /**
     * Returns whether to use CA certificate.
     * 
     * @return whether to use CA certificate
     */
    public boolean isUseCaCertificate()
    {
        return getCaCertificate() != null;
    }
    
    private boolean disabled;
    
    /**
     * Returns whether this site is disabled.
     * 
     * When disabled, plugins in this site gets unavailable.
     * 
     * @return the whether this site is disabled
     */
    @Override
    public boolean isDisabled()
    {
        return disabled;
    }
    
    private String note;
    
    /**
     * Returns the note
     * 
     * Note is only used for the displaying purpose.
     * 
     * @return the note
     */
    @Override
    public String getNote()
    {
        return note;
    }
    
    /**
     * Returns the path to the CA certificate file.
     * 
     * The file to store the CA certificate temporary.
     * 
     * @return the path to the CA certificate file
     */
    protected File getCaCertificateFile()
    {
        File caCertificateDir = new File(Jenkins.getInstance().getRootDir(), "update-center-rootCAs");
        if(!caCertificateDir.exists())
        {
            assert(caCertificateDir.mkdir());
        }
        
        return new File(caCertificateDir, String.format("tmp-ManageUpdateSite-%s.crt", getId()));
    }
    
    /**
     * Create a new instance
     * 
     * @param id
     * @param url
     * @param useCaCertificate
     * @param caCertificate
     * @param note
     * @param disabled
     */
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
    
    /**
     * Process update-center.json.
     * 
     * If a CA certificate is set, write it to file and make it ready to verify the signature.
     * 
     * @param req
     * @return FormValidation object
     * @throws IOException
     * @throws GeneralSecurityException
     * @see hudson.model.UpdateSite#doPostBack(org.kohsuke.stapler.StaplerRequest)
     */
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
    
    /**
     * Uses custom CA certificate to process update-center.json.
     * 
     * To avoid having the certificate file overwritten by another thread,
     * this method is synchronized.
     * 
     * @param req
     * @return FormValidation object
     * @throws IOException
     * @throws GeneralSecurityException
     */
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
    
    /**
     * Verify the signature of downloaded update-center.json.
     * 
     * @return FormValidation object.
     * @throws IOException
     * @see hudson.model.UpdateSite#doVerifySignature()
     */
    @Override
    public FormValidation doVerifySignature() throws IOException
    {
        if(isUseCaCertificate())
        {
            return doVerifySignatureWithCaCertificate();
        }
        
        return super.doVerifySignature();
    }
    
    /**
     * Verify the signature with custom CA certificate.
     * 
     * To avoid having the certificate file overwritten by another thread,
     * this method is synchronized.
     * 
     * @return FormValidation object.
     * @throws IOException
     */
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
    
    /**
     * Descriptor for this class.
     */
    @Extension
    static public class DescriptorImpl extends DescribedUpdateSite.Descriptor
    {
        /**
         * Returns the name of this UpdateSite.
         * 
         * shown when select UpdateSite to create.
         * 
         * @return
         * @see hudson.model.Descriptor#getDisplayName()
         */
        @Override
        public String getDisplayName()
        {
            return Messages.ManagedUpdateSite_DisplayName();
        }
        
        /**
         * Returns the description of this UpdateSite.
         * 
         * shown when select UpdateSite to create.
         * 
         * @return
         * @see jp.ikedam.jenkins.plugins.updatesitesmanager.DescribedUpdateSite.Descriptor#getDescription()
         */
        @Override
        public String getDescription()
        {
            return Messages.ManagedUpdateSite_Description();
        }
    };
}
