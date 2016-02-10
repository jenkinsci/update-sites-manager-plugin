package jp.ikedam.jenkins.plugins.updatesitesmanager.testext;

import jp.ikedam.jenkins.plugins.updatesitesmanager.ManagedUpdateSiteJenkinsTest;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.jvnet.hudson.test.JenkinsRecipe;
import org.jvnet.hudson.test.JenkinsRule;
import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Resource-based dummy web-server on random port
 * To get example, see {@link ManagedUpdateSiteJenkinsTest#shouldSuccessfullyUpdateWithWorkingUC()}
 *
 * @author lanwen (Merkushev Kirill)
 */
@Documented
@JenkinsRecipe(WebServerRecipe.RuleRunnerImpl.class)
@Target(METHOD)
@Retention(RUNTIME)
public @interface WebServerRecipe {

    class RuleRunnerImpl extends JenkinsRecipe.Runner<WebServerRecipe> {
        private static final Logger LOGGER = LoggerFactory.getLogger(WebServerRecipe.class);

        /**
         * In case of parallel execution of multiply test-methods we can handle server for each method
         */
        private static Map<String, String> servers = new HashMap<String, String>();
        private Server server;

        public void setup(final JenkinsRule jenkinsRule, WebServerRecipe recipe) throws Exception {
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
                    LOGGER.info("UC gets request to: {}", request.getRequestURI());
                    try {
                        responseBody = FileUtils.readFileToString(
                                getResource(target, jenkinsRule.getTestDescription().getTestClass()),
                                "UTF-8");
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
            String link = new URL("http", "localhost", connector.getLocalPort(), "").toString();
            LOGGER.info("Started UC: <{}> for [{}]", link, jenkinsRule.getTestDescription().getMethodName());
            servers.put(
                    jenkinsRule.getTestDescription().getMethodName(),
                    link
            );
        }

        @Override
        public void tearDown(JenkinsRule jenkinsRule, WebServerRecipe recipe) throws Exception {
            if (server != null) {
                server.stop();
            }
            LOGGER.info("Shutdown UC for: [{}]", jenkinsRule.getTestDescription().getMethodName());
            servers.remove(jenkinsRule.getTestDescription().getMethodName());
        }

        /**
         * To search resource
         *
         * @param name resource file name
         * @param cls  context
         *
         * @return resource file ready to read
         */
        public static File getResource(String name, Class<?> cls) throws URISyntaxException, FileNotFoundException {
            String filename = String.format("%s/%s", StringUtils.join(cls.getName().split("\\."), "/"), name);
            URL url = ClassLoader.getSystemResource(filename);
            if (url == null) {
                throw new FileNotFoundException(String.format("Not found: %s", filename));
            }
            return new File(url.toURI());
        }

        /**
         * To get base url of dummy-server
         *
         * @param method test method name
         *
         * @return base url with defined port
         */
        public static String urlFor(String method) {
            return servers.get(method);
        }
    }
}
