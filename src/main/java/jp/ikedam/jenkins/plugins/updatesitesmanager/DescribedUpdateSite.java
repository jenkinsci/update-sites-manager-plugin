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
import java.io.Serializable;

import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.ForwardToView;
import org.kohsuke.stapler.HttpRedirect;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;

import jenkins.model.Jenkins;
import jenkins.model.ModelObjectWithContextMenu;
import hudson.DescriptorExtensionList;
import hudson.ExtensionPoint;
import hudson.Util;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.UpdateSite;
import hudson.model.Descriptor.FormException;

/**
 * Base for UpdateSites that have Descriptor.
 */
@SuppressWarnings("serial")
abstract public class DescribedUpdateSite extends UpdateSite implements Describable<DescribedUpdateSite>, ExtensionPoint, Serializable, ModelObjectWithContextMenu
{
    public DescribedUpdateSite(String id, String url)
    {
        super(id, url);
    }
    
    public UpdateSite getUpdateSite()
    {
        return this;
    }
    
    @Override
    public String getDisplayName()
    {
        return getId();
    }
    
    public String getPageUrl()
    {
        return String.format("%s/", Util.rawEncode(getId()));
    }
    
    public boolean isEditable()
    {
        return true;
    }
    
    public boolean isDisabled()
    {
        return false;
    }
    
    public String getNote()
    {
        return "";
    }
    
    @RequirePOST
    public HttpResponse doConfigure(StaplerRequest req, StaplerResponse rsp) throws FormException, ServletException, IOException
    {
        Jenkins.getInstance().checkPermission(Jenkins.ADMINISTER);
        
        if(!isEditable())
        {
            throw new FormException("this site cannot be edited", "id");
        }
        
        JSONObject json = req.getSubmittedForm();
        DescribedUpdateSite newSite = req.bindJSON(getClass(), json);
        
        if(!newSite.getId().equals(getId()))
        {
            for(UpdateSite site: Jenkins.getInstance().getUpdateCenter().getSites())
            {
                if(site.getId().equals(newSite.getId()))
                {
                    // ID is duplicated.
                    throw new FormException("id is duplicated", "id");
                }
            }
        }
        
        Jenkins.getInstance().getUpdateCenter().getSites().replace(
                getUpdateSite(), 
                newSite.getUpdateSite()
        );
        Jenkins.getInstance().getUpdateCenter().save();
        
        return new HttpRedirect("..");
    }
    
    public HttpResponse doDelete(StaplerRequest req, StaplerResponse rsp) throws FormException, IOException
    {
        Jenkins.getInstance().checkPermission(Jenkins.ADMINISTER);
        
        if(!isEditable())
        {
            throw new FormException("this site cannot be edited", "id");
        }
        
        if(!"POST".equals(req.getMethod()))
        {
            return new ForwardToView(this, "delete.jelly");
        }
        
        Jenkins.getInstance().getUpdateCenter().getSites().remove(this);
        Jenkins.getInstance().getUpdateCenter().save();
        
        return new HttpRedirect("..");
    }
    
    @Override
    public ContextMenu doContextMenu(StaplerRequest request, StaplerResponse response) throws Exception {
        return new ContextMenu().from(this,request,response);
    }
    
    static public DescriptorExtensionList<DescribedUpdateSite,Descriptor<DescribedUpdateSite>> all()
    {
        return Jenkins.getInstance().<DescribedUpdateSite,Descriptor<DescribedUpdateSite>>getDescriptorList(DescribedUpdateSite.class);
    }
    
    
    /**
     * @return
     * @see hudson.model.Describable#getDescriptor()
     */
    @Override
    @SuppressWarnings("unchecked")
    public Descriptor<DescribedUpdateSite> getDescriptor()
    {
        return Jenkins.getInstance().getDescriptorOrDie(getClass());
    }
    
    static public abstract class DescriptorImpl extends Descriptor<DescribedUpdateSite>
    {
        abstract public String getDescription();
        
        // TODO: id, urlのチェック処理
    }
}
