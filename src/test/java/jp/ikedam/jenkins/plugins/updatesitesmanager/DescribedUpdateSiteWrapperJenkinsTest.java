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

import hudson.model.UpdateSite;
import jenkins.model.Jenkins;

import org.jvnet.hudson.test.HudsonTestCase;

import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

/**
 * Tests for DescribedUpdateSiteWrapper, concerned with Jenkins.
 *
 */
public class DescribedUpdateSiteWrapperJenkinsTest extends HudsonTestCase
{
    public void testDoConfigure() throws Exception
    {
        UpdateSite target = new UpdateSite(
            "test1",
            "http://example.com/test/update-center.json"
        );
        
        // Multiple update site.
        Jenkins.getInstance().getUpdateCenter().getSites().clear();
        Jenkins.getInstance().getUpdateCenter().getSites().add(target);
        
        String originalId = target.getId();
        
        WebClient wc = new WebClient();
        
        HtmlPage editSitePage = wc.goTo(String.format("%s/%s", UpdateSitesManager.URL, target.getId()));
        
        HtmlForm editSiteForm = editSitePage.getFormByName("editSiteForm");
        assertNotNull("There must be editSiteForm", editSiteForm);
        
        String newId = "newId";
        String newUrl = "http://localhost/update-center.json";
        editSiteForm.getInputByName("_.id").setValueAttribute(newId);
        editSiteForm.getInputByName("_.url").setValueAttribute(newUrl);
        submit(editSiteForm);
        
        UpdateSite site = null;
        for(UpdateSite s: Jenkins.getInstance().getUpdateCenter().getSites())
        {
            if(newId.equals(s.getId()))
            {
                site = s;
            }
            assertFalse("id must be updated(old one must not remain)", originalId.equals(s.getId()));
        }
        assertNotNull("id must be updated", site);
        assertEquals("url must be updated", newUrl, site.getUrl());
    }
    
    public void testDoDelete() throws Exception
    {
        UpdateSite target = new UpdateSite(
            "test1",
            "http://example.com/test/update-center.json"
        );
        
        Jenkins.getInstance().getUpdateCenter().getSites().clear();
        Jenkins.getInstance().getUpdateCenter().getSites().add(target);
        
        int initialSize = Jenkins.getInstance().getUpdateCenter().getSites().size();
        
        WebClient wc = new WebClient();
        
        HtmlPage deleteSitePage = wc.goTo(String.format("%s/%s/delete", UpdateSitesManager.URL, target.getId()));
        assertEquals("UpdateSite must not be deleted yet.", initialSize, Jenkins.getInstance().getUpdateCenter().getSites().size());
        
        HtmlForm deleteSiteForm = deleteSitePage.getFormByName("deleteSiteForm");
        assertNotNull("There must be deleteSiteForm", deleteSiteForm);
            
        submit(deleteSiteForm);
        assertEquals("UpdateSite must be deleted.", initialSize - 1, Jenkins.getInstance().getUpdateCenter().getSites().size());
    }
}
