package com.finbourne.TO_BE_REPLACED.unit.extensions;

import com.finbourne.TO_BE_REPLACED.extensions.ApiConfiguration;
import com.finbourne.TO_BE_REPLACED.extensions.ApiConfigurationBuilder;
import com.finbourne.TO_BE_REPLACED.extensions.ApiConfigurationBuilder.ConfigurationWithErrors;
import com.finbourne.TO_BE_REPLACED.extensions.ApiConfigurationException;
import com.finbourne.TO_BE_REPLACED.extensions.ConfigurationOptions;

import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import static com.finbourne.TO_BE_REPLACED.TestConstants.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collection;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(SystemStubsExtension.class)
public class ApiConfigurationBuilderTests {

    final static String TOKEN_URL = "https://some-non-existing-test-instance.lusid.com/oauth2/doesnotexist/v1/token";
    final static String USERNAME = "user";
    final static String PASSWORD = "pass";
    final static String CLIENT_ID = "client-id";
    final static String CLIENT_SECRET = "secret";
    final static String ACCESS_TOKEN = "pat-token";
    final static String API_URL = "https://some-non-existing-test-instance.lusid.com/api";
    final static String APP_NAME = "non-existent";
    final static int TOTAL_TIMEOUT_MS = 40000;
    final static int CONNECT_TIMEOUT_MS = 10000;
    final static int READ_TIMEOUT_MS = 20000;
    final static int WRITE_TIMEOUT_MS = 30000;
    final static int RATE_LIMIT_RETRIES = 2;
    final static String PROXY_ADDRESS = "https://proxy";
    final static int PROXY_PORT = 1234;
    final static String PROXY_USERNAME = "proxy-username";
    final static String PROXY_PASSWORD = "proxy-password";


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
    public void correctApiConfigurationBuiltFromEnvVars() {
        // arrange
        environmentVariables
            .set("FBN_TOKEN_URL", TOKEN_URL)
            .set("FBN_USERNAME", USERNAME)
            .set("FBN_PASSWORD", PASSWORD)
            .set("FBN_CLIENT_ID", CLIENT_ID)
            .set("FBN_CLIENT_SECRET", CLIENT_SECRET)
            .set("FBN_ACCESS_TOKEN", ACCESS_TOKEN)
            .set("FBN_TO_BE_REPLACED_UPPER_SNAKECASE_URL", API_URL)
            .set("FBN_APP_NAME", APP_NAME)
            .set("FBN_TOTAL_TIMEOUT_MS", String.valueOf(TOTAL_TIMEOUT_MS))
            .set("FBN_CONNECT_TIMEOUT_MS", String.valueOf(CONNECT_TIMEOUT_MS))
            .set("FBN_READ_TIMEOUT_MS", String.valueOf(READ_TIMEOUT_MS))
            .set("FBN_WRITE_TIMEOUT_MS", String.valueOf(WRITE_TIMEOUT_MS))
            .set("FBN_RATE_LIMIT_RETRIES", String.valueOf(RATE_LIMIT_RETRIES))
            .set("FBN_PROXY_ADDRESS", PROXY_ADDRESS)
            .set("FBN_PROXY_PORT", String.valueOf(PROXY_PORT))
            .set("FBN_PROXY_USERNAME", PROXY_USERNAME)
            .set("FBN_PROXY_PASSWORD", PROXY_PASSWORD);

        ApiConfigurationBuilder apiConfigurationBuilder = new ApiConfigurationBuilder();

        // act
        ConfigurationWithErrors configurationWithErrors = apiConfigurationBuilder.getApiConfigurationFromEnvironmentVariables(new ConfigurationOptions());

        // assert
        verify_expectedApiConfigurationNoErrors(configurationWithErrors);
    }

