package com.finbourne.TO_BE_REPLACED.integration.extensions;

import com.finbourne.TO_BE_REPLACED.ApiClient;
import com.finbourne.TO_BE_REPLACED.auth.Authentication;
import com.finbourne.TO_BE_REPLACED.auth.OAuth;
import com.finbourne.TO_BE_REPLACED.extensions.*;
import com.finbourne.TO_BE_REPLACED.extensions.auth.FinbourneTokenException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.time.Duration;
import java.time.Instant;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertEquals;

public class ApiClientBuilderIT {

    private ApiClientBuilder apiClientBuilder;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        apiClientBuilder = new ApiClientBuilder();
    }

    @Test
    public void build_OnValidConfigurationFile_ShouldBuildKeepLiveApiClient()
            throws ApiConfigurationException, FinbourneTokenException {
        // This test assumes default secrets file is valid without a PAT. Same assertion
        // as all other integration tests.
        ApiConfiguration apiConfiguration = new ApiConfigurationBuilder().build(CredentialsSource.credentialsFile);
        ApiClient apiClient = new ApiClientBuilder().build(apiConfiguration);
        // running with no exceptions ensures client built correctly with no
        // configuration or token creation exceptions
        assertThat("Unexpected extended implementation of ApiClient for default build.",
                apiClient, instanceOf(ApiClient.class));
    }

    @Test
    public void build_WithValidPAT_ShouldBuildKeepLiveApiClient()
            throws ApiConfigurationException, FinbourneTokenException {

        ApiConfiguration apiConfiguration = new ApiConfigurationBuilder().build("secrets-pat.json");
        ApiClient apiClient = new ApiClientBuilder().build(apiConfiguration);

        OAuth auth = (OAuth) apiClient.getAuthentication("oauth2");

        assertEquals(auth.getAccessToken(), apiConfiguration.getPersonalAccessToken());
    }

    @Test
    public void build_BadTokenConfigurationFile_ShouldThrowException() throws FinbourneTokenException {
        thrown.expect(FinbourneTokenException.class);
        ApiConfiguration apiConfiguration = getBadTokenConfiguration();
        ApiClient apiClient = new ApiClientBuilder().build(apiConfiguration);
    }

    private ApiConfiguration getBadTokenConfiguration() {
        return new ApiConfiguration(
                "https://some-non-existing-test-instance.doesnotexist.com/oauth2/doesnotexist/v1/token",
                "test.testing@finbourne.com",
                "note",
                "invalid-client-id",
                "none",
                "https://some-non-existing-test-instance.lusid.com/api",
                "non-existent",
                null,
                // proxy strs
                "", 8888, "", "",
                0);
    }
}
