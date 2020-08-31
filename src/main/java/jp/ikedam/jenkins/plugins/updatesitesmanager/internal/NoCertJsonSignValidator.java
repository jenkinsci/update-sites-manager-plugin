package jp.ikedam.jenkins.plugins.updatesitesmanager.internal;

import static java.lang.String.format;
import hudson.util.FormValidation;

import java.io.IOException;

import jenkins.util.JSONSignatureValidator;
import net.sf.json.JSONObject;

/**
 * Skips signature validation
 */
public class NoCertJsonSignValidator extends JSONSignatureValidator {

    public NoCertJsonSignValidator(String id, String cert) {
        super(format("Update site without cert for %s", id));
    }

    @Override
    public FormValidation verifySignature(JSONObject arg0) throws IOException
    {
      return FormValidation.ok();
    }
    
}