    @Test
    public void envVarsOverriddenByOpts() {
        // arrange
        environmentVariables
            .set("FBN_TOKEN_URL", TOKEN_URL)
            .set("FBN_USERNAME", USERNAME)
            .set("FBN_PASSWORD", PASSWORD)
            .set("FBN_CLIENT_ID", CLIENT_ID)
            .set("FBN_CLIENT_SECRET", CLIENT_SECRET)
            .set("FBN_ACCESS_TOKEN", ACCESS_TOKEN)
            .set("FBN_TO_BE_REPLACED_UPPER_SNAKECASE_URL", API_URL)
            .set("FBN_APP_NAME", APP_NAME)
            .set("FBN_TOTAL_TIMEOUT_MS", String.valueOf(TOTAL_TIMEOUT_MS))
            .set("FBN_CONNECT_TIMEOUT_MS", String.valueOf(CONNECT_TIMEOUT_MS))
            .set("FBN_READ_TIMEOUT_MS", String.valueOf(READ_TIMEOUT_MS))
            .set("FBN_WRITE_TIMEOUT_MS", String.valueOf(WRITE_TIMEOUT_MS))
            .set("FBN_RATE_LIMIT_RETRIES", String.valueOf(RATE_LIMIT_RETRIES))
            .set("FBN_PROXY_ADDRESS", PROXY_ADDRESS)
            .set("FBN_PROXY_PORT", String.valueOf(PROXY_PORT))
            .set("FBN_PROXY_USERNAME", PROXY_USERNAME)
            .set("FBN_PROXY_PASSWORD", PROXY_PASSWORD);

        ApiConfigurationBuilder apiConfigurationBuilder = new ApiConfigurationBuilder();
        int totalTimeoutMs = 400;
        int connectTimeoutMs = 100;
        int readTimeoutMs = 200;
        int writeTimeoutMs = 300;
        int maxRetryAttempts = 5;

        ConfigurationOptions opts = new ConfigurationOptions(
            totalTimeoutMs, connectTimeoutMs, readTimeoutMs, writeTimeoutMs, maxRetryAttempts);

        // act
        ConfigurationWithErrors configurationWithErrors = apiConfigurationBuilder.getApiConfigurationFromEnvironmentVariables(opts);

        // assert
        assertThat((Collection<String>)configurationWithErrors.errors, is(empty()));
        ApiConfiguration apiConfiguration = configurationWithErrors.configuration;
        assertThat(apiConfiguration.getTokenUrl(), equalTo(TOKEN_URL));
        assertThat(apiConfiguration.getUsername(), equalTo(USERNAME));
        assertThat(apiConfiguration.getPassword(), equalTo(PASSWORD));
        assertThat(apiConfiguration.getClientId(), equalTo(CLIENT_ID));
        assertThat(apiConfiguration.getClientSecret(), equalTo(CLIENT_SECRET));
        assertThat(apiConfiguration.getPersonalAccessToken(), equalTo(ACCESS_TOKEN));
        assertThat(apiConfiguration.getApiUrl(), equalTo(API_URL));
        assertThat(apiConfiguration.getApplicationName(), equalTo(APP_NAME));
        assertThat(apiConfiguration.getProxyAddress(), equalTo(PROXY_ADDRESS));
        assertThat(apiConfiguration.getProxyPort(), equalTo(PROXY_PORT));
        assertThat(apiConfiguration.getProxyUsername(), equalTo(PROXY_USERNAME));
        assertThat(apiConfiguration.getProxyPassword(), equalTo(PROXY_PASSWORD));

        // config overridden by the opts
        assertThat(apiConfiguration.getTotalTimeoutMs(), equalTo(totalTimeoutMs));
        assertThat(apiConfiguration.getConnectTimeoutMs(), equalTo(connectTimeoutMs));
        assertThat(apiConfiguration.getReadTimeoutMs(), equalTo(readTimeoutMs));
        assertThat(apiConfiguration.getWriteTimeoutMs(), equalTo(writeTimeoutMs));
        assertThat(apiConfiguration.getRateLimitRetries(), equalTo(maxRetryAttempts));
    }

    @Test
    public void correctApiConfigurationBuiltFromFile() throws ApiConfigurationException {
        // arrange
        ApiConfigurationBuilder apiConfigurationBuilder = new ApiConfigurationBuilder();

        // act
        ConfigurationWithErrors configurationWithErrors = apiConfigurationBuilder.getApiConfigurationFromFile(DUMMY_CREDENTIALS_FILE, new ConfigurationOptions());

        // assert
        verify_expectedApiConfigurationNoErrors(configurationWithErrors);
    }

