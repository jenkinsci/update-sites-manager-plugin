package jp.ikedam.jenkins.plugins.updatesitesmanager.internal;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.security.cert.CertificateFactory;
import java.security.cert.TrustAnchor;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

/**
 * @author lanwen (Merkushev Kirill)
 */
public class ExtendedCertJsonSignValidatorTest {

    @Test
    public void shouldAddCustomCertToTrustAnchors() throws Exception {
        String cert = IOUtils.toString(getClass().getClassLoader()
                .getResourceAsStream("jp/ikedam/jenkins/plugins/updatesitesmanager/" +
                        "ManagedUpdateSiteJenkinsTest/caCertificate.crt"), Charsets.UTF_8);
        CertificateFactory cf = CertificateFactory.getInstance("X509");

        Set<TrustAnchor> test = new ExtendedCertJsonSignValidator("test", cert).loadTrustAnchors(cf);
        assertThat(test, hasSize(1));
    }
}