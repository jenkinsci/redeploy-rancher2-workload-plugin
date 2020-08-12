package io.jenkins.plugins.rancher2;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import hudson.*;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.*;

import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;

public class Rancher2RedeployBuilder extends Builder implements SimpleBuildStep {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Nonnull
    private String credential;
    @Nonnull
    private final String workload;
    private final String images;
    private final boolean alwaysPull;

    @DataBoundConstructor
    public Rancher2RedeployBuilder(
            @Nonnull String credential,
            @Nonnull String workload,
            @Nullable String images,
            boolean alwaysPull
    ) {
        this.credential = credential;
        this.workload = workload;
        this.images = images;
        this.alwaysPull = alwaysPull;
    }

    @Nonnull
    public String getCredential() {
        return credential;
    }

    @Nonnull
    public String getWorkload() {
        return workload;
    }

    public String getImages() {
        return images;
    }

    public boolean isAlwaysPull() {
        return alwaysPull;
    }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
        PrintStream logger = listener.getLogger();
        EnvVars envVars = run.getEnvironment(listener);
        String credentialId = envVars.expand(this.credential);

        Rancher2Credentials credential = CredentialsProvider.findCredentialById(
                credentialId,
                Rancher2Credentials.class,
                run,
                (DomainRequirement) null);
        if (credential == null) {
            throw new AbortException(Messages.Rancher2RedeployBuilder_missCredential(credentialId));
        }

        Map<String, String> imageTags = new HashMap<>();
        if (StringUtils.isNotBlank(images)) {
            String expandImages = envVars.expand(images);
            String[] imageArray = expandImages.split(";");
            for (String imageTag : imageArray) {
                String name = parseImageName(imageTag);
                imageTags.put(name, imageTag);
            }
        }
        Set<String> workloadImages = new HashSet<>();
        Set<String> updatedImages = new HashSet<>();

        String endpoint = credential.getEndpoint();
        if (endpoint.endsWith("/")) {
            endpoint = endpoint.substring(0, endpoint.length() - 1);
        }
        try (CloseableHttpClient client = ClientBuilder.create(endpoint, credential.isTrustCert())) {
            String url = endpoint + envVars.expand(workload);
            if (url.startsWith("/p/")) {
                url = url.replaceFirst("/p/", "/project/").replaceFirst("/workload/", "/workloads/");
            }
            HttpUriRequest request = RequestBuilder.get(url)
                    .addHeader("Authorization", "Bearer " + credential.getBearerToken())
                    .addHeader("Accept", "application/json")
                    .build();

            CloseableHttpResponse response = client.execute(request);
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new AbortException(
                        Messages.Rancher2RedeployBuilder_badResponse(
                                response.getStatusLine().getStatusCode(),
                                EntityUtils.toString(response.getEntity())
                        )
                );
            }

            // modify json body for PUT request
            JsonNode root = MAPPER.readTree(response.getEntity().getContent());
            ObjectNode objectNode = (ObjectNode) root;
            objectNode.remove("actions");
            objectNode.remove("links");
            //annotations
            ObjectNode annotations = (ObjectNode) root.get("annotations");
            String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(new Date());
            annotations.put("cattle.io/timestamp", timestamp);
            JsonNode containers = root.get("containers");
            for (int i = 0; i < containers.size(); i++) {
                ObjectNode container = (ObjectNode) containers.get(i);
                String oldTag = container.get("image").asText();
                if (oldTag != null) {
                    String name = parseImageName(oldTag);
                    workloadImages.add(name);
                    if (imageTags.containsKey(name)) {
                        String newTag = imageTags.get(name);
                        container.put("image", newTag);
                        if (alwaysPull) {
                            container.put("imagePullPolicy", "Always");
                        }
                        logger.println(Messages.Rancher2RedeployBuilder_setImageTag(oldTag, newTag));
                        updatedImages.add(name);
                    }
                }
            }