    @Test
    public void fileConfigOverriddenByOpts() throws ApiConfigurationException {
        // arrange
        ApiConfigurationBuilder apiConfigurationBuilder = new ApiConfigurationBuilder();
        int totalTimeoutMs = 400;
        int connectTimeoutMs = 100;
        int readTimeoutMs = 200;
        int writeTimeoutMs = 300;
        int maxRetryAttempts = 5;

        ConfigurationOptions opts = new ConfigurationOptions(
            totalTimeoutMs, connectTimeoutMs, readTimeoutMs, writeTimeoutMs, maxRetryAttempts);

        // act
        ConfigurationWithErrors configurationWithErrors = apiConfigurationBuilder.getApiConfigurationFromFile(DUMMY_CREDENTIALS_FILE, opts);

        // assert
        assertThat((Collection<String>)configurationWithErrors.errors, is(empty()));
        ApiConfiguration apiConfiguration = configurationWithErrors.configuration;
        assertThat(apiConfiguration.getTokenUrl(), equalTo(TOKEN_URL));
        assertThat(apiConfiguration.getUsername(), equalTo(USERNAME));
        assertThat(apiConfiguration.getPassword(), equalTo(PASSWORD));
        assertThat(apiConfiguration.getClientId(), equalTo(CLIENT_ID));
        assertThat(apiConfiguration.getClientSecret(), equalTo(CLIENT_SECRET));
        assertThat(apiConfiguration.getPersonalAccessToken(), equalTo(ACCESS_TOKEN));
        assertThat(apiConfiguration.getApiUrl(), equalTo(API_URL));
        assertThat(apiConfiguration.getApplicationName(), equalTo(APP_NAME));
        assertThat(apiConfiguration.getProxyAddress(), equalTo(PROXY_ADDRESS));
        assertThat(apiConfiguration.getProxyPort(), equalTo(PROXY_PORT));
        assertThat(apiConfiguration.getProxyUsername(), equalTo(PROXY_USERNAME));
        assertThat(apiConfiguration.getProxyPassword(), equalTo(PROXY_PASSWORD));

        // config overridden by the opts
        assertThat(apiConfiguration.getTotalTimeoutMs(), equalTo(totalTimeoutMs));
        assertThat(apiConfiguration.getConnectTimeoutMs(), equalTo(connectTimeoutMs));
        assertThat(apiConfiguration.getReadTimeoutMs(), equalTo(readTimeoutMs));
        assertThat(apiConfiguration.getWriteTimeoutMs(), equalTo(writeTimeoutMs));
        assertThat(apiConfiguration.getRateLimitRetries(), equalTo(maxRetryAttempts));
    }

    private void verify_expectedApiConfigurationNoErrors(ConfigurationWithErrors configurationWithErrors) {
        assertThat((Collection<String>)configurationWithErrors.errors, is(empty()));
        ApiConfiguration apiConfiguration = configurationWithErrors.configuration;
        verify_expectedApiConfiguration(apiConfiguration);
    }

    private void verify_expectedApiConfiguration(ApiConfiguration apiConfiguration) {
        assertThat(apiConfiguration.getTokenUrl(), equalTo(TOKEN_URL));
        assertThat(apiConfiguration.getUsername(), equalTo(USERNAME));
        assertThat(apiConfiguration.getPassword(), equalTo(PASSWORD));
        assertThat(apiConfiguration.getClientId(), equalTo(CLIENT_ID));
        assertThat(apiConfiguration.getClientSecret(), equalTo(CLIENT_SECRET));
        assertThat(apiConfiguration.getPersonalAccessToken(), equalTo(ACCESS_TOKEN));
        assertThat(apiConfiguration.getApiUrl(), equalTo(API_URL));
        assertThat(apiConfiguration.getApplicationName(), equalTo(APP_NAME));
        assertThat(apiConfiguration.getTotalTimeoutMs(), equalTo(TOTAL_TIMEOUT_MS));
        assertThat(apiConfiguration.getConnectTimeoutMs(), equalTo(CONNECT_TIMEOUT_MS));
        assertThat(apiConfiguration.getReadTimeoutMs(), equalTo(READ_TIMEOUT_MS));
        assertThat(apiConfiguration.getWriteTimeoutMs(), equalTo(WRITE_TIMEOUT_MS));
        assertThat(apiConfiguration.getRateLimitRetries(), equalTo(RATE_LIMIT_RETRIES));
        assertThat(apiConfiguration.getProxyAddress(), equalTo(PROXY_ADDRESS));
        assertThat(apiConfiguration.getProxyPort(), equalTo(PROXY_PORT));
        assertThat(apiConfiguration.getProxyUsername(), equalTo(PROXY_USERNAME));
        assertThat(apiConfiguration.getProxyPassword(), equalTo(PROXY_PASSWORD));
    }

