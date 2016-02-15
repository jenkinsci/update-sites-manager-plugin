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

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import hudson.model.UpdateSite;
import org.junit.Test;
import org.junit.runner.RunWith;

import static hudson.util.FormValidation.Kind.ERROR;
import static hudson.util.FormValidation.Kind.OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Tests for DescribedUpdateSite, not concerned with Jenkins.
 */
@RunWith(DataProviderRunner.class)
public class DescribedUpdateSiteSimpleTest {
    private static class TestDescribedUpdateSite extends DescribedUpdateSite {
        private static final long serialVersionUID = 1934091438438690698L;

        public TestDescribedUpdateSite(String id, String url) {
            super(id, url);
        }

        public static class DescriptorImpl extends DescribedUpdateSite.Descriptor {
            @Override
            public String getDisplayName() {
                return null;
            }
        }
    }

    @Test
    public void shouldTrimUrlAndId() {
        String id = "testId";
        String url = "http://localhost/update-center.json";
        UpdateSite site = new TestDescribedUpdateSite(
                "  " + id + "  ",
                " " + url + "\t"
        );

        assertThat("id is not trimmed", site.getId(), is(id));
        assertThat("url is not trimmed", site.getUrl(), is(url));
    }

    @Test
    public void shoulHandleNull() {
        new TestDescribedUpdateSite(null, null);
    }

    private DescribedUpdateSite.Descriptor getDescriptor() {
        return new TestDescribedUpdateSite.DescriptorImpl();
    }

    @Test
    public void testDescriptorDoCheckIdOk() {
        assertThat("simple id", getDescriptor().doCheckId("somevalue").kind, is(OK));
    }

    @Test
    @DataProvider(value = {
            "empty,",
            "blank, ",
            "null value,null"
    }, trimValues = false)
    public void shouldReturnValidationErrOnWrongId(String comment, String id) {
        assertThat(comment, getDescriptor().doCheckId(id).kind, is(ERROR));
    }

    @Test
    public void testDescriptorDoCheckUrlOk() {
        String url = "http://localhost/update-center.json";
        assertThat("simple url", getDescriptor().doCheckUrl(url).kind, is(OK));
    }

    @Test
    @DataProvider(value = {
            "empty,",
            "blank, ",
            "null value,null",
            "non url,blabla"
    }, trimValues = false)
    public void shouldReturnValidationErrOnCheckWrongUrl(String comment, String url) {
        assertThat(comment, getDescriptor().doCheckUrl(url).kind, is(ERROR));
    }
}
