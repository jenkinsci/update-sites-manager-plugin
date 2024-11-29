package jp.ikedam.jenkins.plugins.updatesitesmanager.testext;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import jp.ikedam.jenkins.plugins.updatesitesmanager.ManagedUpdateSiteJenkinsTest;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.Callback;
import org.jvnet.hudson.test.JenkinsRecipe;
import org.jvnet.hudson.test.JenkinsRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        private static final Map<String, String> servers = new HashMap<String, String>();

        private Server server;

        public void setup(final JenkinsRule jenkinsRule, WebServerRecipe recipe) throws Exception {
            server = new Server();
            ServerConnector connector = new ServerConnector(server);
            server.addConnector(connector);
            server.setHandler(new Handler.Abstract() {
                @Override
                public boolean handle(Request request, Response response, Callback callback) throws Exception {
                    String responseBody = null;
                    boolean handled = false;
                    LOGGER.info("UC gets request to: {}", request.getHttpURI());
                    try {
                        responseBody = FileUtils.readFileToString(
                                getResource(
                                        request.getId(),
                                        jenkinsRule.getTestDescription().getTestClass()),
                                "UTF-8");
                    } catch (URISyntaxException e) {
                        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        handled = true;
                    }
                    if (responseBody != null) {
                        response.getHeaders().add(HttpHeader.CONTENT_TYPE, "text/plain; charset=utf-8");
                        response.setStatus(HttpServletResponse.SC_OK);
                        response.write(true, ByteBuffer.wrap(responseBody.getBytes()), callback);
                        handled = true;
                    }
                    return handled;
                }
            });
            server.start();
            String link = new URL("http", "localhost", connector.getLocalPort(), "").toString();
            LOGGER.info(
                    "Started UC: <{}> for [{}]",
                    link,
                    jenkinsRule.getTestDescription().getMethodName());
            servers.put(jenkinsRule.getTestDescription().getMethodName(), link);
        }

        @Override
        public void tearDown(JenkinsRule jenkinsRule, WebServerRecipe recipe) throws Exception {
            if (server != null) {
                server.stop();
            }
            LOGGER.info(
                    "Shutdown UC for: [{}]", jenkinsRule.getTestDescription().getMethodName());
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
            String filename = "%s/%s".formatted(StringUtils.join(cls.getName().split("\\."), "/"), name);
            URL url = ClassLoader.getSystemResource(filename);
            if (url == null) {
                throw new FileNotFoundException("Not found: %s".formatted(filename));
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
