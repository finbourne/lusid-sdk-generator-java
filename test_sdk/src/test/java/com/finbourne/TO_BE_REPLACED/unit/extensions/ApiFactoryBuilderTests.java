package com.finbourne.TO_BE_REPLACED.unit.extensions;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.finbourne.TO_BE_REPLACED.TestConstants;
import com.finbourne.TO_BE_REPLACED.api.TEST_API;
import com.finbourne.TO_BE_REPLACED.extensions.ApiConfigurationBuilder;
import com.finbourne.TO_BE_REPLACED.extensions.ApiConfigurationException;
import com.finbourne.TO_BE_REPLACED.extensions.ApiFactory;
import com.finbourne.TO_BE_REPLACED.extensions.ApiFactoryBuilder;
import com.finbourne.TO_BE_REPLACED.extensions.ConfigurationOptions;
import com.finbourne.TO_BE_REPLACED.extensions.RateLimitRetryInterceptor;
import com.finbourne.TO_BE_REPLACED.extensions.auth.FinbourneTokenException;

import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import okhttp3.OkHttpClient;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

@ExtendWith(SystemStubsExtension.class)
public class ApiFactoryBuilderTests {

    @SystemStub
    private EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @BeforeEach
    public void SetUp() throws Exception {
        environmentVariables.setup();
    }

    @AfterEach
    public void TearDown() throws Exception {
        environmentVariables.teardown();
    }

    @Test
    public void WhenNoConfig_UsesDefaults() throws ApiConfigurationException, FinbourneTokenException {
        // setup minimum required env vars
        environmentVariables
            .set("FBN_ACCESS_TOKEN", "pat-token")
            .set("FBN_TO_BE_REPLACED_UPPER_SNAKECASE_URL", "https://some-non-existing-test-instance.lusid.com/api");

        // build an ApiFactory without specifying any config
        ApiFactory apiFactory = ApiFactoryBuilder.build();

        // get the http client from an api instance generated from the factory
        OkHttpClient httpClient = apiFactory.build(TEST_API.class).getApiClient().getHttpClient();

        // verify default config set
        verifyHttpClientUsingDefaults(httpClient);
    }

    @Test
    public void WhenOverridesSpecified_UsesOverrides() throws ApiConfigurationException, FinbourneTokenException {
        // setup minimum required env vars
        environmentVariables
            .set("FBN_ACCESS_TOKEN", "pat-token")
            .set("FBN_TO_BE_REPLACED_UPPER_SNAKECASE_URL", "https://some-non-existing-test-instance.lusid.com/api");

        // build an ApiFactory with overrides
        ConfigurationOptions opts = new ConfigurationOptions(4000, 1000, 2000, 3000, 5);
        ApiFactory apiFactory = ApiFactoryBuilder.build(opts);

        // get the http client from an api instance generated from the factory
        OkHttpClient httpClient = apiFactory.build(TEST_API.class).getApiClient().getHttpClient();

        // verify overrides used
        verifyHttpClientUsingOverrides(httpClient, opts);
    }

    @Test
    public void WhenNoConfig_AndFileSpecified_UsesDefaults() throws ApiConfigurationException, FinbourneTokenException {
        // build an ApiFactory with minimum required config from file
        ApiFactory apiFactory = ApiFactoryBuilder.build(TestConstants.DUMMY_CREDENTIALS_FILE_LONG_LIVED_AUTH);

        // get the http client from an api instance generated from the factory
        OkHttpClient httpClient = apiFactory.build(TEST_API.class).getApiClient().getHttpClient();

        // verify default config set
        verifyHttpClientUsingDefaults(httpClient);
    }

    @Test
    public void WhenOverridesSpecified_AndFileSpecified_UsesOverrides() throws ApiConfigurationException, FinbourneTokenException {
        // build an ApiFactory with minimum required config from file and overrides
        ConfigurationOptions opts = new ConfigurationOptions(4000, 1000, 2000, 3000, 5);
        ApiFactory apiFactory = ApiFactoryBuilder.build(TestConstants.DUMMY_CREDENTIALS_FILE_LONG_LIVED_AUTH, opts);

        // get the http client from an api instance generated from the factory
        OkHttpClient httpClient = apiFactory.build(TEST_API.class).getApiClient().getHttpClient();

        // verify overrides used
        verifyHttpClientUsingOverrides(httpClient, opts);
    }

    private void verifyHttpClientUsingDefaults(OkHttpClient httpClient) {
        assertThat(httpClient.callTimeoutMillis(), equalTo(ApiConfigurationBuilder.DEFAULT_TOTAL_TIMEOUT_MS));
        assertThat(httpClient.connectTimeoutMillis(), equalTo(ApiConfigurationBuilder.DEFAULT_CONNECT_TIMEOUT_MS));
        assertThat(httpClient.readTimeoutMillis(), equalTo(ApiConfigurationBuilder.DEFAULT_READ_TIMEOUT_MS));
        assertThat(httpClient.writeTimeoutMillis(), equalTo(ApiConfigurationBuilder.DEFAULT_WRITE_TIMEOUT_MS));
        verifyRateLimitRetries(httpClient, ApiConfigurationBuilder.DEFAULT_RATE_LIMIT_RETRIES);
    }

    private void verifyHttpClientUsingOverrides(OkHttpClient httpClient, ConfigurationOptions opts) {
        assertThat(httpClient.callTimeoutMillis(), equalTo(opts.getTotalTimeoutMs()));
        assertThat(httpClient.connectTimeoutMillis(), equalTo(opts.getConnectTimeoutMs()));
        assertThat(httpClient.readTimeoutMillis(), equalTo(opts.getReadTimeoutMs()));
        assertThat(httpClient.writeTimeoutMillis(), equalTo(opts.getWriteTimeoutMs()));
        verifyRateLimitRetries(httpClient, opts.getRateLimitRetries());
    }

    private void verifyRateLimitRetries(OkHttpClient httpClient, int expectedMaxRetries) {
        int rateLimitInterceptors = 0;
        for (okhttp3.Interceptor interceptor : httpClient.interceptors()) {
            rateLimitInterceptors += 1;
            if (interceptor instanceof RateLimitRetryInterceptor) {
                RateLimitRetryInterceptor rateLimitRetryInterceptor = (RateLimitRetryInterceptor)interceptor;
                assertThat(rateLimitRetryInterceptor.getMaxRetries(), equalTo(expectedMaxRetries));
            }
        }
        if (rateLimitInterceptors != 1) {
            Assertions.fail("expected there to be exactly one RateLimitRetryInterceptor but there were: " + rateLimitInterceptors);
        }
    }
}
