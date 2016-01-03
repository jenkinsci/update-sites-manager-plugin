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

import hudson.util.FormValidation;
import jenkins.model.DownloadSettings;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.stapler.HttpResponse;
import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import static hudson.util.FormValidation.Kind.ERROR;
import static hudson.util.FormValidation.Kind.OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;

/**
 * Tests for ManagedUpdateSite, concerned with Jenkins.
 */
public class ManagedUpdateSiteJenkinsTest {

    @Rule
    public JenkinsRule jRule = new JenkinsRule();

    private Server server;
    private URL baseUrl;

    @Test
    public void testDescriptorDoCheckCaCertificate() throws IOException, URISyntaxException {
        String caCertificate = FileUtils.readFileToString(getResource("caCertificate.crt"));

        assertThat("Always ok if certificate is disabled",
                getDescriptor().doCheckCaCertificate(false, null).kind, is(OK));

        assertThat("OK for valid certificate", getDescriptor().doCheckCaCertificate(true, caCertificate).kind, is(OK));
    }

    @Test
    public void testDescriptorDoCheckCaCertificateError() {
        assertThat("Null certificate", getDescriptor().doCheckCaCertificate(true, null).kind, is(ERROR));

        assertThat("Empty certificate", getDescriptor().doCheckCaCertificate(true, "").kind, is(ERROR));

        assertThat("Blank certificate", getDescriptor().doCheckCaCertificate(true, "  ").kind, is(ERROR));

        assertThat("Invalid certificate",
                getDescriptor().doCheckCaCertificate(true, "hogehogehogehoge").kind, is(ERROR));
    }

    @Test
    public void testDoCheckUpdateServer() throws Exception {
        setUpWebServer();

        // Ensure to use server-based download.
        boolean isUseBrowser = DownloadSettings.get().isUseBrowser();
        try {
            DownloadSettings.get().setUseBrowser(false);
            String caCertificate = FileUtils.readFileToString(getResource("caCertificate.crt"));

            TestManagedUpdateSite target = new TestManagedUpdateSite(
                    "test",
                    new URL(baseUrl, "update-center.json").toExternalForm(),
                    false,
                    null,
                    "test",
                    false
            );
            jRule.getInstance().getUpdateCenter().getSites().clear();

            {
                target.setCaCertificate(null);
                HttpResponse rsp = jRule.getInstance().getPluginManager().doCheckUpdatesServer();
                if (rsp instanceof FormValidation) {
                    // this fails with Jenkins < 1.600, Jenkins < 1.596.1
                    assertEquals(
                            "Accessing update center without any update sites should succeed",
                            FormValidation.Kind.OK,
                            ((FormValidation) rsp).kind
                    );
                }
            }

            jRule.getInstance().getUpdateCenter().getSites().add(target);

            {
                target.setCaCertificate(null);
                HttpResponse rsp = jRule.getInstance().getPluginManager().doCheckUpdatesServer();
                if (rsp instanceof FormValidation) {
                    assertEquals(
                            "Accessing update center with no certificate must fail",
                            FormValidation.Kind.ERROR,
                            ((FormValidation) rsp).kind
                    );
                }
            }

            {
                target.setCaCertificate(caCertificate);
                HttpResponse rsp = jRule.getInstance().getPluginManager().doCheckUpdatesServer();
                if (rsp instanceof FormValidation) {
                    assertEquals(
                            "Accessing update center with a proper certificate must succeed",
                            FormValidation.Kind.OK,
                            ((FormValidation) rsp).kind
                    );
                }
            }

            {
                target.setCaCertificate(null);
                HttpResponse rsp = jRule.getInstance().getPluginManager().doCheckUpdatesServer();
                if (rsp instanceof FormValidation) {
                    assertEquals(
                            "Accessing update center with no certificate must fail",
                            FormValidation.Kind.ERROR,
                            ((FormValidation) rsp).kind
                    );
                }
            }
        } finally {
            DownloadSettings.get().setUseBrowser(isUseBrowser);
            if (server != null) {
                server.stop();
            }
        }
    }

    public void setUpWebServer() throws Exception {
        server = new Server();
        SocketConnector connector = new SocketConnector();
        server.addConnector(connector);
        server.setHandler(new AbstractHandler() {
            @Override
            public void handle(
                    String target, HttpServletRequest request,
                    HttpServletResponse response, int dispatch
            ) throws IOException, ServletException {
                String responseBody = null;
                try {
                    responseBody = FileUtils.readFileToString(getResource(target), "UTF-8");
                } catch (URISyntaxException e) {
                    HttpConnection.getCurrentConnection().getRequest().setHandled(true);
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                }
                if (responseBody != null) {
                    HttpConnection.getCurrentConnection().getRequest().setHandled(true);
                    response.setContentType("text/plain; charset=utf-8");
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.getOutputStream().write(responseBody.getBytes());
                }
            }
        });
        server.start();
        baseUrl = new URL("http", "localhost", connector.getLocalPort(), "");
    }


    private File getResource(String name) throws URISyntaxException, FileNotFoundException {
        String filename = String.format("%s/%s", StringUtils.join(getClass().getName().split("\\."), "/"), name);
        URL url = ClassLoader.getSystemResource(filename);
        if (url == null) {
            throw new FileNotFoundException(String.format("Not found: %s", filename));
        }
        return new File(url.toURI());
    }

    private ManagedUpdateSite.DescriptorImpl getDescriptor() {
        return (ManagedUpdateSite.DescriptorImpl) new ManagedUpdateSite(null, null, false, null, null, false)
                .getDescriptor();
    }

    static public class TestManagedUpdateSite extends ManagedUpdateSite {
        private static final long serialVersionUID = -6888318503867286760L;

        public TestManagedUpdateSite(
                String id,
                String url,
                boolean useCaCertificate,
                String caCertificate,
                String note,
                boolean disabled
        ) {
            super(id, url, useCaCertificate, caCertificate, note, disabled);
        }
    }
}
