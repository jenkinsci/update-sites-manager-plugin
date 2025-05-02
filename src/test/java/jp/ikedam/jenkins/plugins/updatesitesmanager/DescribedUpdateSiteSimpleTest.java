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

import static hudson.util.FormValidation.Kind.ERROR;
import static hudson.util.FormValidation.Kind.OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import hudson.model.UpdateSite;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Tests for DescribedUpdateSite, not concerned with Jenkins.
 */
@WithJenkins
class DescribedUpdateSiteSimpleTest {
    private static class TestDescribedUpdateSite extends DescribedUpdateSite {
        public TestDescribedUpdateSite(String id, String url) {
            super(id, url);
        }

        public static class DescriptorImpl extends DescribedUpdateSiteDescriptor {
            @Override
            public String getDisplayName() {
                return null;
            }
        }
    }

    @Test
    void shouldTrimUrlAndId(JenkinsRule j) {
        String id = "testId";
        String url = "http://localhost/update-center.json";
        UpdateSite site = new TestDescribedUpdateSite("  " + id + "  ", " " + url + "\t");

        assertThat("id is not trimmed", site.getId(), is(id));
        assertThat("url is not trimmed", site.getUrl(), is(url));
    }

    @Test
    void shouldHandleNull(JenkinsRule j) {
        new TestDescribedUpdateSite(null, null);
    }

    private DescribedUpdateSiteDescriptor getDescriptor() {
        return new TestDescribedUpdateSite.DescriptorImpl();
    }

    @Test
    void testDescriptorDoCheckIdOk(JenkinsRule j) {
        assertThat("simple id", getDescriptor().doCheckId("somevalue").kind, is(OK));
    }

    @ParameterizedTest
    @CsvSource(
            value = {"empty,", "blank, ", "null value,null"},
            ignoreLeadingAndTrailingWhitespace = false,
            nullValues = {"null"})
    void shouldReturnValidationErrOnWrongId(String comment, String id, JenkinsRule j) {
        assertThat(comment, getDescriptor().doCheckId(id).kind, is(ERROR));
    }

    @Test
    void testDescriptorDoCheckUrlOk(JenkinsRule j) {
        String url = "http://localhost/update-center.json";
        assertThat("simple url", getDescriptor().doCheckUrl(url).kind, is(OK));
    }

    @ParameterizedTest
    @CsvSource(
            value = {"empty,", "blank, ", "null value,null", "non url,blabla"},
            ignoreLeadingAndTrailingWhitespace = false,
            nullValues = {"null"})
    void shouldReturnValidationErrOnCheckWrongUrl(String comment, String url, JenkinsRule j) {
        assertThat(comment, getDescriptor().doCheckUrl(url).kind, is(ERROR));
    }
}
