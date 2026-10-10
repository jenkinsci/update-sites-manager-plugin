package jp.ikedam.jenkins.plugins.updatesitesmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.model.UpdateSite;
import io.jenkins.plugins.casc.ConfigurationAsCode;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.Configurator;
import io.jenkins.plugins.casc.ConfiguratorRegistry;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
public class UpdateSiteConfiguratorTest {

    @TempDir
    Path tempDir;

    @Test
    @SuppressWarnings("unused")
    void shouldOverrideCoreUpdateSiteConfigurator(JenkinsRule j) {
        ConfigurationContext context = new ConfigurationContext(ConfiguratorRegistry.get());

        Configurator<?> configurator = context.lookupOrFail(UpdateSite.class);

        assertInstanceOf(UpdateSiteConfigurator.class, configurator);
    }

    @Test
    void shouldKeepStandardUpdateSiteConfigurationCompatible(JenkinsRule j) throws Exception {
        configure(
                """
                jenkins:
                  updateCenter:
                    sites:
                      - id: "standard"
                        url: "https://example.com/update-center.json"
                """);

        UpdateSite site = getSite(j, "standard");

        assertEquals(UpdateSite.class, site.getClass());
        assertEquals("standard", site.getId());
        assertEquals("https://example.com/update-center.json", site.getUrl());
    }

    @Test
    void shouldConfigureManagedUpdateSiteCustomFields(JenkinsRule j) throws Exception {
        configure(
                """
                jenkins:
                  updateCenter:
                    sites:
                      - id: "managed"
                        url: "https://example.com/managed-update-center.json"
                        useCaCertificate: true
                        caCertificate: "test-certificate"
                        note: "Managed update site"
                        disabled: true
                """);

        UpdateSite site = getSite(j, "managed");
        ManagedUpdateSite managedSite = assertInstanceOf(ManagedUpdateSite.class, site);

        assertEquals("https://example.com/managed-update-center.json", managedSite.getUrl());
        assertEquals("test-certificate", managedSite.getCaCertificate());
        assertTrue(managedSite.isUseCaCertificate());
        assertEquals("Managed update site", managedSite.getNote());
        assertTrue(managedSite.isDisabled());
    }

    @Test
    void shouldConfigureCredentialRequiredUpdateSite(JenkinsRule j) throws Exception {
        configure(
                """
                jenkins:
                  updateCenter:
                    sites:
                      - id: "authenticated"
                        url: "https://example.com/private-update-center.json"
                        credentialsId: "update-site-credentials"
                """);

        UpdateSite site = getSite(j, "authenticated");
        CredentialRequiredUpdateSite credentialSite = assertInstanceOf(CredentialRequiredUpdateSite.class, site);

        assertEquals("authenticated", credentialSite.getId());
        assertEquals("https://example.com/private-update-center.json", credentialSite.getUrl());
        assertEquals("update-site-credentials", credentialSite.getCredentialsId());
    }

    @Test
    void shouldTreatUseCaCertificateAsCustomFieldWhenItIsTheOnlyCustomField(JenkinsRule j) throws Exception {
        configure(
                """
                jenkins:
                  updateCenter:
                    sites:
                      - id: "managed-without-certificate"
                        url: "https://example.com/update-center.json"
                        useCaCertificate: false
                """);

        UpdateSite site = getSite(j, "managed-without-certificate");

        assertInstanceOf(ManagedUpdateSite.class, site);
    }

    @Test
    void shouldExportCustomFields(JenkinsRule j) throws Exception {
        UpdateSite standardSite = new UpdateSite("standard-export", "https://example.com/standard.json");

        ManagedUpdateSite managedSite = new ManagedUpdateSite(
                "managed-export", "https://example.com/managed.json", true, "exported-cert", "exported note", true);

        CredentialRequiredUpdateSite credentialSite = new CredentialRequiredUpdateSite(
                "credential-export", "https://example.com/credential.json", false, null, null, false);
        credentialSite.setCredentialsId("exported-creds");

        j.getInstance().getUpdateCenter().getSites().clear();
        j.getInstance().getUpdateCenter().getSites().add(standardSite);
        j.getInstance().getUpdateCenter().getSites().add(managedSite);
        j.getInstance().getUpdateCenter().getSites().add(credentialSite);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ConfigurationAsCode.get().export(out);
        String exportedYaml = out.toString(StandardCharsets.UTF_8);

        assertTrue(exportedYaml.contains("id: \"standard-export\""));
        assertTrue(exportedYaml.contains("url: \"https://example.com/standard.json\""));
        assertTrue(exportedYaml.contains("id: \"managed-export\""));
        assertTrue(exportedYaml.contains("caCertificate: \"exported-cert\""));
        assertTrue(exportedYaml.contains("useCaCertificate: true"));
        assertTrue(exportedYaml.contains("note: \"exported note\""));
        assertTrue(exportedYaml.contains("disabled: true"));
        assertTrue(exportedYaml.contains("id: \"credential-export\""));
        assertTrue(exportedYaml.contains("credentialsId: \"exported-creds\""));
    }

    @Test
    void shouldRoundTripDefaultManagedSite(JenkinsRule j) throws Exception {
        configure(
                """
            jenkins:
              updateCenter:
                sites:
                  - id: "default-managed"
                    url: "https://example.com/update-center.json"
                    disabled: true
            """);

        UpdateSite site = getSite(j, "default-managed");
        assertInstanceOf(ManagedUpdateSite.class, site);
        assertTrue(((ManagedUpdateSite) site).isDisabled());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ConfigurationAsCode.get().export(out);
        String exportedYaml = out.toString(StandardCharsets.UTF_8);

        assertTrue(exportedYaml.contains("id: \"default-managed\""));
        assertTrue(exportedYaml.contains("disabled: true"));

        org.junit.jupiter.api.Assertions.assertFalse(
                exportedYaml.contains("caCertificate:"), "Exported YAML should not contain null caCertificate");
        org.junit.jupiter.api.Assertions.assertFalse(
                exportedYaml.contains("note:"), "Exported YAML should not contain null note");

        Path exportedFile = tempDir.resolve("exported.yaml");
        Files.writeString(exportedFile, exportedYaml, StandardCharsets.UTF_8);
        ConfigurationAsCode.get().configure(exportedFile.toString());

        UpdateSite roundTrippedSite = getSite(j, "default-managed");
        assertInstanceOf(ManagedUpdateSite.class, roundTrippedSite);
        assertTrue(((ManagedUpdateSite) roundTrippedSite).isDisabled());
    }

    private void configure(String yaml) throws Exception {
        Path configFile = tempDir.resolve("jenkins.yaml");

        Files.writeString(configFile, yaml.replace("\t", "  "), StandardCharsets.UTF_8);

        ConfigurationAsCode.get().configure(configFile.toString());
    }

    private UpdateSite getSite(JenkinsRule j, String id) {
        List<UpdateSite> sites = j.getInstance().getUpdateCenter().getSites();

        UpdateSite site = sites.stream()
                .filter(candidate -> id.equals(candidate.getId()))
                .findFirst()
                .orElse(null);

        assertNotNull(site, "Expected update site with id: " + id);
        return site;
    }
}
