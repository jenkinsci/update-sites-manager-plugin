package jp.ikedam.jenkins.plugins.updatesitesmanager;

import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import hudson.Extension;
import hudson.ProxyConfiguration;
import hudson.model.Item;
import hudson.model.Queue;
import hudson.model.queue.Tasks;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.interceptor.RequirePOST;
import org.springframework.security.core.Authentication;

public class CredentialRequiredUpdateSite extends ManagedUpdateSite {
    private enum AuthenticationType {
        BASIC("Basic"),
        BEARER("Bearer"),
        API_KEY("API Key");

        private final String displayName;
        private static final Map<String, AuthenticationType> VALUES_BY_ID = Arrays.stream(values())
                .collect(Collectors.toUnmodifiableMap(AuthenticationType::name, value -> value));

        AuthenticationType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static AuthenticationType fromId(String id) {
            return VALUES_BY_ID.getOrDefault(id, BASIC);
        }
    }

    private String authenticationType;
    private String apiKeyHeaderName;
    private String credentialsId;

    /**
     * Create a new instance
     *
     * @param id               id for the site
     * @param url              URL for the site
     * @param useCaCertificate whether to use a specified CA certificate
     * @param caCertificate    CA certificate to verify the site
     * @param note             note
     * @param disabled         {@code true} to disable the site
     */
    @DataBoundConstructor
    public CredentialRequiredUpdateSite(
            String id, String url, boolean useCaCertificate, String caCertificate, String note, boolean disabled) {
        super(id, url, useCaCertificate, caCertificate, note, disabled);
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    public String getAuthenticationType() {
        return StringUtils.defaultIfBlank(authenticationType, AuthenticationType.BASIC.name());
    }

    public String getApiKeyHeaderName() {
        return StringUtils.defaultIfBlank(apiKeyHeaderName, "X-API-Key");
    }

    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    @DataBoundSetter
    public void setAuthenticationType(String authenticationType) {
        this.authenticationType = authenticationType;
    }

    @DataBoundSetter
    public void setApiKeyHeaderName(String apiKeyHeaderName) {
        this.apiKeyHeaderName = apiKeyHeaderName;
    }

    @Nullable
    private StandardUsernamePasswordCredentials getCredential() {
        List<StandardUsernamePasswordCredentials> credentials = CredentialsProvider.lookupCredentialsInItem(
                StandardUsernamePasswordCredentials.class, null, ACL.SYSTEM2, null);
        return CredentialsMatchers.firstOrNull(credentials, CredentialsMatchers.withId(credentialsId));
    }

    @Override
    public @NonNull FormValidation updateDirectlyNow(boolean signatureCheck) throws IOException {
        URL url = new URL(getUrl());

        if (credentialsId == null) {
            return FormValidation.error(Messages.CredentialRequiredUpdateSite_credentialsNotFound());
        }

        StandardUsernamePasswordCredentials credential = getCredential();
        if (credential != null) {
            String authorizationValue = getAuthorizationValue(credential);

            HttpClient httpClient = ProxyConfiguration.newHttpClient();
            HttpRequest httpRequest;
            try {
                httpRequest = ProxyConfiguration.newHttpRequestBuilder(url.toURI())
                        .header("Accept", "application/json")
                        .header(getAuthenticationHeaderName(), authorizationValue)
                        .GET()
                        .build();
            } catch (IllegalArgumentException | URISyntaxException e) {
                return FormValidation.error(e.getMessage());
            }
            try {
                HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                String jsonp = response.body();
                String json = "";
                int start = jsonp.indexOf('{');
                int end = jsonp.lastIndexOf('}');
                if (start >= 0 && end > start) {
                    json = jsonp.substring(start, end + 1);
                } else {
                    throw new IOException("Could not find JSON in " + url);
                }
                return updateData(json, signatureCheck);
            } catch (IOException | InterruptedException e) {
                return FormValidation.error(e.getMessage());
            }
        }
        return FormValidation.error(Messages.CredentialRequiredUpdateSite_invalidCredentials(credentialsId));
    }

    @Override
    public URLConnection connect(URL src) throws IOException {
        URLConnection connection = super.connect(src);
        StandardUsernamePasswordCredentials credential = getCredential();
        if (credential != null) {
            connection.setRequestProperty(getAuthenticationHeaderName(), getAuthorizationValue(credential));
        } else {
            throw new IOException(Messages.CredentialRequiredUpdateSite_invalidCredentials(credentialsId));
        }
        return connection;
    }

    private String getAuthenticationHeaderName() {
        return switch (AuthenticationType.fromId(getAuthenticationType())) {
            case API_KEY -> getApiKeyHeaderName();
            case BASIC, BEARER -> "Authorization";
        };
    }

    private String getAuthorizationValue(StandardUsernamePasswordCredentials credential) {
        return switch (AuthenticationType.fromId(getAuthenticationType())) {
            case API_KEY -> credential.getPassword().getPlainText();
            case BEARER -> "Bearer " + credential.getPassword().getPlainText();
            case BASIC -> {
                String token = credential.getUsername() + ':' + credential.getPassword().getPlainText();
                yield "Basic " + Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
            }
        };
    }

    @Extension
    public static class DescriptorImpl extends DescribedUpdateSiteDescriptor {
        /**
         * Returns the kind name of this UpdateSite.
         *
         * shown when select UpdateSite to create.
         *
         * @return the kind name of the site
         * @see hudson.model.Descriptor#getDisplayName()
         */
        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.CredentialRequiredUpdateSite_DisplayName();
        }

        @SuppressWarnings("unused") // invoked from stapler view
        @RequirePOST
        public FormValidation doCheckCredentialsId(
                @CheckForNull @AncestorInPath Item item,
                @QueryParameter String credentialsId,
                @QueryParameter String serverUrl) {
            if (item == null) {
                if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                    return FormValidation.ok();
                }
            } else if (!item.hasPermission(Item.EXTENDED_READ) && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
                return FormValidation.ok();
            }
            if (!StringUtils.isBlank(credentialsId)) {
                List<DomainRequirement> domainRequirement =
                        URIRequirementBuilder.fromUri(serverUrl).build();
                if (CredentialsProvider.listCredentialsInItem(
                                StandardUsernameCredentials.class,
                                item,
                                getAuthentication(item),
                                domainRequirement,
                                CredentialsMatchers.withId(credentialsId))
                        .isEmpty()) {
                    return FormValidation.error("invalid credentials");
                }
            }
            return FormValidation.ok();
        }