    @Test
    public void envVarsOverrideFileConfig_whenAllRequiredEnvVarsSpecified() throws ApiConfigurationException {
        // arrange
        environmentVariables
            .set("FBN_TOKEN_URL", TOKEN_URL)
            .set("FBN_USERNAME", USERNAME)
            .set("FBN_PASSWORD", PASSWORD)
            .set("FBN_CLIENT_ID", CLIENT_ID)
            .set("FBN_CLIENT_SECRET", CLIENT_SECRET)
            .set("FBN_ACCESS_TOKEN", ACCESS_TOKEN)
            .set("FBN_TO_BE_REPLACED_UPPER_SNAKECASE_URL", API_URL)
            .set("FBN_APP_NAME", APP_NAME)
            .set("FBN_TOTAL_TIMEOUT_MS", String.valueOf(TOTAL_TIMEOUT_MS))
            .set("FBN_CONNECT_TIMEOUT_MS", String.valueOf(CONNECT_TIMEOUT_MS))
            .set("FBN_READ_TIMEOUT_MS", String.valueOf(READ_TIMEOUT_MS))
            .set("FBN_WRITE_TIMEOUT_MS", String.valueOf(WRITE_TIMEOUT_MS))
            .set("FBN_RATE_LIMIT_RETRIES", String.valueOf(RATE_LIMIT_RETRIES))
            .set("FBN_PROXY_ADDRESS", PROXY_ADDRESS)
            .set("FBN_PROXY_PORT", String.valueOf(PROXY_PORT))
            .set("FBN_PROXY_USERNAME", PROXY_USERNAME)
            .set("FBN_PROXY_PASSWORD", PROXY_PASSWORD);
            ApiConfigurationBuilder apiConfigurationBuilder = new ApiConfigurationBuilder();

        // act
        ApiConfiguration configuration = apiConfigurationBuilder.build(ALTERNATE_DUMMY_CREDENTIALS_FILE);

        // assert
        verify_expectedApiConfiguration(configuration);
    }

    @Test
    public void fileConfigUsed_whenNotAllRequiredEnvVarsSpecified() throws ApiConfigurationException {
        // arrange
        // all env vars set except the url - since the url is a required config the env vars alone should be used
        environmentVariables
            .set("FBN_TOKEN_URL", "should not be used")
            .set("FBN_USERNAME", "should not be used")
            .set("FBN_PASSWORD", "should not be used")
            .set("FBN_CLIENT_ID", "should not be used")
            .set("FBN_CLIENT_SECRET", "should not be used")
            .set("FBN_ACCESS_TOKEN", "should not be used")
            .set("FBN_APP_NAME", "should not be used")
            .set("FBN_TOTAL_TIMEOUT_MS", "should not be used")
            .set("FBN_CONNECT_TIMEOUT_MS", "should not be used")
            .set("FBN_READ_TIMEOUT_MS", "should not be used")
            .set("FBN_WRITE_TIMEOUT_MS", "should not be used")
            .set("FBN_RATE_LIMIT_RETRIES", "should not be used")
            .set("FBN_PROXY_ADDRESS", "should not be used")
            .set("FBN_PROXY_PORT", "should not be used")
            .set("FBN_PROXY_USERNAME", "should not be used")
            .set("FBN_PROXY_PASSWORD", "should not be used");
            ApiConfigurationBuilder apiConfigurationBuilder = new ApiConfigurationBuilder();

        // act
        ApiConfiguration configuration = apiConfigurationBuilder.build(DUMMY_CREDENTIALS_FILE);

        // assert
        verify_expectedApiConfiguration(configuration);
    }

    @Test
    public void build_OnNonExistingConfigurationFile_ShouldThrowException() throws ApiConfigurationException {
        // arrange
        ApiConfigurationBuilder apiConfigurationBuilder = new ApiConfigurationBuilder();

        // act
        Exception exception = assertThrows(ApiConfigurationException.class, () -> {
            apiConfigurationBuilder.getApiConfigurationFromFile("does-not-exist.json", new ConfigurationOptions());
        });

        // assert
        assertThat(
            exception.getMessage(), 
            containsString("Error when loading details from configuration file: Cannot find 'does-not-exist.json' in either classpath resources or as an absolute path"));
    }

