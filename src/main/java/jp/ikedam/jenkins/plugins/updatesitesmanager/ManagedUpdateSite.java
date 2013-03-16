/**
 * 
 */
package jp.ikedam.jenkins.plugins.updatesitesmanager;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

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
        if(isDisabled())
        {
            return FormValidation.ok();
        }
        
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
    
    @Override
    public List<Plugin> getAvailables()
    {
        if(isDisabled())
        {
            return new ArrayList<Plugin>(0);
        }
        return super.getAvailables();
    }
    
    @Override
    public List<Plugin> getUpdates()
    {
        if(isDisabled())
        {
            return new ArrayList<Plugin>(0);
        }
        return super.getUpdates();
    }
    
    @Override
    public boolean isDue()
    {
        if(isDisabled())
        {
            return false;
        }
        return super.isDue();
    }
    
    @Override
    public boolean hasUpdates()
    {
        if(isDisabled())
        {
            return false;
        }
        return super.hasUpdates();
    }
    
    @Extension
    static public class DescriptorImpl extends DescribedUpdateSite.DescriptorImpl
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
