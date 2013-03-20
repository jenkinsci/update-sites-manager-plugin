/**
 * 
 */
package jp.ikedam.jenkins.plugins.updatesitesmanager;

import hudson.model.UpdateSite;

import java.io.IOException;

import jenkins.model.Jenkins;

import org.jvnet.hudson.test.HudsonTestCase;
import org.xml.sax.SAXException;

import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;


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
}
