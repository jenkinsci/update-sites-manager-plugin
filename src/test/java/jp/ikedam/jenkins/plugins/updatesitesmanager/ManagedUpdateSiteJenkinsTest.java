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

import hudson.util.FormValidation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.GeneralSecurityException;


import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.jvnet.hudson.test.HudsonTestCase;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Tests for ManagedUpdateSite, concerned with Jenkins.
 */
public class ManagedUpdateSiteJenkinsTest extends HudsonTestCase
{
    static public class TestManagedUpdateSite extends ManagedUpdateSite
    {
        private static final long serialVersionUID = -6888318503867286760L;
        
        public TestManagedUpdateSite(
                String id,
                String url,
                boolean useCaCertificate,
                String caCertificate,
                String note,
                boolean disabled
        )
        {
            super(id, url, useCaCertificate, caCertificate, note, disabled);
        }
        
        private FormValidation doPostBackResult = null;
        
        public FormValidation getDoPostBackResult()
        {
            return doPostBackResult;
        }
        
        
        public void setDoPostBackResult(FormValidation doPostBackResult)
        {
            this.doPostBackResult = doPostBackResult;
        }
        
        @Override
        public FormValidation doPostBack(StaplerRequest req)
                throws IOException, GeneralSecurityException
        {
            FormValidation ret = super.doPostBack(req);
            setDoPostBackResult(ret);
            return ret;
        }
    }
    
    
    private File getResource(String name) throws URISyntaxException, FileNotFoundException
    {
        String filename = String.format("%s/%s", StringUtils.join(getClass().getName().split("\\."), "/"), name);
        URL url = ClassLoader.getSystemResource(filename);
        if(url == null)
        {
            throw new FileNotFoundException(String.format("Not found: %s", filename));
        }
        return new File(url.toURI());
    }
    
    private ManagedUpdateSite.DescriptorImpl getDescriptor()
    {
        return (ManagedUpdateSite.DescriptorImpl)new ManagedUpdateSite(null, null, false, null, null, false).getDescriptor();
    }
    
    public void testDescriptorDoCheckCaCertificate() throws FileNotFoundException, IOException, URISyntaxException
    {
        ManagedUpdateSite.DescriptorImpl descriptor = getDescriptor();
        String caCertificate = FileUtils.readFileToString(getResource("caCertificate.crt"));
        
        {
            assertEquals(
                    "Always ok if certificate is disabled",
                    FormValidation.Kind.OK,
                    descriptor.doCheckCaCertificate(false, null).kind
            );
        }
        
        {
            assertEquals(
                    "OK for valid certificate",
                    FormValidation.Kind.OK,
                    descriptor.doCheckCaCertificate(true, caCertificate).kind
            );
        }
    }
    
    public void testDescriptorDoCheckCaCertificateError()
    {
        ManagedUpdateSite.DescriptorImpl descriptor = getDescriptor();
        
        {
            assertEquals(
                    "Null certificate",
                    FormValidation.Kind.ERROR,
                    descriptor.doCheckCaCertificate(true, null).kind
            );
        }
        
        {
            assertEquals(
                    "Empty certificate",
                    FormValidation.Kind.ERROR,
                    descriptor.doCheckCaCertificate(true, "").kind
            );
        }
        
        {
            assertEquals(
                    "Blank certificate",
                    FormValidation.Kind.ERROR,
                    descriptor.doCheckCaCertificate(true, "  ").kind
            );
        }
        
        {
            assertEquals(
                    "Invalid certificate",
                    FormValidation.Kind.ERROR,
                    descriptor.doCheckCaCertificate(true, "hogehogehogehoge").kind
            );
        }
    }
}
