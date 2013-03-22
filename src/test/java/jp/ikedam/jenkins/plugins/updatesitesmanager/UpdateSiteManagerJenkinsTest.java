/**
 * 
 */
package jp.ikedam.jenkins.plugins.updatesitesmanager;

import hudson.model.Describable;
import hudson.model.UpdateSite;
import hudson.model.Descriptor;

import java.io.IOException;
import java.util.List;

import jenkins.model.Jenkins;

import org.jvnet.hudson.test.HudsonTestCase;
import org.xml.sax.SAXException;

import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.google.common.collect.Lists;


/**
 * Tests for UpdateSiteManager, concerned with Jenkins.
 *
 */
public class UpdateSiteManagerJenkinsTest extends HudsonTestCase
{
    // Test the link to UpdateSiteManager exists.
    public void testManagementLink() throws IOException, SAXException
    {
        WebClient wc = new WebClient();
        
        // make output quiet.
        // comment out here if an unexpected behavior occurs.
        wc.setPrintContentOnFailingStatusCode(false);
        
        HtmlPage managementPage = wc.goTo("/manage");
        assertNotNull(
                "Link to UpdateSiteManager does not exists in Manage Jenkins page",
                managementPage.getAnchorByHref(UpdateSitesManager.URL)
        );
    }
    
    // Test No update site is exists.
    public void testNoUpdateSitesInList() throws IOException, SAXException
    {
        // No update site.
        Jenkins.getInstance().getUpdateCenter().getSites().clear();
        
        WebClient wc = new WebClient();
        
        // make output quiet.
        // comment out here if an unexpected behavior occurs.
        wc.setPrintContentOnFailingStatusCode(false);
        
        HtmlPage updateSitesPage = wc.goTo(String.format("/%s", UpdateSitesManager.URL));
        HtmlElement table = updateSitesPage.getElementById("update-sites");
        DomNodeList<HtmlElement> trs = table.getElementsByTagName("tr");
        assertEquals(
            "There are entries even when no updatesites available.",
            1,
            trs.size()
        );
    }
    
    // Test Multiple update site is exists.
    public void testManyUpdateSitesInList() throws IOException, SAXException
    {
        UpdateSite site1 = new UpdateSite(
            "test1",
            "http://example.com/test/update-center.json"
        );
        UpdateSite site2 = new ManagedUpdateSite(
            "test2",
            "http://example.com/test2/update-center.json",
            false,
            null,
            null,
            false
        );
        // Multiple update site.
        Jenkins.getInstance().getUpdateCenter().getSites().clear();
        Jenkins.getInstance().getUpdateCenter().getSites().add(site1);
        Jenkins.getInstance().getUpdateCenter().getSites().add(site2);
        
        WebClient wc = new WebClient();
        
        // make output quiet.
        // comment out here if an unexpected behavior occurs.
        wc.setPrintContentOnFailingStatusCode(false);
        
        HtmlPage updateSitesPage = wc.goTo(String.format("/%s", UpdateSitesManager.URL));
        HtmlElement table = updateSitesPage.getElementById("update-sites");
        DomNodeList<HtmlElement> trs = table.getElementsByTagName("tr");
        assertEquals(
            "Unexpected rows in updatesites list.",
            3,
            trs.size()
        );
        
        assertNotNull("Link to site1 does not exists", updateSitesPage.getAnchorByHref(String.format("%s/", site1.getId())));
        assertNotNull("Link to site2 does not exists", updateSitesPage.getAnchorByHref(String.format("%s/", site2.getId())));
    }
    
