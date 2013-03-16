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

import hudson.model.Descriptor;
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
    @DataBoundConstructor
    public DescribedUpdateSiteWrapper(String id, String url)
    {
        super(null, null);
        this.updateSite = new UpdateSite(id, url);
    }
    
    public DescribedUpdateSiteWrapper(UpdateSite updateSite)
    {
        super(null, null);
        this.updateSite = updateSite;
    }
    
    private UpdateSite updateSite;
    
    @Override
    public UpdateSite getUpdateSite()
    {
        return updateSite;
    }
    
    @Override
    public String getId()
    {
        return getUpdateSite().getId();
    }
    
    @Override
    public String getUrl()
    {
        return getUpdateSite().getUrl();
    }
    
    @Override
    public boolean isEditable()
    {
        return updateSite.getClass().equals(UpdateSite.class)
                && !"default".equals(getId());
    }
    
    @Override
    public Descriptor<DescribedUpdateSite> getDescriptor()
    {
        return DescriptorImpl.DESCRIPTOR;
    }
    
    static public class DescriptorImpl extends DescribedUpdateSite.DescriptorImpl
    {
        static DescriptorImpl DESCRIPTOR = new DescriptorImpl();
        
        @Override
        public String getDisplayName()
        {
            return Messages.DescribedUpdateSiteWrapper_DisplayName();
        }
        
        @Override
        public String getDescription()
        {
            return Messages.DescribedUpdateSiteWrapper_Description();
        }
    };
}
