package jp.ikedam.jenkins.plugins.updatesitesmanager.internal;

import hudson.util.HttpResponses;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import javax.servlet.ServletException;
import jp.ikedam.jenkins.plugins.updatesitesmanager.UpdateSitesManager;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.Interceptor;
import org.kohsuke.stapler.interceptor.InterceptorAnnotation;

/**
 * Just redirects to url of sites manager if not POST
 *
 * @author lanwen (Merkushev Kirill)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
@InterceptorAnnotation(IgnoreNotPOST.Processor.class)
public @interface IgnoreNotPOST {
    class Processor extends Interceptor {
        @Deprecated
        @Override
        public Object invoke(StaplerRequest request, StaplerResponse response, Object instance, Object[] arguments)
                throws IllegalAccessException, InvocationTargetException, ServletException {

            if (!request.getMethod().equals("POST")) {
                throw new InvocationTargetException(HttpResponses.redirectViaContextPath(UpdateSitesManager.URL));
            }

            return target.invoke(request, response, instance, arguments);
        }
    }
}
