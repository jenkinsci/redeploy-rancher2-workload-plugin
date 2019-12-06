package io.jenkins.plugins.rancher2;

import com.cloudbees.plugins.credentials.common.StandardCredentials;

import java.io.IOException;

public interface Rancher2Credentials extends StandardCredentials {
    String getEndpoint() throws IOException, InterruptedException;
    boolean isTrustCert() throws IOException, InterruptedException;
    String getBearerToken() throws IOException, InterruptedException;
}