    @Test
    public void shortLivedAuthConfigAndUrlOnly_fromEnvironmentVariables_isValid() {
        // arrange
        environmentVariables
            .set("FBN_TOKEN_URL", TOKEN_URL)
            .set("FBN_USERNAME", USERNAME)
            .set("FBN_PASSWORD", PASSWORD)
            .set("FBN_CLIENT_ID", CLIENT_ID)
            .set("FBN_CLIENT_SECRET", CLIENT_SECRET)
            .set("FBN_TO_BE_REPLACED_UPPER_SNAKECASE_URL", API_URL);
        ApiConfigurationBuilder apiConfigurationBuilder = new ApiConfigurationBuilder();

        // act
        ConfigurationWithErrors configurationWithErrors = apiConfigurationBuilder.getApiConfigurationFromEnvironmentVariables(new ConfigurationOptions());

        // assert
        verify_shortLivedAuthConfigAndUrlOnly_isValid(configurationWithErrors);
    }

    @Test
    public void shortLivedAuthConfigAndUrlOnly_fromFile_isValid() throws ApiConfigurationException {
        // arrange
        ApiConfigurationBuilder apiConfigurationBuilder = new ApiConfigurationBuilder();

        // act
        ConfigurationWithErrors configurationWithErrors = apiConfigurationBuilder.getApiConfigurationFromFile(DUMMY_CREDENTIALS_FILE_SHORT_LIVED_AUTH, new ConfigurationOptions());

        // assert
        verify_shortLivedAuthConfigAndUrlOnly_isValid(configurationWithErrors);
    }

    private void verify_shortLivedAuthConfigAndUrlOnly_isValid(ConfigurationWithErrors configurationWithErrors) {
        // assert no errors
        assertThat((Collection<String>)configurationWithErrors.errors, is(empty()));
        ApiConfiguration apiConfiguration = configurationWithErrors.configuration;

        // check that specified config has been correctly set
        assertThat(apiConfiguration.getTokenUrl(), equalTo(TOKEN_URL));
        assertThat(apiConfiguration.getUsername(), equalTo(USERNAME));
        assertThat(apiConfiguration.getPassword(), equalTo(PASSWORD));
        assertThat(apiConfiguration.getClientId(), equalTo(CLIENT_ID));
        assertThat(apiConfiguration.getClientSecret(), equalTo(CLIENT_SECRET));
        assertThat(apiConfiguration.getApiUrl(), equalTo(API_URL));

        // check that non specified config is at default value
        assertThat(apiConfiguration.getPersonalAccessToken(), equalTo(null));
        assertThat(apiConfiguration.getApplicationName(), equalTo(null));
        assertThat(apiConfiguration.getTotalTimeoutMs(), equalTo(ApiConfigurationBuilder.DEFAULT_TOTAL_TIMEOUT_MS));
        assertThat(apiConfiguration.getConnectTimeoutMs(), equalTo(ApiConfigurationBuilder.DEFAULT_CONNECT_TIMEOUT_MS));
        assertThat(apiConfiguration.getReadTimeoutMs(), equalTo(ApiConfigurationBuilder.DEFAULT_READ_TIMEOUT_MS));
        assertThat(apiConfiguration.getWriteTimeoutMs(), equalTo(ApiConfigurationBuilder.DEFAULT_WRITE_TIMEOUT_MS));
        assertThat(apiConfiguration.getRateLimitRetries(), equalTo(ApiConfigurationBuilder.DEFAULT_RATE_LIMIT_RETRIES));
        assertThat(apiConfiguration.getProxyAddress(), equalTo(null));
        assertThat(apiConfiguration.getProxyPort(), equalTo(-1));
        assertThat(apiConfiguration.getProxyUsername(), equalTo(null));
        assertThat(apiConfiguration.getProxyPassword(), equalTo(null));
    }

    @Test
    public void longLivedAuthConfigAndUrlOnly_fromEnvironmentVariables_isValid() {
        // arrange
        environmentVariables
            .set("FBN_ACCESS_TOKEN", ACCESS_TOKEN)
            .set("FBN_TO_BE_REPLACED_UPPER_SNAKECASE_URL", API_URL);
        ApiConfigurationBuilder apiConfigurationBuilder = new ApiConfigurationBuilder();

        // act
        ConfigurationWithErrors configurationWithErrors = apiConfigurationBuilder.getApiConfigurationFromEnvironmentVariables(new ConfigurationOptions());

        // assert
        verify_longLivedAuthConfigAndUrlOnly_isValid(configurationWithErrors);
    }

