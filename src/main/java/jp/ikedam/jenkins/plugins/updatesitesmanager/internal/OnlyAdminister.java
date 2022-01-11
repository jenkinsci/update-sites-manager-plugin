package jp.ikedam.jenkins.plugins.updatesitesmanager.internal;

import jenkins.model.Jenkins;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.Interceptor;
import org.kohsuke.stapler.interceptor.InterceptorAnnotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;

import javax.servlet.ServletException;

/**
 * Returns 403 if not permitted
 *
 * @author lanwen (Merkushev Kirill)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
@InterceptorAnnotation(OnlyAdminister.Processor.class)
public @interface OnlyAdminister {
    class Processor extends Interceptor {
        @Override
        public Object invoke(StaplerRequest request, StaplerResponse response, Object instance, Object[] arguments)
                throws IllegalAccessException, InvocationTargetException, ServletException {

            Jenkins.get().checkPermission(Jenkins.ADMINISTER);
            return target.invoke(request, response, instance, arguments);
        }
    }
}
