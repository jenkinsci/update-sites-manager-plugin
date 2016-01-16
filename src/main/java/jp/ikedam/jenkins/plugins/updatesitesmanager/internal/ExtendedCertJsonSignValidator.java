package jp.ikedam.jenkins.plugins.updatesitesmanager.internal;

import jenkins.util.JSONSignatureValidator;
import org.apache.tools.ant.filters.StringInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.String.format;

/**
 * Adds provided cert to trust anchors when validating update center json
 *
 * @author lanwen (Merkushev Kirill)
 */
public class ExtendedCertJsonSignValidator extends JSONSignatureValidator {
    private static final Logger LOGGER = Logger.getLogger(ExtendedCertJsonSignValidator.class.getName());

    private String cert;

    public ExtendedCertJsonSignValidator(String id, String cert) {
        super(format("Update site with own cert for %s", id));
        this.cert = cert;
    }

    @Override
    protected Set<TrustAnchor> loadTrustAnchors(CertificateFactory cf) throws IOException {
        Set<TrustAnchor> trustAnchors = super.loadTrustAnchors(cf);
        InputStream stream = null;
        try {
            stream = new StringInputStream(cert);
            Certificate certificate = cf.generateCertificate(stream);
            trustAnchors.add(new TrustAnchor((X509Certificate) certificate, null));
        } catch (CertificateException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        } finally {
            if (stream != null) {
                stream.close();
                stream = null;
            }
        }
        return trustAnchors;
    }
}
