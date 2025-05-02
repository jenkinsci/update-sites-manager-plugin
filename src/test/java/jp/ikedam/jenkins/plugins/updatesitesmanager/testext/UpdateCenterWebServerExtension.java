package jp.ikedam.jenkins.plugins.updatesitesmanager.testext;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.Callback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.runner.Description;
import org.jvnet.hudson.test.JenkinsRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class UpdateCenterWebServerExtension implements BeforeEachCallback, AfterEachCallback {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateCenterWebServerExtension.class);

    /**
     * In case of parallel execution of multiply test-methods we can handle server for each method
     */
    private static final Map<String, String> servers = new HashMap<String, String>();

    private Server server;

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        server = new Server();
        ServerConnector connector = new ServerConnector(server);
        server.addConnector(connector);
        server.setHandler(new Handler.Abstract() {
            @Override
            public boolean handle(Request request, Response response, Callback callback) throws Exception {
                String responseBody = null;
                boolean handled = false;
                LOGGER.info("UC gets request to: {}", request.getHttpURI());

                Optional<Class<?>> testClass = context.getTestClass();
                if(testClass.isEmpty()) {
                    LOGGER.info("Test class is not available");
                    return false;
                }

                try {
                    responseBody = FileUtils.readFileToString(
                            getResource(
                                    request.getId(),
                                    testClass.get()),
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
        String methodName = methodFor(context);
        LOGGER.info(
                "Started UC: <{}> for [{}]",
                link,
                methodName);
        servers.put(methodName, link);
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        if (server != null) {
            server.stop();
        }
        String methodName = methodFor(context);
        LOGGER.info(
                "Shutdown UC for: [{}]", methodName);
        servers.remove(methodName);
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

    private String methodFor(ExtensionContext context) {
        if(context.getTestMethod().isPresent()) {
            Method method = context.getTestMethod().get();
            Description description = Description.createTestDescription(method.getDeclaringClass(), method.getName(), method.getAnnotations());
            return description.getMethodName();
        }
        return "";
    }
}
