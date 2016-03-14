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

import static org.apache.commons.lang.StringUtils.isEmpty;
import hudson.Extension;
import hudson.util.FormValidation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;

import jenkins.util.JSONSignatureValidator;
import jp.ikedam.jenkins.plugins.updatesitesmanager.internal.ExtendedCertJsonSignValidator;
import jp.ikedam.jenkins.plugins.updatesitesmanager.internal.NoCertJsonSignValidator;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

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
 * The CA certificate is written as additional trust anchor dynamically
 */
public class ManagedUpdateSite extends DescribedUpdateSite
{
    private static Logger LOGGER = Logger.getLogger(ManagedUpdateSite.class.getName());
    
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
    
    private boolean skipSignatureCheck;
    
    /**
     * Returns whether to skip signature checks
     * 
     * @return whether to skip signature checks
     */
    public boolean isSkipSignatureCheck()
    {
        return skipSignatureCheck;
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
            boolean disabled,
            boolean skipSignatureCheck
            
    )
    {
        super(id, url);
        this.caCertificate = useCaCertificate?StringUtils.trim(caCertificate):null;
        this.note = note;
        this.disabled = disabled;
        this.skipSignatureCheck = skipSignatureCheck;
    }
    
    /**
     * Verifier for the signature of downloaded update-center.json.
     *
     * @return JSONSignatureValidator object with additional cert as anchor if enabled
     */
    @Nonnull
    @Override
    protected JSONSignatureValidator getJsonSignatureValidator()
    {
        if(isSkipSignatureCheck()) {
          return new NoCertJsonSignValidator(getId(), getCaCertificate());
        } else if (isUseCaCertificate()) {
            return new ExtendedCertJsonSignValidator(getId(), getCaCertificate());
        } else {
            return super.getJsonSignatureValidator();
        }
    }
    
    /**
     * Descriptor for this class.
     */
    @Extension
    static public class DescriptorImpl extends DescribedUpdateSiteDescriptopr
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
         * Returns whether the certificate is valid.
         * 
         * @return FormValidation object
         */
        public FormValidation doCheckCaCertificate(
                @QueryParameter boolean useCaCertificate,
                @QueryParameter String caCertificate
        )
        {
            if(!useCaCertificate)
            {
                return FormValidation.ok();
            }
            
            if(StringUtils.isBlank(caCertificate))
            {
                return FormValidation.error(Messages.ManagedUpdateSite_caCertificate_required());
            }
            
            CertificateFactory cf = null;
            try
            {
                cf = CertificateFactory.getInstance("X509");
            }
            catch(CertificateException e)
            {
                LOGGER.log(Level.WARNING, "Failed to retrieve CertificateFactory for X509", e);
            }
            
            if(cf != null)
            {
                try
                {
                    cf.generateCertificate(new ByteArrayInputStream(StringUtils.trim(caCertificate).getBytes("UTF-8")));
                }
                catch(CertificateException e)
                {
                    return FormValidation.error(Messages.ManagedUpdateSite_caCertificate_invalid(e.getLocalizedMessage()));
                }
                catch(UnsupportedEncodingException e)
                {
                    LOGGER.log(Level.WARNING, "Failed to decode Certificate", e);
                }
            }
            
            return FormValidation.ok();
        }
        
        public FormValidation doCheckConnection(
              @QueryParameter String id,
              @QueryParameter String url,
              @QueryParameter boolean useCaCertificate,
              @QueryParameter String caCertificate,
              @QueryParameter String note,
              @QueryParameter boolean disabled,
              @QueryParameter boolean skipSignatureCheck
              ) throws IOException, ServletException {
          if(isEmpty(id)) return FormValidation.error("id not set");
          if(isEmpty(url)) return FormValidation.error("url not set");
          ManagedUpdateSite site = new ManagedUpdateSite(id, url, useCaCertificate, caCertificate, note, disabled, skipSignatureCheck);
          site.doInvalidateData();
          FormValidation result = site.updateDirectlyNow(false);
          if(!result.equals(FormValidation.ok())) return result;
          result = site.doVerifySignature();
          if(result.equals(FormValidation.ok())) return FormValidation.okWithMarkup("Connection tested successfully...");
          return result;
        }
        
    }
}
