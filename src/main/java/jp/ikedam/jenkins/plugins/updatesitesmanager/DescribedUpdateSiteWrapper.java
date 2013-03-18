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

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.model.UpdateSite;

/**
 * Wrapper for {@link UpdateSite}.
 * 
 * UpdateSite is not designed to manage in configuration pages,
 * that is, UpdateSite has no {@link Descriptor} nor {@link DataBoundConstructor}.
 * This class wraps UpdateSite and provides functions to be managed in views.
 */

public class DescribedUpdateSiteWrapper extends DescribedUpdateSite
{
    private static final long serialVersionUID = -381591686508537334L;
    
    /**
     * Constructor for new UpdateSite
     * 
     * @param id
     * @param url
     */
    @DataBoundConstructor
    public DescribedUpdateSiteWrapper(String id, String url)
    {
        super(null, null);
        this.updateSite = new UpdateSite(id, url);
    }
    
    /**
     * Constructor from existing UpdateSite
     * 
     * @param updateSite
     */
    public DescribedUpdateSiteWrapper(UpdateSite updateSite)
    {
        super(null, null);
        this.updateSite = updateSite;
    }
    
    private UpdateSite updateSite;
    
    /**
     * Returns UpdateSite to register to Jenkins.
     * 
     * @return UpdateSite
     * @see jp.ikedam.jenkins.plugins.updatesitesmanager.DescribedUpdateSite#getUpdateSite()
     */
    @Override
    public UpdateSite getUpdateSite()
    {
        return updateSite;
    }
    
    /**
     * Returns site id.
     * 
     * @return site id
     * @see hudson.model.UpdateSite#getId()
     */
    @Override
    public String getId()
    {
        return getUpdateSite().getId();
    }
    
    /**
     * Returns the URL of the site.
     * 
     * @return the URL of the site
     * @see hudson.model.UpdateSite#getUrl()
     */
    @Override
    public String getUrl()
    {
        return getUpdateSite().getUrl();
    }
    
    /**
     * Returns whether this UpdateSite is editable by UpdateSiteManager
     * 
     * Editable only when wrapped object is an instance of UpdateSite.
     * Subclass of UpdateSite is not allowed, for there may be fields UpdateSiteManager does not know.
     * The site whose id is "default" is also ineditable, for it must be managed in UpdateCenter.
     * 
     * @return whether this UpdateSite is editable
     * @see jp.ikedam.jenkins.plugins.updatesitesmanager.DescribedUpdateSite#isEditable()
     */
    @Override
    public boolean isEditable()
    {
        return updateSite.getClass().equals(UpdateSite.class)
                && !"default".equals(getId());
    }
    
    /**
     * Descriptor for this class.
     */
    @Extension
    static public class DescriptorImpl extends DescribedUpdateSite.Descriptor
    {
        static DescriptorImpl DESCRIPTOR = new DescriptorImpl();
        
        /**
         * Returns the displaying name. 
         * 
         * This is not used for this class, for canCreateNewSite returns false.
         * 
         * @return the displaying name
         * @see hudson.model.Descriptor#getDisplayName()
         */
        @Override
        public String getDisplayName()
        {
            return Messages.DescribedUpdateSiteWrapper_DisplayName();
        }
        
        /**
         * Returns the description of this class.
         * 
         * This is not used for this class, for canCreateNewSite returns false.
         * 
         * @return the description of this class
         * @see jp.ikedam.jenkins.plugins.updatesitesmanager.DescribedUpdateSite.Descriptor#getDescription()
         */
        @Override
        public String getDescription()
        {
            return Messages.DescribedUpdateSiteWrapper_Description();
        }
        
        /**
         * Returns whether this class can be used to create new UpdateSite.
         * 
         * Always returns false for this class is used only for managing existing UpdateSite. 
         * 
         * @return false
         * @see jp.ikedam.jenkins.plugins.updatesitesmanager.DescribedUpdateSite.Descriptor#canCreateNewSite()
         */
        @Override
        public boolean canCreateNewSite()
        {
            return false;
        }
    };
}
