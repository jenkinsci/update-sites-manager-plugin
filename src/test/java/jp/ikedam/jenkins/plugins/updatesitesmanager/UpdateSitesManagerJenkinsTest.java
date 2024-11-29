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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.ManagementLink;
import hudson.model.UpdateSite;
import java.io.IOException;
import java.util.List;
import org.htmlunit.FailingHttpStatusCodeException;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;
import org.xml.sax.SAXException;

/**
 * Tests for UpdateSitesManager, concerned with Jenkins.
 */
public class UpdateSitesManagerJenkinsTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void shouldExistsLinkToManager() throws IOException, SAXException {
        try (JenkinsRule.WebClient wc = j.createWebClient()) {
            HtmlPage managementPage = wc.goTo("manage");

            assertThat(
                    "Link to UpdateSitesManager does not exists in Manage Jenkins page",
                    managementPage.getAnchorByHref(UpdateSitesManager.URL),
                    notNullValue());
        }
    }

    @Test
    public void shouldNotReturnAnyUCIfEmptyList() {
        j.getInstance().getUpdateCenter().getSites().clear();

        UpdateSitesManager manager =
                j.getInstance().getExtensionList(ManagementLink.class).get(UpdateSitesManager.class);
        assertNotNull(manager);
        assertThat("managed", manager.getManagedUpdateSiteList(), hasSize(0));
        assertThat("not managed", manager.getNotManagedUpdateSiteList(), hasSize(0));
    }

    @Test
    public void shouldReturnBothManagedUnmanaged() throws IOException, SAXException {
        UpdateSite site1 = new UpdateSite("test1", "http://example.com/test/update-center.json");
        UpdateSite site2 =
                new ManagedUpdateSite("test2", "http://example.com/test2/update-center.json", false, null, null, false);
        // Multiple update site.
        j.getInstance().getUpdateCenter().getSites().clear();
        j.getInstance().getUpdateCenter().getSites().add(site1);
        j.getInstance().getUpdateCenter().getSites().add(site2);

        UpdateSitesManager manager =
                j.getInstance().getExtensionList(ManagementLink.class).get(UpdateSitesManager.class);
        assertNotNull(manager);
        assertThat("managed", manager.getManagedUpdateSiteList(), hasSize(1));
        assertThat("not managed", manager.getNotManagedUpdateSiteList(), hasSize(1));
    }

    public static <T extends Describable<T>> boolean containsDescriptor(
            List<? extends Descriptor<T>> descriptorList, Class<? extends T> clazz) {
        for (Descriptor<T> descriptor : descriptorList) {
            if (descriptor.clazz.equals(clazz)) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void testGetUpdateSiteDescriptorList() {
        UpdateSitesManager target = new UpdateSitesManager();

        List<DescribedUpdateSiteDescriptor> availableDescriptorList = target.getUpdateSiteDescriptorList();

        // availableDescriptorList must contain ManagedUpdateSite.
        assertTrue(
                "ManagedUpdateSite is filtered", containsDescriptor(availableDescriptorList, ManagedUpdateSite.class));
    }

    @Test
    public void shouldSubmitSites() throws Exception {
        UpdateSite site1 = new UpdateSite("test1", "http://example.com/test/update-center.json");
        UpdateSite site2 =
                new ManagedUpdateSite("test2", "http://example.com/test2/update-center.json", false, null, "", false);
        j.getInstance().getUpdateCenter().getSites().clear();
        j.getInstance().getUpdateCenter().getSites().add(site1);
        j.getInstance().getUpdateCenter().getSites().add(site2);

        try (JenkinsRule.WebClient wc = j.createWebClient()) {
            wc.getOptions().setPrintContentOnFailingStatusCode(false);

            // configure
            HtmlForm form = wc.goTo(UpdateSitesManager.URL).getFormByName("sitesForm");
            HtmlPage submit = j.submit(form);

            assertThat(
                    "should be on manage url after submit",
                    submit.getWebResponse().getWebRequest().getUrl().toString(),
                    endsWith("manage/"));

            assertThat("should see all sites", j.getInstance().getUpdateCenter().getSites(), hasSize(2));
            j.assertEqualDataBoundBeans(
                    site2, j.getInstance().getUpdateCenter().getSites().get(1));
        }
    }

    @Test
    public void shouldReturn400OnBlankId() throws Exception {
        UpdateSite site =
                new ManagedUpdateSite(" ", "http://example.com/test2/update-center.json", false, null, null, false);
        j.getInstance().getUpdateCenter().getSites().clear();
        j.getInstance().getUpdateCenter().getSites().add(site);

        try (JenkinsRule.WebClient wc = j.createWebClient()) {
            HtmlForm form = wc.goTo(UpdateSitesManager.URL).getFormByName("sitesForm");
            Exception ex = assertThrows(FailingHttpStatusCodeException.class, () -> j.submit(form));
            assertThat(ex.getMessage(), containsString("400"));
        }
    }

    @Test
    public void shouldReturn400OnDuplicatedId() throws Exception {
        UpdateSite site1 = new UpdateSite("test1", "http://example.com/test/update-center.json");
        UpdateSite site2 =
                new ManagedUpdateSite("test1", "http://example.com/test2/update-center.json", false, null, null, false);
        j.getInstance().getUpdateCenter().getSites().clear();
        j.getInstance().getUpdateCenter().getSites().add(site1);
        j.getInstance().getUpdateCenter().getSites().add(site2);

        try (JenkinsRule.WebClient wc = j.createWebClient()) {
            HtmlForm form = wc.goTo(UpdateSitesManager.URL).getFormByName("sitesForm");
            Exception ex = assertThrows(FailingHttpStatusCodeException.class, () -> j.submit(form));
            assertThat(ex.getMessage(), containsString("400"));
        }
    }

    @Test
    @LocalData
    public void testPrivilege() throws Exception {
        try (JenkinsRule.WebClient wcUser = j.createWebClient()) {
            wcUser.login("user", "user");
            wcUser.getOptions().setPrintContentOnFailingStatusCode(false);

            Exception ex = assertThrows(
                    FailingHttpStatusCodeException.class,
                    () -> wcUser.goTo("%s/update".formatted(UpdateSitesManager.URL)));
            assertThat(ex.getMessage(), containsString("403"));
        }
    }
}
