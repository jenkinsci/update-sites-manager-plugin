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


import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import hudson.model.UpdateSite;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;
import org.jvnet.hudson.test.recipes.LocalData;
import org.kohsuke.stapler.DataBoundConstructor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;

/**
 * Tests for DescribedUpdateSite, concerned with Jenkins.
 */
public class DescribedUpdateSiteJenkinsTest {

    @Rule
    public JenkinsRule jRule = new JenkinsRule();

    @Rule
    public ExpectedException ex = ExpectedException.none();

    public static class DescribedUpdateSiteForConfigureTest extends DescribedUpdateSite {
        private String testValue;

        public String getTestValue() {
            return testValue;
        }

        @DataBoundConstructor
        public DescribedUpdateSiteForConfigureTest(String id, String url, String testValue) {
            super(id, url);
            this.testValue = testValue;
        }

        @TestExtension("shouldShowAllDescribedSitesAsManaged")
        static public class DescriptorImpl extends Descriptor {
            @Override
            public String getDisplayName() {
                return "DescribedUpdateSiteForConfigureTest";
            }
        }
    }

    @Test
    public void shouldShowAllDescribedSitesAsManaged() throws Exception {
        String existingId = "test1";
        UpdateSite site1 = new UpdateSite(
                existingId,
                "http://example.com/test/update-center.json"
        );
        DescribedUpdateSiteForConfigureTest target = new DescribedUpdateSiteForConfigureTest(
                "test2",
                "http://example.com/test2/update-center.json",
                "Some value"
        );

        // Multiple update site.
        jRule.getInstance().getUpdateCenter().getSites().clear();
        jRule.getInstance().getUpdateCenter().getSites().add(site1);
        jRule.getInstance().getUpdateCenter().getSites().add(target);

        UpdateSitesManager manager = new UpdateSitesManager();

        assertThat("should register described UC only", manager.getManagedUpdateSiteList(), hasSize(1));
        jRule.assertEqualDataBoundBeans(target, manager.getManagedUpdateSiteList().get(0));
    }

    @Test
    @LocalData
    public void testPrivilege() throws Exception {
        UpdateSite site = new UpdateSite(
                "test1",
                "http://example.com/test/update-center.json"
        );
        jRule.getInstance().getUpdateCenter().getSites().add(site);

        JenkinsRule.WebClient wcAdmin = jRule.createWebClient();
        wcAdmin.setPrintContentOnFailingStatusCode(false);
        wcAdmin.login("admin", "admin");

        JenkinsRule.WebClient wcUser = jRule.createWebClient();
        wcUser.setPrintContentOnFailingStatusCode(false);
        wcUser.login("user", "user");

        // configure
        HtmlForm form = wcAdmin.goTo(UpdateSitesManager.URL)
                .getFormByName("sitesForm");
        jRule.submit(form);

        ex.expect(FailingHttpStatusCodeException.class);
        ex.expectMessage(containsString("403"));
        wcUser.goTo(UpdateSitesManager.URL);
    }

    @Test
    public void shouldRedirectOnGetReqOfUpdate() throws Exception {
        JenkinsRule.WebClient wc = jRule.createWebClient();
        wc.setPrintContentOnFailingStatusCode(false);

        HtmlPage htmlPage = wc.goTo(UpdateSitesManager.URL + "/update");
        assertThat("should redirect",
                htmlPage.getWebResponse().getUrl().toString(), endsWith(UpdateSitesManager.URL + "/"));
    }
}
