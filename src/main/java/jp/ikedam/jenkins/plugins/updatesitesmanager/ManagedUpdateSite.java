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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import hudson.Extension;
import hudson.ProxyConfiguration;
import hudson.Util;
import hudson.model.Item;
import hudson.model.Queue;
import hudson.model.UpdateSite;
import hudson.model.queue.Tasks;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import jenkins.util.JSONSignatureValidator;
import jp.ikedam.jenkins.plugins.updatesitesmanager.internal.ExtendedCertJsonSignValidator;

import org.acegisecurity.Authentication;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

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

    private String credentialsId;

    /**
     * Returns the credentials to use with the update site URL.
     * 
     * @return the credentials identifier
     */
    public String getCredentialsId() {
        return credentialsId;
    }

    /**
     * Returns the credentials to use with the update site URL.
     * 
     * @return the credentials identifier
     */
    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = Util.fixEmptyAndTrim(credentialsId);
    }

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
     *
     * @param caCertificate CA certificate
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
     * Create a new instance
     * 
     * @param id id for the site
     * @param url URL for the site
     * @param useCaCertificate whether to use a specified CA certificate
     * @param caCertificate CA certificate to verify the site
     * @param note note
     * @param disabled {@code true} to disable the site
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
        this.caCertificate = useCaCertificate?StringUtils.trim(caCertificate):null;
        this.note = note;
        this.disabled = disabled;
    }

    @Override
    public FormValidation updateDirectlyNow(boolean signatureCheck) throws IOException {
        URL url = new URL(getUrl());

        URLConnection con = ProxyConfiguration.open(url);
        if (con instanceof HttpURLConnection) {
            // prevent problems from misbehaving plugins disabling redirects by
            // default
            ((HttpURLConnection) con).setInstanceFollowRedirects(true);
            UpdateSiteManagerHelper.addAuthorisationHeader(con, credentialsId);
        }

        String json;
        try (InputStream is = con.getInputStream()) {
            String jsonp = IOUtils.toString(is, "UTF-8");
            int start = jsonp.indexOf('{');
            int end = jsonp.lastIndexOf('}');
            if (start >= 0 && end > start) {
                json = jsonp.substring(start, end + 1);
            } else {
                throw new IOException("Could not find JSON in " + url);
            }
        }

        Method method;
        try {
            method = UpdateSite.class.getDeclaredMethod("updateData", String.class, boolean.class);
            method.setAccessible(true);
            return (FormValidation) method.invoke(this, json, false);
        } catch (SecurityException | ReflectiveOperationException e) {
            throw new IOException(e);
        }
    }

    /**
     * Verifier for the signature of downloaded update-center.json.
     *
     * @return JSONSignatureValidator object with additional cert as anchor if enabled
     */
    @Nonnull
    @Override
    protected JSONSignatureValidator getJsonSignatureValidator(@CheckForNull String name)
    {
        if (isUseCaCertificate()) {
            return new ExtendedCertJsonSignValidator(name, getCaCertificate());
        } else {
            return super.getJsonSignatureValidator(name);
        }
    }
    
    /**
     * Descriptor for this class.
     */
    @Extension
    static public class DescriptorImpl extends DescribedUpdateSiteDescriptopr
    {
        /**
         * Returns the kind name of this UpdateSite.
         * 
         * shown when select UpdateSite to create.
         * 
         * @return the kind name of the site
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
         * @param useCaCertificate {@code true} to use a specific CA certificate
         * @param caCertificate the CA certificate
         * @return FormValidation the validation result
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

        public FormValidation doCheckCredentialsId(@CheckForNull @AncestorInPath Item item,
                                                   @QueryParameter String credentialsId,
                                                   @QueryParameter String serverUrl) {
            if (item == null) {
                if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                    return FormValidation.ok();
                }
            } else if (!item.hasPermission(Item.EXTENDED_READ) && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
                return FormValidation.ok();
            }
            if (!StringUtils.isBlank(credentialsId)) {
                List<DomainRequirement> domainRequirement = URIRequirementBuilder.fromUri(serverUrl).build();
                if (CredentialsProvider.listCredentials(StandardUsernameCredentials.class, item, getAuthentication(item), domainRequirement, CredentialsMatchers.withId(credentialsId)).isEmpty()) {
                    return FormValidation.error("invalid credentials");
                }
            }
            return FormValidation.ok();
        }

        public ListBoxModel doFillCredentialsIdItems(final @AncestorInPath Item item,
                                                     @QueryParameter String credentialsId,
                                                     final @QueryParameter String url) {
            StandardListBoxModel result = new StandardListBoxModel();

            credentialsId = StringUtils.trimToEmpty(credentialsId);
            if (item == null) {
                if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                    return result.includeCurrentValue(credentialsId);
                }
            } else {
                if (!item.hasPermission(Item.EXTENDED_READ) && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
                    return result.includeCurrentValue(credentialsId);
                }
            }

            Authentication authentication = getAuthentication(item);
            List<DomainRequirement> build = URIRequirementBuilder.fromUri(url).build();
            CredentialsMatcher always = CredentialsMatchers.always();
            Class<StandardUsernameCredentials> type = StandardUsernameCredentials.class;

            result.includeEmptyValue();
            if (item != null) {
                result.includeMatchingAs(authentication, item, type, build, always);
            } else {
                result.includeMatchingAs(authentication, Jenkins.get(), type, build, always);
            }
            return result;
        }

        protected Authentication getAuthentication(Item item) {
            return item instanceof Queue.Task ? Tasks.getAuthenticationOf((Queue.Task) item) : ACL.SYSTEM;
        }
    }
}
