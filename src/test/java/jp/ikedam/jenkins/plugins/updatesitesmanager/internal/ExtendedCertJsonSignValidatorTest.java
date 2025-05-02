package jp.ikedam.jenkins.plugins.updatesitesmanager.internal;

import static org.junit.Assert.*;

import hudson.util.FormValidation;
import jenkins.util.JSONSignatureValidator;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * @author lanwen (Merkushev Kirill)
 */
@WithJenkins
public class ExtendedCertJsonSignValidatorTest {

    @Test
    void shouldAddCustomCertToTrustAnchors(JenkinsRule j) throws Exception {
        String RESOURCE_BASE = "jp/ikedam/jenkins/plugins/updatesitesmanager/ManagedUpdateSiteJenkinsTest";

        String cert = IOUtils.toString(
                Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(RESOURCE_BASE + "/caCertificate.crt")), StandardCharsets.UTF_8);
        JSONSignatureValidator validator = new ExtendedCertJsonSignValidator("test", cert);
        JSONObject ucToTest = JSONObject.fromObject(IOUtils.toString(
                Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(RESOURCE_BASE + "/update-center.json")),
                StandardCharsets.UTF_8));
        Assertions.assertEquals(FormValidation.Kind.OK, validator.verifySignature(ucToTest).kind);
    }
}
