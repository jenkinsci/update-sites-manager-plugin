package jp.ikedam.jenkins.plugins.updatesitesmanager.internal;

import net.sf.json.JSONObject;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.util.FormValidation;

import jenkins.util.JSONSignatureValidator;
import static org.junit.Assert.*;

/**
 * @author lanwen (Merkushev Kirill)
 */
public class ExtendedCertJsonSignValidatorTest {

    @ClassRule
    public static JenkinsRule j = new JenkinsRule();

    @Test
    public void shouldAddCustomCertToTrustAnchors() throws Exception {
        String RESOURCE_BASE = "jp/ikedam/jenkins/plugins/updatesitesmanager/ManagedUpdateSiteJenkinsTest";
        
        String cert = IOUtils.toString(getClass().getClassLoader()
                .getResourceAsStream(RESOURCE_BASE + "/caCertificate.crt"), Charsets.UTF_8);
        JSONSignatureValidator validator = new ExtendedCertJsonSignValidator("test", cert);
        //JSONSignatureValidator validator = new JSONSignatureValidator("test");
        JSONObject ucToTest = JSONObject.fromObject(IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream(RESOURCE_BASE + "/update-center.json"),
                Charsets.UTF_8
        ));
        assertEquals(FormValidation.Kind.OK, validator.verifySignature(ucToTest).kind);
    }
}
