package jp.ikedam.jenkins.plugins.updatesitesmanager.internal;

import hudson.model.UpdateSite;
import jakarta.servlet.ServletException;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.AnnotationHandler;
import org.kohsuke.stapler.InjectedParameter;
import org.kohsuke.stapler.StaplerRequest2;

/**
 * Extracts sites from form submission, or just returns empty list
 *
 * @author lanwen (Merkushev Kirill)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@Documented
@InjectedParameter(Sites.PayloadHandler.class)
public @interface Sites {
    class PayloadHandler extends AnnotationHandler<Sites> {
        @Override
        public List<UpdateSite> parse(StaplerRequest2 req, Sites a, Class type, String pName) throws ServletException {
            if (!req.getMethod().equals("POST")) {
                return new ArrayList<>();
            }

            JSONObject sites = req.getSubmittedForm();
            return req.bindJSONToList(UpdateSite.class, sites.get("sites"));
        }
    }
}