    @Test
    public void longLivedAuthConfigAndUrlOnly_fromFile_isValid() throws ApiConfigurationException {
        // arrange
        ApiConfigurationBuilder apiConfigurationBuilder = new ApiConfigurationBuilder();

        // act
        ConfigurationWithErrors configurationWithErrors = apiConfigurationBuilder.getApiConfigurationFromFile(DUMMY_CREDENTIALS_FILE_LONG_LIVED_AUTH, new ConfigurationOptions());

        // assert
        verify_longLivedAuthConfigAndUrlOnly_isValid(configurationWithErrors);
    }

    private void verify_longLivedAuthConfigAndUrlOnly_isValid(ConfigurationWithErrors configurationWithErrors) {
        // assert no errors
        assertThat((Collection<String>)configurationWithErrors.errors, is(empty()));
        ApiConfiguration apiConfiguration = configurationWithErrors.configuration;

        // check that specified config has been correctly set
        assertThat(apiConfiguration.getPersonalAccessToken(), equalTo(ACCESS_TOKEN));
        assertThat(apiConfiguration.getApiUrl(), equalTo(API_URL));

        // check that non specified config is at default value
        assertThat(apiConfiguration.getTokenUrl(), equalTo(null));
        assertThat(apiConfiguration.getUsername(), equalTo(null));
        assertThat(apiConfiguration.getPassword(), equalTo(null));
        assertThat(apiConfiguration.getClientId(), equalTo(null));
        assertThat(apiConfiguration.getClientSecret(), equalTo(null));
        assertThat(apiConfiguration.getApplicationName(), equalTo(null));
        assertThat(apiConfiguration.getTotalTimeoutMs(), equalTo(ApiConfigurationBuilder.DEFAULT_TOTAL_TIMEOUT_MS));
        assertThat(apiConfiguration.getConnectTimeoutMs(), equalTo(ApiConfigurationBuilder.DEFAULT_CONNECT_TIMEOUT_MS));
        assertThat(apiConfiguration.getReadTimeoutMs(), equalTo(ApiConfigurationBuilder.DEFAULT_READ_TIMEOUT_MS));
        assertThat(apiConfiguration.getWriteTimeoutMs(), equalTo(ApiConfigurationBuilder.DEFAULT_WRITE_TIMEOUT_MS));
        assertThat(apiConfiguration.getRateLimitRetries(), equalTo(ApiConfigurationBuilder.DEFAULT_RATE_LIMIT_RETRIES));
        assertThat(apiConfiguration.getProxyAddress(), equalTo(null));
        assertThat(apiConfiguration.getProxyPort(), equalTo(-1));
        assertThat(apiConfiguration.getProxyUsername(), equalTo(null));
        assertThat(apiConfiguration.getProxyPassword(), equalTo(null));
    }

    @Test
    public void whenNoRequiredConfig_inEnvVars_returnsRelevantErrors() throws ApiConfigurationException {
        // arrange
        ApiConfigurationBuilder apiConfigurationBuilder = new ApiConfigurationBuilder();

        // act
        Exception exception = assertThrows(ApiConfigurationException.class, () -> {
            apiConfigurationBuilder.build();
        });

        // assert
        String expectedMessage = "Configuration parameters are not valid. The following issues were detected with the environment variables set: 'FBN_TOKEN_URL' was not set; 'FBN_USERNAME' was not set; 'FBN_PASSWORD' was not set; 'FBN_CLIENT_ID' was not set; 'FBN_CLIENT_SECRET' was not set; 'FBN_TO_BE_REPLACED_UPPER_SNAKECASE_URL' was not set. You may also use the String overload of this method to provide configuration via a configuration file.";

        assertThat(
            exception.getMessage(), 
            equalTo(expectedMessage));
    }

    @Test
    public void whenFileDoesNotContainApiSection_returnsRelevantError() throws ApiConfigurationException {
        // arrange
        ApiConfigurationBuilder apiConfigurationBuilder = new ApiConfigurationBuilder();

        // act
        Exception exception = assertThrows(ApiConfigurationException.class, () -> {
            apiConfigurationBuilder.build(DUMMY_CREDENTIALS_FILE_EMPTY);
        });

        // assert
        assertThat(
            exception.getMessage(), 
            containsString("The following issues were detected with the secrets file: configuration is missing required 'api' section"));
    }