            if (updatedImages.size() != imageTags.size()) {
                throw new AbortException(Messages.Rancher2RedeployBuilder_notMatch(workloadImages, imageTags.keySet()));
            }

            HttpUriRequest putRequest = RequestBuilder.put(url)
                    .addHeader("Authorization", "Bearer " + credential.getBearerToken())
                    .addHeader("Accept", "application/json")
                    .addHeader("Content-Type", "application/json; charset=utf-8")
                    .setEntity(new StringEntity(MAPPER.writeValueAsString(root), "utf-8"))
                    .build();

            CloseableHttpResponse putResponse = client.execute(putRequest);
            if (putResponse.getStatusLine().getStatusCode() != 200) {
                throw new AbortException(Messages.Rancher2RedeployBuilder_badResponse(
                        response.getStatusLine().getStatusCode(),EntityUtils.toString(response.getEntity())
                ));
            }

            logger.println(Messages._Rancher2RedeployBuilder_success());
        }
    }

    /**
     * @param imageTag
     * @return image name without version
     */
    private static String parseImageName(String imageTag) {
        int index = imageTag.lastIndexOf(":");
        if (index < 0) {
            return imageTag;
        }
        return imageTag.substring(0, index);
    }

    @Symbol("rancherRedeploy")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public FormValidation doCheckWorkload(
                @QueryParameter String value) {
            if (StringUtils.isBlank(value)) {
                return FormValidation.error(Messages.Rancher2RedeployBuilder_DescriptorImpl_requireWorkloadPath());
            }
            if (!value.startsWith("/project") && !value.startsWith("/p/")) {
                return FormValidation.error(Messages.Rancher2RedeployBuilder_DescriptorImpl_startWithProject());
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckImages(
                @QueryParameter String value) {
            if (StringUtils.isBlank(value)) {
                return FormValidation.ok();
            }
            String[] images = value.split(";");
            for (String image : images) {
                if (image.isEmpty()) {
                    return FormValidation.error(Messages.Rancher2RedeployBuilder_DescriptorImpl_redundantSemicolon());
                }
            }
            return FormValidation.ok();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.Rancher2RedeployBuilder_DescriptorImpl_displayName();
        }

        public ListBoxModel doFillCredentialItems(
                @AncestorInPath Item item,
                @QueryParameter String credential
        ) {
            StandardListBoxModel result = new StandardListBoxModel();
            if (item == null) {
                if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                    return result.includeCurrentValue(credential);
                }
            } else {
                if (!item.hasPermission(Item.EXTENDED_READ)
                        && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
                    return result.includeCurrentValue(credential);
                }
            }

            return CredentialsProvider.listCredentials(Rancher2Credentials.class,
                    Jenkins.get(), ACL.SYSTEM, Collections.<DomainRequirement>emptyList(), null);
        }

        public FormValidation doCheckCredential(
                @AncestorInPath Item item, // (2)
                @QueryParameter String value
        ) {
            if (item == null) {
                if (!Jenkins.getInstance().hasPermission(Jenkins.ADMINISTER)) {
                    return FormValidation.ok(); // (3)
                }
            } else {
                if (!item.hasPermission(Item.EXTENDED_READ)
                        && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
                    return FormValidation.ok(); // (3)
                }
            }
            if (StringUtils.isBlank(value)) { // (4)
                return FormValidation.ok(); // (4)
            }
            if (value.startsWith("${") && value.endsWith("}")) { // (5)
                return FormValidation.warning(Messages.Rancher2RedeployBuilder_DescriptorImpl_credentialsCannotValidate());
            }
            if (CredentialsProvider.listCredentials(
                    Rancher2Credentials.class,
                    item, null, null,
                    CredentialsMatchers.withId(value)).isEmpty()) {
                return FormValidation.error(Messages.Rancher2RedeployBuilder_DescriptorImpl_credentialsCannotFind());
            }
            return FormValidation.ok();
        }
    }

}