    public void testGetUpdateSiteList() throws IOException
    {
        UpdateSitesManager target = new UpdateSitesManager();
        // no update sites
        {
            // No update site.
            Jenkins.getInstance().getUpdateCenter().getSites().clear();
            
            List<DescribedUpdateSite> sites = Lists.newArrayList(target.getUpdateSiteList());
            assertEquals("No sites should be return!", 0, sites.size());
        }
        
        // many update sites
        {
            UpdateSite site1 = new UpdateSite(
                "test1",
                "http://example.com/test/update-center.json"
            );
            UpdateSite site2 = new ManagedUpdateSite(
                "test2",
                "http://example.com/test2/update-center.json",
                false,
                null,
                null,
                false
            );
            Jenkins.getInstance().getUpdateCenter().getSites().clear();
            Jenkins.getInstance().getUpdateCenter().getSites().add(site1);
            Jenkins.getInstance().getUpdateCenter().getSites().add(site2);
            
            List<DescribedUpdateSite> sites = Lists.newArrayList(target.getUpdateSiteList());
            assertEquals("Returned sites are too short", 2, sites.size());
            assertEquals("site1 does not match", site1.getId(), sites.get(0).getId());
            assertEquals("site1 does not match", site1.getUrl(), sites.get(0).getUrl());
            assertSame("site2 does not match", site2, sites.get(1));
        }
    }
    
    static public <T extends Describable<T>> boolean containsDescriptor(List<? extends Descriptor<T>> descriptorList, Class<? extends T> clazz)
    {
        for(Descriptor<T> descriptor: descriptorList)
        {
            if(descriptor.clazz.equals(clazz))
            {
                return true;
            }
        }
        return false;
    }
    
    public void testGetUpdateSiteDescriptorList()
    {
        UpdateSitesManager target = new UpdateSitesManager();
        
        List<DescribedUpdateSite.Descriptor> allDescriptorList = 
                DescribedUpdateSite.all();
        
        List<DescribedUpdateSite.Descriptor> availableDescriptorList =
                target.getUpdateSiteDescriptorList();
        
        // allDescriptorList must contain DescribedUpdateSiteWrapper.
        assertTrue(
                "DescribedUpdateSiteWrapper is not registered",
                containsDescriptor(allDescriptorList, DescribedUpdateSiteWrapper.class)
        );
        
        // availableDescriptorList must contain DescribedUpdateSiteWrapper.
        assertFalse(
                "DescribedUpdateSiteWrapper is not filtered",
                containsDescriptor(availableDescriptorList, DescribedUpdateSiteWrapper.class)
        );
        
        // availableDescriptorList must contain ManagedUpdateSite.
        assertTrue(
                "ManagedUpdateSite is filtered",
                containsDescriptor(availableDescriptorList, ManagedUpdateSite.class)
        );
    }
    
    public void testGetDynamic() throws IOException
    {
        UpdateSitesManager target = new UpdateSitesManager();
        // no update sites
        {
            // No update site.
            Jenkins.getInstance().getUpdateCenter().getSites().clear();
            
            assertNull("Null should be return for unknown site", target.getDynamic("nosite"));
            assertNull("Null should be return for null", target.getDynamic(null));
            assertNull("Null should be return for empty", target.getDynamic(""));
        }
        
        // many update sites
        {
            UpdateSite site1 = new UpdateSite(
                "test1",
                "http://example.com/test/update-center.json"
            );
            UpdateSite site2 = new ManagedUpdateSite(
                "test2",
                "http://example.com/test2/update-center.json",
                false,
                null,
                null,
                false
            );
            Jenkins.getInstance().getUpdateCenter().getSites().clear();
            Jenkins.getInstance().getUpdateCenter().getSites().add(site1);
            Jenkins.getInstance().getUpdateCenter().getSites().add(site2);
            
            assertSame("should return specified site", site1, target.getDynamic("test1").getUpdateSite());
            assertSame("should return specified site", site2, target.getDynamic("test2").getUpdateSite());
            assertNull("Null should be return for unknown site", target.getDynamic("nosite"));
            assertNull("Null should be return for null", target.getDynamic(null));
            assertNull("Null should be return for empty", target.getDynamic(""));
        }
    }
    
    public void testDo_add()
    {
        // TODO
    }
    
}