    @Test
    public void whenNoRequiredConfig_inEnvVarsOrFile_returnsRelevantErrors() throws ApiConfigurationException {
        // arrange
        ApiConfigurationBuilder apiConfigurationBuilder = new ApiConfigurationBuilder();

        // act
        Exception exception = assertThrows(ApiConfigurationException.class, () -> {
            apiConfigurationBuilder.build(DUMMY_CREDENTIALS_FILE_API_SECTION_EMPTY);
        });

        // assert
        String expectedMessage = "Configuration parameters are not valid. Either all required environment variables must be set or else the configuration file 'dummy_credentials_api_section_empty.json' must contain all required configuration. The following issues were detected with the environment variables set: 'FBN_TOKEN_URL' was not set; 'FBN_USERNAME' was not set; 'FBN_PASSWORD' was not set; 'FBN_CLIENT_ID' was not set; 'FBN_CLIENT_SECRET' was not set; 'FBN_TO_BE_REPLACED_UPPER_SNAKECASE_URL' was not set. The following issues were detected with the secrets file: 'api.tokenUrl' was not set; 'api.username' was not set; 'api.password' was not set; 'api.clientId' was not set; 'api.clientSecret' was not set; 'api.TO_BE_REPLACEDUrl' was not set";

        assertThat(
            exception.getMessage(), 
            equalTo(expectedMessage));
    }

    @Test
    public void nonNumericsInEnvVars_reportedAsError() {
        // arrange
        environmentVariables
            .set("FBN_ACCESS_TOKEN", ACCESS_TOKEN)
            .set("FBN_TO_BE_REPLACED_UPPER_SNAKECASE_URL", API_URL)
            .set("FBN_TOTAL_TIMEOUT_MS", "40s")
            .set("FBN_CONNECT_TIMEOUT_MS", "10s")
            .set("FBN_READ_TIMEOUT_MS", "20s")
            .set("FBN_WRITE_TIMEOUT_MS", "30s")
            .set("FBN_RATE_LIMIT_RETRIES", "one");
        ApiConfigurationBuilder apiConfigurationBuilder = new ApiConfigurationBuilder();

        // act
        Exception exception = assertThrows(ApiConfigurationException.class, () -> {
            apiConfigurationBuilder.build();
        });

        // assert
        assertThat(
            exception.getMessage(), 
            containsString("'FBN_TOTAL_TIMEOUT_MS' is not a valid integer; 'FBN_CONNECT_TIMEOUT_MS' is not a valid integer; 'FBN_READ_TIMEOUT_MS' is not a valid integer; 'FBN_WRITE_TIMEOUT_MS' is not a valid integer; 'FBN_RATE_LIMIT_RETRIES' is not a valid integer"));
    }

    @Test
    public void nonNumericsInFile_reportedAsError() {
        // arrange
        ApiConfigurationBuilder apiConfigurationBuilder = new ApiConfigurationBuilder();

        // act
        Exception exception = assertThrows(ApiConfigurationException.class, () -> {
            apiConfigurationBuilder.build(DUMMY_CREDENTIALS_FILE_NON_NUMERIC_TIMEOUT);
        });

        // assert
        assertThat(
            exception.getMessage(), 
            containsString(" 'api.totalTimeoutMs' is not a valid integer; 'api.connectTimeoutMs' is not a valid integer; 'api.readTimeoutMs' is not a valid integer; 'api.writeTimeoutMs' is not a valid integer; 'api.rateLimitRetries' is not a valid integer"));
    }

    @Test
    public void nonIntegersInEnvVars_reportedAsError() {
        // arrange
        environmentVariables
            .set("FBN_ACCESS_TOKEN", ACCESS_TOKEN)
            .set("FBN_TO_BE_REPLACED_UPPER_SNAKECASE_URL", API_URL)
            .set("FBN_TOTAL_TIMEOUT_MS", "40.5")
            .set("FBN_CONNECT_TIMEOUT_MS", "10.5")
            .set("FBN_READ_TIMEOUT_MS", "20.5")
            .set("FBN_WRITE_TIMEOUT_MS", "30.5")
            .set("FBN_RATE_LIMIT_RETRIES", "1.5");
        ApiConfigurationBuilder apiConfigurationBuilder = new ApiConfigurationBuilder();

        // act
        Exception exception = assertThrows(ApiConfigurationException.class, () -> {
            apiConfigurationBuilder.build();
        });

        // assert
        assertThat(
            exception.getMessage(), 
            containsString("'FBN_TOTAL_TIMEOUT_MS' is not a valid integer; 'FBN_CONNECT_TIMEOUT_MS' is not a valid integer; 'FBN_READ_TIMEOUT_MS' is not a valid integer; 'FBN_WRITE_TIMEOUT_MS' is not a valid integer; 'FBN_RATE_LIMIT_RETRIES' is not a valid integer"));
    }

