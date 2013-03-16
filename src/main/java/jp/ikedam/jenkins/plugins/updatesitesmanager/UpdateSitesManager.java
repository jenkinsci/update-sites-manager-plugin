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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import jenkins.model.Jenkins;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.ForwardToView;
import org.kohsuke.stapler.HttpRedirect;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import hudson.DescriptorExtensionList;
import hudson.Extension;
import hudson.model.ManagementLink;
import hudson.model.Descriptor;
import hudson.model.Descriptor.FormException;
import hudson.model.UpdateSite;
import hudson.search.SearchIndex;
import hudson.search.SearchIndexBuilder;
import hudson.search.SearchableModelObject;
import hudson.search.Search;

/**
 * Pages for manage UpdateSites.
 * New link is added in Manage Jenkins page.
 */
@Extension(ordinal = Integer.MAX_VALUE - 410)   // show just after Manage Plugins (1.489 and later)
public class UpdateSitesManager extends ManagementLink
{
    private Logger LOGGER = Logger.getLogger(UpdateSitesManager.class.getName());
    /**
     * Return the name of the link shown in Manage Jenkins page.
     * 
     * @return the name of the link.
     * @see hudson.model.Action#getDisplayName()
     */
    @Override
    public String getDisplayName()
    {
        return Messages.UpdateSitesManager_DisplayName();
    }
    
    /**
     * Return the icon file name shown in Manage Jenkins page.
     * 
     * @return icon file name
     * @see hudson.model.ManagementLink#getIconFileName()
     */
    @Override
    public String getIconFileName()
    {
        // TODO: create a more appropriate icon.
        return "plugin.gif";
    }
    
    /**
     * Return the description shown in Manage Jenkins page.
     * 
     * @return the description
     * @see hudson.model.ManagementLink#getDescription()
     */
    @Override
    public String getDescription()
    {
        return Messages.UpdateSitesManager_Description();
    }
    
    /**
     * Return the name used in url.
     * 
     * @return the name used in url.
     * @see hudson.model.ManagementLink#getUrlName()
     */
    @Override
    public String getUrlName()
    {
        return "updatesites";
    }
    
    protected DescribedUpdateSite getUpdateSiteWithDescriptor(UpdateSite updateSite)
    {
        if(updateSite == null)
        {
            return null;
        }
        else if(updateSite.getClass().equals(UpdateSite.class))
        {
            // If input is an instance of UpdateSite,
            // wrap it and add Descriptor to handle in a view.
            // For UpdateSite does not provide appropriate
            // methods for update configuration, subclass of UpdateSite
            // cannot be handled.
            return new DescribedUpdateSiteWrapper(updateSite);
        }
        else if(updateSite instanceof DescribedUpdateSite)
        {
            return (DescribedUpdateSite)updateSite;
        }
        
        // No appropriate UpdateSite with Descriptor.
        LOGGER.warning(String.format("Cannot handle with UpdateSiteManager: %s", updateSite.getClass().getName()));
        return null;
    }
    
    public Iterable<DescribedUpdateSite> getUpdateSiteList()
    {
        return Iterables.filter(
            Iterables.transform(
                Jenkins.getInstance().getUpdateCenter().getSites(),
                new Function<UpdateSite, DescribedUpdateSite>()
                {
                    @Override
                    public DescribedUpdateSite apply(UpdateSite input)
                    {
                        return getUpdateSiteWithDescriptor(input);
                    }
                }
            ),
            new Predicate<DescribedUpdateSite>()
            {
                @Override
                public boolean apply(DescribedUpdateSite input)
                {
                    return (input != null);
                }
            }
        );
    }
    
    public DescriptorExtensionList<DescribedUpdateSite,Descriptor<DescribedUpdateSite>> getUpdateSiteDescriptorList()
    {
        return DescribedUpdateSite.all();
    }
    
    public HttpResponse do_add(StaplerRequest req, StaplerResponse rsp) throws IOException, UnsupportedEncodingException, ServletException, FormException
    {
        Descriptor<DescribedUpdateSite> descriptor = null;
        
        DescriptorExtensionList<DescribedUpdateSite,Descriptor<DescribedUpdateSite>>
        descriptorList = getUpdateSiteDescriptorList();
        
        if(req.getParameter("stapler-class") != null)
        {
            try
            {
                @SuppressWarnings("unchecked")
                Class<DescribedUpdateSite.DescriptorImpl> clazz = (Class<DescribedUpdateSite.DescriptorImpl>)Class.forName(req.getParameter("stapler-class"));
                descriptor = Jenkins.getInstance().getDescriptorByType(clazz);
            }
            catch(ClassCastException e)
            {
                LOGGER.log(Level.WARNING, String.format("Bad stapler-class: %s", req.getParameter("stapler-class")), e);
            }
            catch (ClassNotFoundException e)
            {
                LOGGER.log(Level.WARNING, String.format("Bad stapler-class: %s", req.getParameter("stapler-class")), e);
            }
            
        }
        
        if(descriptor == null && descriptorList.size() == 1)
        {
            descriptor = descriptorList.get(0);
        }
            
        if(descriptor == null)
        {
            return new ForwardToView(this, "newSiteSelect.jelly");
        }
        
        if(!"POST".equals(req.getMethod()))
        {
            DescribedUpdateSite newSite = descriptor.newInstance(req, new JSONObject());
            
            req.setAttribute("instance", newSite);
            return new ForwardToView(this, "newSite.jelly");
        }
        
        JSONObject json = req.getSubmittedForm();
        DescribedUpdateSite newSite = descriptor.newInstance(req, json);
        
        for(UpdateSite site: Jenkins.getInstance().getUpdateCenter().getSites())
        {
            if(site.getId().equals(newSite.getId()))
            {
                // ID is duplicated.
                throw new FormException("id is duplicated", "id");
            }
        }
        
        Jenkins.getInstance().getUpdateCenter().getSites().add(newSite);
        Jenkins.getInstance().getUpdateCenter().save();
        
        return new HttpRedirect(".");
    }
    
    public Object getDynamic(String token)
    {
        for(DescribedUpdateSite site: getUpdateSiteList())
        {
            if(token.equals(site.getId()))
            {
                return site;
            }
        }
        return null;
    }
    
}