        @SuppressWarnings("unused") // invoked from stapler view
        @RequirePOST
        public ListBoxModel doFillCredentialsIdItems(
                final @AncestorInPath Item item,
                @QueryParameter String credentialsId,
                final @QueryParameter String url) {
            StandardListBoxModel result = new StandardListBoxModel();

            credentialsId = StringUtils.trimToEmpty(credentialsId);
            if (item == null) {
                if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                    return result.includeCurrentValue(credentialsId);
                }
            } else {
                if (!item.hasPermission(Item.EXTENDED_READ) && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
                    return result.includeCurrentValue(credentialsId);
                }
            }

            Authentication authentication = getAuthentication(item);
            List<DomainRequirement> build = URIRequirementBuilder.fromUri(url).build();
            CredentialsMatcher always = CredentialsMatchers.always();
            Class<StandardUsernameCredentials> type = StandardUsernameCredentials.class;

            result.includeEmptyValue();
            if (item != null) {
                result.includeMatchingAs(authentication, item, type, build, always);
            } else {
                result.includeMatchingAs(authentication, Jenkins.get(), type, build, always);
            }
            return result;
        }

        @SuppressWarnings("unused")
        public ListBoxModel doFillAuthenticationTypeItems() {
            ListBoxModel result = new ListBoxModel();
            for (AuthenticationType value : AuthenticationType.values()) {
                result.add(value.getDisplayName(), value.name());
            }
            return result;
        }

        protected Authentication getAuthentication(Item item) {
            return item instanceof Queue.Task task ? Tasks.getAuthenticationOf2(task) : ACL.SYSTEM2;
        }
    }
}