    @Test
    public void nonIntegersInFile_reportedAsError() {
        // arrange
        ApiConfigurationBuilder apiConfigurationBuilder = new ApiConfigurationBuilder();

        // act
        Exception exception = assertThrows(ApiConfigurationException.class, () -> {
            apiConfigurationBuilder.build(DUMMY_CREDENTIALS_FILE_NON_INTEGER_TIMEOUT);
        });

        // assert
        assertThat(
            exception.getMessage(), 
            containsString("'api.totalTimeoutMs' is not a valid integer; 'api.connectTimeoutMs' is not a valid integer; 'api.readTimeoutMs' is not a valid integer; 'api.writeTimeoutMs' is not a valid integer; 'api.rateLimitRetries' is not a valid integer"));
    }

    @Test
    public void nonPositivesInEnvVars_reportedAsError() {
        // arrange
        environmentVariables
            .set("FBN_ACCESS_TOKEN", ACCESS_TOKEN)
            .set("FBN_TO_BE_REPLACED_UPPER_SNAKECASE_URL", API_URL)
            .set("FBN_TOTAL_TIMEOUT_MS", "-1")
            .set("FBN_CONNECT_TIMEOUT_MS", "-1")
            .set("FBN_READ_TIMEOUT_MS", "-1")
            .set("FBN_WRITE_TIMEOUT_MS", "-1")
            .set("FBN_RATE_LIMIT_RETRIES", "-1");
        ApiConfigurationBuilder apiConfigurationBuilder = new ApiConfigurationBuilder();

        // act
        Exception exception = assertThrows(ApiConfigurationException.class, () -> {
            apiConfigurationBuilder.build();
        });

        // assert
        assertThat(
            exception.getMessage(), 
            containsString("'FBN_TOTAL_TIMEOUT_MS' must be a positive integer between 0 and 2147483647; 'FBN_CONNECT_TIMEOUT_MS' must be a positive integer between 0 and 2147483647; 'FBN_READ_TIMEOUT_MS' must be a positive integer between 0 and 2147483647; 'FBN_WRITE_TIMEOUT_MS' must be a positive integer between 0 and 2147483647; 'FBN_RATE_LIMIT_RETRIES' must be a positive integer between 0 and 2147483647"));
    }

    @Test
    public void nonPositivesInFile_reportedAsError() {
        // arrange
        ApiConfigurationBuilder apiConfigurationBuilder = new ApiConfigurationBuilder();

        // act
        Exception exception = assertThrows(ApiConfigurationException.class, () -> {
            apiConfigurationBuilder.build(DUMMY_CREDENTIALS_FILE_NON_POSITIVE_TIMEOUT);
        });

        // assert
        assertThat(
            exception.getMessage(), 
            containsString("'api.totalTimeoutMs' must be a positive integer between 0 and 2147483647; 'api.connectTimeoutMs' must be a positive integer between 0 and 2147483647; 'api.readTimeoutMs' must be a positive integer between 0 and 2147483647; 'api.writeTimeoutMs' must be a positive integer between 0 and 2147483647; 'api.rateLimitRetries' must be a positive integer between 0 and 2147483647"));
    }

    @Test
    public void usesOldStyleApiUrl_inEnvVars() {
        // arrange
        environmentVariables
            .set("FBN_ACCESS_TOKEN", ACCESS_TOKEN)
            .set("FBN_TO_BE_REPLACED_UPPER_SNAKECASE_API_URL", API_URL);
        ApiConfigurationBuilder apiConfigurationBuilder = new ApiConfigurationBuilder();

        // act
        ConfigurationWithErrors configurationWithErrors = apiConfigurationBuilder.getApiConfigurationFromEnvironmentVariables(new ConfigurationOptions());

        // assert
        verify_longLivedAuthConfigAndUrlOnly_isValid(configurationWithErrors);
    }

    @Test
    public void usesOldStyleApiUrl_inFile() throws ApiConfigurationException {
        ApiConfigurationBuilder apiConfigurationBuilder = new ApiConfigurationBuilder();

        // act
        ConfigurationWithErrors configurationWithErrors = apiConfigurationBuilder.getApiConfigurationFromFile(DUMMY_CREDENTIALS_FILE_OLD_STYLE_API_URL, new ConfigurationOptions());

        // assert
        verify_longLivedAuthConfigAndUrlOnly_isValid(configurationWithErrors);
    }
}
