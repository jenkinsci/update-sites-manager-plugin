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

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.ForwardToView;
import org.kohsuke.stapler.HttpRedirect;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import hudson.Extension;
import hudson.model.ManagementLink;
import hudson.model.Descriptor;
import hudson.model.Descriptor.FormException;
import hudson.model.UpdateSite;

/**
 * Pages for manage UpdateSites.
 * 
 * Provides following pages.
 * <ul>
 *   <li>Manage UpdateSites, shown in Manage Jenkins page</li>
 *   <li>Add New Site</li>
 * </ul>
 * 
 * And provides access to each updatesite configuration page.
 */
@Extension(ordinal = Integer.MAX_VALUE - 410)   // show just after Manage Plugins (1.489 and later)
public class UpdateSitesManager extends ManagementLink
{
    private Logger LOGGER = Logger.getLogger(UpdateSitesManager.class.getName());
    
    public final static String URL = "updatesites";
    
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
        return URL;
    }
    
    /**
     * Return DescribedUpdateSite for an UpdateSite.
     * 
     * DescribedUpdateSite is an UpdateSite with Descriptor for used in views.
     * If passed UpdateSite is an instance of DescribedUpdateSite, simply returns itself.
     * If passed UpdateSite is not an instance of DescribedUpdateSite, wrap it with DescribedUpdateSiteWrapper.
     * 
     * @param updateSite an UpdateSite
     * @return a DescribedUpdateSite
     */
    protected DescribedUpdateSite getUpdateSiteWithDescriptor(UpdateSite updateSite)
    {
        if(updateSite == null)
        {
            return null;
        }
        
        if(updateSite instanceof DescribedUpdateSite)
        {
            return (DescribedUpdateSite)updateSite;
        }
        
        return new DescribedUpdateSiteWrapper(updateSite);
    }
    
    /**
     * Return a list of UpdateSites registered in Jenkins.
     * 
     * Each UpdateSites is wrapped to DescribedUpdateSite.
     * 
     * @return a list of UpdateSites
     */
    public Iterable<DescribedUpdateSite> getUpdateSiteList()
    {
        return Iterables.transform(
            Jenkins.getInstance().getUpdateCenter().getSites(),
            new Function<UpdateSite, DescribedUpdateSite>()
            {
                @Override
                public DescribedUpdateSite apply(UpdateSite input)
                {
                    return getUpdateSiteWithDescriptor(input);
                }
            }
        );
    }
    
    /**
     * Returns all the registered DescribedUpdateSite.
     * 
     * Only returns DescribedUpdateSite that can be used to create a new site.
     * 
     * @return a list of Desctiptor of DescribedUpdateSite.
     */
    public List<DescribedUpdateSite.Descriptor> getUpdateSiteDescriptorList()
    {
        return Lists.newArrayList(Iterables.filter(
                DescribedUpdateSite.all(),
                new Predicate<DescribedUpdateSite.Descriptor>()
                {
                    @Override
                    public boolean apply(DescribedUpdateSite.Descriptor descriptor)
                    {
                        return descriptor.canCreateNewSite();
                    }
                }
        ));
    }
    
    /**
     * Create a new UpdateSite.
     * 
     * Screen transition is as following:
     * <ol>
     *   <li>Click "Add New Site" in /updatesites/.<li>
     *   <li>newSiteSelect.jelly: Select UpdateSite type. This page is shown if there are more than one DescribedUpdateSite is available.
     *   <li>newSite.jelly: Fill Form.
     *   <li>Post Form. /updatesites/ are shown again.
     * </ol>
     * 
     * @param req
     * @param rsp
     * @return
     * @throws IOException
     * @throws UnsupportedEncodingException
     * @throws ServletException
     * @throws FormException
     */
    public HttpResponse do_add(StaplerRequest req, StaplerResponse rsp) throws IOException, UnsupportedEncodingException, ServletException, FormException
    {
        // Only administrator can create a new site.
        Jenkins.getInstance().checkPermission(Jenkins.ADMINISTER);
        
        Descriptor<DescribedUpdateSite> descriptor = null;
        
        List<DescribedUpdateSite.Descriptor> descriptorList = getUpdateSiteDescriptorList();
        
        if(req.getParameter("stapler-class") != null)
        {
            try
            {
                @SuppressWarnings("unchecked")
                Class<DescribedUpdateSite.Descriptor> clazz = (Class<DescribedUpdateSite.Descriptor>)Class.forName(req.getParameter("stapler-class"));
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
            // There are more than one DescribedUpdateSite is available,
            // so let the user to select which to create.
            return new ForwardToView(this, "newSiteSelect.jelly");
        }
        
        if(!"POST".equals(req.getMethod()))
        {
            // Show form.
            req.setAttribute("instance", null);
            req.setAttribute("descriptor", descriptor);
            return new ForwardToView(this, "newSite.jelly");
        }
        
        // Create a new site.
        JSONObject json = req.getSubmittedForm();
        DescribedUpdateSite newSite = descriptor.newInstance(req, json);
        
        // Check ID filled
        if(StringUtils.isBlank(newSite.getId()))
        {
            // ID is empty.
            throw new FormException("id is empty", "id");
        }
        
        // Check ID duplication.
        for(UpdateSite site: Jenkins.getInstance().getUpdateCenter().getSites())
        {
            if(site.getId().equals(newSite.getId()))
            {
                // ID is duplicated.
                throw new FormException("id is duplicated", "id");
            }
        }
        
        Jenkins.getInstance().getUpdateCenter().getSites().add(newSite.getUpdateSite());
        Jenkins.getInstance().getUpdateCenter().save();
        
        return new HttpRedirect("."); // ${rootURL}/updatesites/
    }
    
    /**
     * Allow users to access each UpdateSite page.
     * 
     * @param token the token from URL. that is, ${rootURL}/updatesites/${toekn}/blahblah
     * @return
     */
    public DescribedUpdateSite getDynamic(String token)
    {
        for(UpdateSite site: Jenkins.getInstance().getUpdateCenter().getSites())
        {
            if(token.equals(site.getId()))
            {
                return getUpdateSiteWithDescriptor(site);
            }
        }
        return null;
    }
}
