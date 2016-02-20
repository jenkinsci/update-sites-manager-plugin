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

import hudson.DescriptorExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.UpdateSite;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Base for UpdateSite that have Descriptor.
 */
abstract public class DescribedUpdateSite extends UpdateSite implements Describable<DescribedUpdateSite>, ExtensionPoint
{
    /**
     * Constructor
     * 
     * @param id
     * @param url
     */
    public DescribedUpdateSite(String id, String url)
    {
        super(StringUtils.trim(id), StringUtils.trim(url));
    }
    
    /**
     * Returns whether this UpdateSite is disabled.
     * 
     * Returning true makes Jenkins ignore the plugins in this UpdateSite.
     * 
     * @return whether this UpdateSite is disabled.
     */
    public boolean isDisabled()
    {
        return false;
    }
    
    /**
     * Returns note
     * 
     * Provided for users to note about this UpdateSite.
     * Used only for displaying purpose.
     * 
     * @return note
     */
    public String getNote()
    {
        return "";
    }
    
    /**
     * Returns a list of plugins that should be shown in the "available" tab.
     * 
     * Returns nothing when disabled.
     * 
     * @return
     * @see hudson.model.UpdateSite#getAvailables()
     */
    @Override
    public List<Plugin> getAvailables()
    {
        if(isDisabled())
        {
            return new ArrayList<Plugin>(0);
        }
        return super.getAvailables();
    }
    
    /**
     * Returns the list of plugins that are updates to currently installed ones.
     * 
     * Returns nothing when disabled.
     * 
     * @return
     * @see hudson.model.UpdateSite#getUpdates()
     */
    @Override
    public List<Plugin> getUpdates()
    {
        if(isDisabled())
        {
            return new ArrayList<Plugin>(0);
        }
        return super.getUpdates();
    }
    
    /**
     * Returns true if it's time for us to check for new version.
     * 
     * Always returns false when disabled.
     * 
     * @return
     * @see hudson.model.UpdateSite#isDue()
     */
    @Override
    public boolean isDue()
    {
        if(isDisabled())
        {
            return false;
        }
        return super.isDue();
    }
    
    /**
     * Does any of the plugin has updates? 
     * 
     * Always returns false when disabled.
     * 
     * @return any of the plugin has updates?
     * @see hudson.model.UpdateSite#hasUpdates()
     */
    @Override
    public boolean hasUpdates()
    {
        if(isDisabled())
        {
            return false;
        }
        return super.hasUpdates();
    }
    
    /**
     * Returns all DescribedUpdateSite classes registered to Jenkins.
     * 
     * @return the list of Descriptor of DescribedUpdateSite subclasses.
     */
    static public DescriptorExtensionList<DescribedUpdateSite, DescribedUpdateSiteDescriptopr> all()
    {
        return Jenkins.getActiveInstance().getDescriptorList(DescribedUpdateSite.class);
    }
    
    
    /**
     * Returns the descriptor for this class.
     * 
     * @return the descriptor for this class.
     * @see hudson.model.Describable#getDescriptor()
     */
    @Override
    public DescribedUpdateSiteDescriptopr getDescriptor()
    {
        return (DescribedUpdateSiteDescriptopr) Jenkins.getActiveInstance().getDescriptorOrDie(getClass());
    }
}
