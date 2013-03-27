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

import net.sf.json.JSONObject;

import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.TestExtension;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.xml.sax.SAXException;

import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
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
        
        HtmlPage updateSitesPage = wc.goTo(UpdateSitesManager.URL);
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
        
        HtmlPage updateSitesPage = wc.goTo(UpdateSitesManager.URL);
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
            assertNull("Null should be returned for unknown site", target.getDynamic("nosite"));
            assertNull("Null should be returned for null", target.getDynamic(null));
            assertNull("Null should be returned for empty", target.getDynamic(""));
        }
    }
    
    public void testDo_add() throws IOException, SAXException
    {
        UpdateSitesManager target = new UpdateSitesManager();
        assertEquals("This test must run with one UpdateSite Descriptors registered.", 1, target.getUpdateSiteDescriptorList().size());
        
        WebClient wc = new WebClient();
        
        HtmlPage updateSitesPage = wc.goTo(UpdateSitesManager.URL);
        
        // follow "Add New Site"
        HtmlAnchor addNewSiteLink = (HtmlAnchor)updateSitesPage.getElementById("add-new-site-link");
        HtmlPage addNewSitePage = wc.getPage(updateSitesPage.getFullyQualifiedUrl(addNewSiteLink.getHrefAttribute()));
        
        // Verify form
        HtmlForm addNewSiteForm = addNewSitePage.getFormByName("addSiteForm");
        assertNotNull("Form page is not shown", addNewSiteForm);
    }
    
    public void testDo_addWithMultiUpdateSites() throws Exception
    {
        // initialize UpdateSites.
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
        
        UpdateSitesManager target = new UpdateSitesManager();
        assertTrue("This test must run with more than one UpdateSite Descriptors registered.", 1 < target.getUpdateSiteDescriptorList().size());
        
        WebClient wc = new WebClient();
        
        HtmlPage updateSitesPage = wc.goTo(UpdateSitesManager.URL);
        
        // follow "Add New Site"
        HtmlAnchor addNewSiteLink = (HtmlAnchor)updateSitesPage.getElementById("add-new-site-link");
        HtmlPage addNewSitePage1 = wc.getPage(updateSitesPage.getFullyQualifiedUrl(addNewSiteLink.getHrefAttribute()));
        
        // Verify Form
        HtmlForm selectNewSiteForm = addNewSitePage1.getFormByName("selectNewSiteForm");
        assertNotNull("A page to select a site type is not shown", selectNewSiteForm);
        
        // Sending form without selecting a site results returning to the same page.
        addNewSitePage1 = submit(selectNewSiteForm);
        selectNewSiteForm = addNewSitePage1.getFormByName("selectNewSiteForm");
        assertNotNull("Submit without selecting a site must results in returning to the same page.", selectNewSiteForm);
        
        // Sending form with selecting an invalid site results returning to the same page.
        addNewSitePage1 = submit(selectNewSiteForm);
        {
            HtmlInput input = selectNewSiteForm.getInputByValue(Jenkins.getInstance().getDescriptor(TestUpdateSite.class).getClass().getName());
            input.setValueAttribute("foobar");
            input.setChecked(true);
        }
        selectNewSiteForm = addNewSitePage1.getFormByName("selectNewSiteForm");
        assertNotNull("Submit with selecting a invalid value must results in returning to the same page.", selectNewSiteForm);
        
        selectNewSiteForm.getInputByValue(Jenkins.getInstance().getDescriptor(TestUpdateSite.class).getClass().getName()).setChecked(true);
        HtmlPage addNewSitePage2 = submit(selectNewSiteForm);
        
        // Verify form
        HtmlForm addNewSiteForm = addNewSitePage2.getFormByName("addSiteForm");
        assertNotNull("Form page is not shown", addNewSiteForm);
        
        // Sites number before posting the form.
        int before = Jenkins.getInstance().getUpdateCenter().getSites().size();
        
        // Post form.
        String id = "newsite1";
        String url = "http://localhost/update-center.json";
        addNewSiteForm.getInputByName("_.id").setValueAttribute(id);
        addNewSiteForm.getInputByName("_.url").setValueAttribute(url);
        updateSitesPage = submit(addNewSiteForm);
        
        // Verify new site is added.
        assertEquals("No site is added", before + 1, Jenkins.getInstance().getUpdateCenter().getSites().size());
        UpdateSite site = null;
        
        for(UpdateSite test: Jenkins.getInstance().getUpdateCenter().getSites())
        {
            if(id.equals(test.getId()))
            {
                site = test;
                break;
            }
        }
        
        assertNotNull("Added site not found", site);
        assertEquals("Unexpected site type", TestUpdateSite.class, site.getClass());
        assertEquals("URL is not configured", url, site.getUrl());
        
        // added site is in list.
        assertNotNull("Added site must be listed", updateSitesPage.getAnchorByHref(((TestUpdateSite)site).getPageUrl()));
    }
    
    public void testDo_addError() throws Exception
    {
        // initialize UpdateSites.
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
        
        UpdateSitesManager target = new UpdateSitesManager();
        assertTrue("This test must run with more than one UpdateSite Descriptors registered.", 1 < target.getUpdateSiteDescriptorList().size());
        
        WebClient wc = new WebClient();
        //wc.setPrintContentOnFailingStatusCode(false);
        
        // Post without id.
        {
            HtmlPage updateSitesPage = wc.goTo(UpdateSitesManager.URL);
            
            // follow "Add New Site"
            HtmlAnchor addNewSiteLink = (HtmlAnchor)updateSitesPage.getElementById("add-new-site-link");
            HtmlPage addNewSitePage1 = wc.getPage(updateSitesPage.getFullyQualifiedUrl(addNewSiteLink.getHrefAttribute()));
            
            // Verify Form
            HtmlForm selectNewSiteForm = addNewSitePage1.getFormByName("selectNewSiteForm");
            assertNotNull("A page to select a site type is not shown", selectNewSiteForm);
            
            selectNewSiteForm.getInputByValue(Jenkins.getInstance().getDescriptor(SingletonUpdateSite.class).getClass().getName()).setChecked(true);
            HtmlPage addNewSitePage2 = submit(selectNewSiteForm);
            
            // Verify form
            HtmlForm addNewSiteForm = addNewSitePage2.getFormByName("addSiteForm");
            assertNotNull("Form page is not shown", addNewSiteForm);
            
            // Post without id
            String url = "http://localhost/update-center.json";
            
            addNewSiteForm.getInputByName("_.id").setValueAttribute("");
            addNewSiteForm.getInputByName("_.url").setValueAttribute(url);
            try
            {
                submit(addNewSiteForm);
                assertTrue("Must return an exception", false);
            }
            catch(Exception e) // TODO: replace with appropriate Exception
            {
                System.out.println(e.getClass().getName());
                System.out.println(e);
            }
        }
        
        // Post with an duplicated id.
        {
            HtmlPage updateSitesPage = wc.goTo(UpdateSitesManager.URL);
            
            // follow "Add New Site"
            HtmlAnchor addNewSiteLink = (HtmlAnchor)updateSitesPage.getElementById("add-new-site-link");
            HtmlPage addNewSitePage1 = wc.getPage(updateSitesPage.getFullyQualifiedUrl(addNewSiteLink.getHrefAttribute()));
            
            // Verify Form
            HtmlForm selectNewSiteForm = addNewSitePage1.getFormByName("selectNewSiteForm");
            assertNotNull("A page to select a site type is not shown", selectNewSiteForm);
            
            selectNewSiteForm.getInputByValue(Jenkins.getInstance().getDescriptor(SingletonUpdateSite.class).getClass().getName()).setChecked(true);
            HtmlPage addNewSitePage2 = submit(selectNewSiteForm);
            
            // Verify form
            HtmlForm addNewSiteForm = addNewSitePage2.getFormByName("addSiteForm");
            assertNotNull("Form page is not shown", addNewSiteForm);
            
            // Post without id
            String url = "http://localhost/update-center.json";
            addNewSiteForm.getInputByName("_.id").setValueAttribute(site1.getId());
            addNewSiteForm.getInputByName("_.url").setValueAttribute(url);
            try
            {
                submit(addNewSiteForm);
                assertTrue("Must return an exception", false);
            }
            catch(Exception e) // TODO: replace with appropriate Exception
            {
                System.out.println(e.getClass().getName());
                System.out.println(e);
            }
        }
    }
    
    static public class TestUpdateSite extends DescribedUpdateSite
    {
        private static final long serialVersionUID = -6892029314071071061L;
        
        @DataBoundConstructor
        public TestUpdateSite(String id, String url)
        {
            super(id, url);
        }
        
        @TestExtension("testDo_addWithMultiUpdateSites")
        static public class DescriptorImpl extends DescribedUpdateSite.Descriptor
        {
            @Override
            public String getDescription()
            {
                return "TestUpdateSite";
            }
            
            @Override
            public String getDisplayName()
            {
                return "Provided for testing purpose";
            }
        }
    }
    
    static public class SingletonUpdateSite extends DescribedUpdateSite
    {
        private static final long serialVersionUID = -6892029314071071061L;
        
        private String id;
        
        @Override
        public String getId()
        {
            return id;
        }
        
        public void setId(String id)
        {
            this.id = id;
        }
        
        private String url;
        
        @Override
        public String getUrl()
        {
            return url;
        }
        
        public void setUrl(String url)
        {
            this.url = url;
        }
        
        public SingletonUpdateSite(String id, String url)
        {
            super(null, null);
            this.id = id;
            this.url = url;
        }
        
        static private SingletonUpdateSite INSTANCE = new SingletonUpdateSite(null, null);
        static public SingletonUpdateSite getInstance()
        {
            return INSTANCE;
        }
        
        @TestExtension("testDo_addError")
        static public class DescriptorImpl extends DescribedUpdateSite.Descriptor
        {
            @Override
            public String getDescription()
            {
                return "TestUpdateSite";
            }
            
            @Override
            public String getDisplayName()
            {
                return "Provided for testing purpose";
            }
            
            @Override
            public DescribedUpdateSite newInstance(StaplerRequest req,
                    JSONObject formData)
                    throws hudson.model.Descriptor.FormException
            {
                return getInstance();
            }
        }
    }
}
