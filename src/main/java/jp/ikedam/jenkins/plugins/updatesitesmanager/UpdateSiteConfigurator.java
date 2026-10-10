package jp.ikedam.jenkins.plugins.updatesitesmanager;

import static io.jenkins.plugins.casc.Attribute.noop;
import static java.lang.Boolean.parseBoolean;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.UpdateSite;
import io.jenkins.plugins.casc.Attribute;
import io.jenkins.plugins.casc.BaseConfigurator;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.model.Mapping;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Extension(optional = true, ordinal = 100)
public class UpdateSiteConfigurator extends BaseConfigurator<UpdateSite> {

    @Override
    public Class<UpdateSite> getTarget() {
        return UpdateSite.class;
    }

    @Override
    protected UpdateSite instance(Mapping mapping, ConfigurationContext context) throws ConfiguratorException {
        String id = mapping.containsKey("id") ? mapping.getScalarValue("id") : null;
        String url = mapping.containsKey("url") ? mapping.getScalarValue("url") : null;

        String credentialsId = mapping.containsKey("credentialsId") ? mapping.getScalarValue("credentialsId") : null;
        String caCertificate = mapping.containsKey("caCertificate") ? mapping.getScalarValue("caCertificate") : null;
        String useCaCertificateStr =
                mapping.containsKey("useCaCertificate") ? mapping.getScalarValue("useCaCertificate") : null;
        String note = mapping.containsKey("note") ? mapping.getScalarValue("note") : null;
        String disabledStr = mapping.containsKey("disabled") ? mapping.getScalarValue("disabled") : null;

        boolean useCaCertificate =
                useCaCertificateStr != null ? parseBoolean(useCaCertificateStr) : (caCertificate != null);
        boolean disabled = parseBoolean(disabledStr);

        if (credentialsId != null) {
            CredentialRequiredUpdateSite site =
                    new CredentialRequiredUpdateSite(id, url, useCaCertificate, caCertificate, note, disabled);
            site.setCredentialsId(credentialsId);
            return site;
        } else if (caCertificate != null || note != null || disabledStr != null || useCaCertificateStr != null) {
            return new ManagedUpdateSite(id, url, useCaCertificate, caCertificate, note, disabled);
        } else {
            return new UpdateSite(id, url);
        }
    }

    @NonNull
    @Override
    public Set<Attribute<UpdateSite, ?>> describe() {
        return new HashSet<>(Arrays.asList(
                new Attribute<UpdateSite, String>("id", String.class)
                        .getter(UpdateSite::getId)
                        .setter(noop()),
                new Attribute<UpdateSite, String>("url", String.class)
                        .getter(UpdateSite::getUrl)
                        .setter(noop()),
                new Attribute<UpdateSite, String>("caCertificate", String.class)
                        .getter(site -> site instanceof ManagedUpdateSite
                                ? ((ManagedUpdateSite) site).getCaCertificate()
                                : null)
                        .setter(noop()),
                new Attribute<UpdateSite, Boolean>("useCaCertificate", Boolean.class)
                        .getter(this::getUseCaCertificate)
                        .setter(noop()),
                new Attribute<UpdateSite, String>("note", String.class)
                        .getter(site -> site instanceof ManagedUpdateSite ? ((ManagedUpdateSite) site).getNote() : null)
                        .setter(noop()),
                new Attribute<UpdateSite, Boolean>("disabled", Boolean.class)
                        .getter(this::getDisabled)
                        .setter(noop()),
                new Attribute<UpdateSite, String>("credentialsId", String.class)
                        .getter(site -> site instanceof CredentialRequiredUpdateSite
                                ? ((CredentialRequiredUpdateSite) site).getCredentialsId()
                                : null)
                        .setter(noop())));
    }

    @CheckForNull
    private Boolean getUseCaCertificate(UpdateSite site) {
        return site instanceof ManagedUpdateSite ? ((ManagedUpdateSite) site).isUseCaCertificate() : null;
    }

    @CheckForNull
    private Boolean getDisabled(UpdateSite site) {
        return site instanceof ManagedUpdateSite ? ((ManagedUpdateSite) site).isDisabled() : null;
    }
}
