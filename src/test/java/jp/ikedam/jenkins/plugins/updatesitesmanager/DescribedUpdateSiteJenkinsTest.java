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

import hudson.model.UpdateSite;
import jakarta.annotation.Nonnull;
import org.htmlunit.FailingHttpStatusCodeException;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.jvnet.hudson.test.junit.jupiter.WithLocalData;
import org.kohsuke.stapler.DataBoundConstructor;

import java.lang.reflect.Method;

/**
 * Tests for DescribedUpdateSite, concerned with Jenkins.
 */
@WithJenkins
class DescribedUpdateSiteJenkinsTest {
    public static class DescribedUpdateSiteForConfigureTest extends DescribedUpdateSite {
        private final String testValue;

        public String getTestValue() {
            return testValue;
        }

        @DataBoundConstructor
        public DescribedUpdateSiteForConfigureTest(String id, String url, String testValue) {
            super(id, url);
            this.testValue = testValue;
        }

        @TestExtension("shouldShowAllDescribedSitesAsManaged")
        public static class DescriptorImpl extends DescribedUpdateSiteDescriptor {
            @Nonnull
            @Override
            public String getDisplayName() {
                return "DescribedUpdateSiteForConfigureTest";
            }
        }
    }

    @Test
    void shouldShowAllDescribedSitesAsManaged(JenkinsRule j) throws Exception {
        String existingId = "test1";
        UpdateSite site1 = new UpdateSite(existingId, "http://example.com/test/update-center.json");
        DescribedUpdateSiteForConfigureTest target = new DescribedUpdateSiteForConfigureTest(
                "test2", "http://example.com/test2/update-center.json", "Some value");

        // Multiple update site.
        j.getInstance().getUpdateCenter().getSites().clear();
        j.getInstance().getUpdateCenter().getSites().add(site1);
        j.getInstance().getUpdateCenter().getSites().add(target);

        UpdateSitesManager manager = new UpdateSitesManager();

        assertThat("should register described UC only", manager.getManagedUpdateSiteList(), hasSize(1));
        j.assertEqualDataBoundBeans(target, manager.getManagedUpdateSiteList().get(0));
    }

    @Test
    @WithLocalData
    void testPrivilege(JenkinsRule j) throws Exception {
        UpdateSite site = new UpdateSite("test1", "http://example.com/test/update-center.json");
        j.getInstance().getUpdateCenter().getSites().add(site);

        HtmlForm form;
        Exception ex;
        try (JenkinsRule.WebClient wcUser = j.createWebClient()) {
            wcUser.getOptions().setPrintContentOnFailingStatusCode(false);
            wcUser.login("user", "user");
            ex = Assertions.assertThrows(FailingHttpStatusCodeException.class, () -> wcUser.goTo(UpdateSitesManager.URL));
        }
        Assertions.assertNotNull(ex);
        assertThat(ex.getMessage(), containsString("403"));

        try (JenkinsRule.WebClient wcAdmin = j.createWebClient()) {
            wcAdmin.getOptions().setPrintContentOnFailingStatusCode(false);
            wcAdmin.login("admin", "admin");
            // configure
            form = wcAdmin.goTo(UpdateSitesManager.URL).getFormByName("sitesForm");
            j.submit(form);
        }
    }

    @Test
    void shouldErrorOnGetReqOfUpdate(JenkinsRule j) throws Exception {
        Exception ex;
        try (JenkinsRule.WebClient wc = j.createWebClient()) {
            wc.getOptions().setPrintContentOnFailingStatusCode(false);
            ex = Assertions.assertThrows(FailingHttpStatusCodeException.class, () -> wc.goTo(UpdateSitesManager.URL + "/update"));
        }
        Assertions.assertNotNull(ex);
        assertThat(ex.getMessage(), containsString("405"));
    }
}
