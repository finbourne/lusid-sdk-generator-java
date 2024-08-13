package com.finbourne.TO_BE_REPLACED.integration.extensions.auth;

import com.finbourne.TO_BE_REPLACED.extensions.*;
import com.finbourne.TO_BE_REPLACED.TestConstants;
import com.finbourne.TO_BE_REPLACED.extensions.auth.*;
import com.finbourne.TO_BE_REPLACED.integration.extensions.CredentialsSource;

import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isEmptyOrNullString;

public class HttpFinbourneTokenProviderIT {

    private HttpFinbourneTokenProvider httpFinbourneTokenProvider;

    @Before
    public void setUp() throws ApiConfigurationException {
        ApiConfiguration apiConfiguration = new ApiConfigurationBuilder().build(CredentialsSource.credentialsFile);
        OkHttpClient httpClient = new OkHttpClient.Builder().build();
        httpFinbourneTokenProvider = new HttpFinbourneTokenProvider(apiConfiguration, httpClient);
    }

    @Test
    public void get_OnRequestingAnInitialToken_ShouldReturnNewToken() throws FinbourneTokenException {
        FinbourneToken FinbourneToken = httpFinbourneTokenProvider.get(Optional.empty());

        assertThat(FinbourneToken.getAccessToken(), not(isEmptyOrNullString()));
        assertThat(FinbourneToken.getRefreshToken(), not(isEmptyOrNullString()));
        assertThat(FinbourneToken.getExpiresAt(), not(nullValue()));
    }

    @Test
    public void get_OnRequestingANewTokenWithRefreshing_ShouldReturnNewRefreshedToken() throws FinbourneTokenException {
        FinbourneToken initialToken = httpFinbourneTokenProvider.get(Optional.empty());
        // request a new access token based on the refresh parameter of our original
        // token
        FinbourneToken refreshedToken = httpFinbourneTokenProvider.get(Optional.of(initialToken.getRefreshToken()));

        assertThat(refreshedToken.getAccessToken(), not(isEmptyOrNullString()));
        assertThat(refreshedToken.getRefreshToken(), not(isEmptyOrNullString()));
        assertThat(refreshedToken.getExpiresAt(), not(nullValue()));
        // ensure our new token is in fact a new and different token
        assertThat(refreshedToken, not(equalTo(initialToken)));
    }

    @Test
    public void get_OnRequestingANewTokenWithoutRefreshing_ShouldReturnNewToken() throws FinbourneTokenException {
        FinbourneToken initialToken = httpFinbourneTokenProvider.get(Optional.empty());
        // request a new access token by going through a full reauthentication (i.e. not
        // a refresh)
        FinbourneToken aNewToken = httpFinbourneTokenProvider.get(Optional.empty());

        assertThat(aNewToken.getAccessToken(), not(isEmptyOrNullString()));
        assertThat(aNewToken.getRefreshToken(), not(isEmptyOrNullString()));
        assertThat(aNewToken.getExpiresAt(), not(nullValue()));
        // ensure our new token is in fact a new and different token
        assertThat(aNewToken, not(equalTo(initialToken)));
    }

    // Error cases
    @Test(expected = IllegalArgumentException.class)
    public void get_OnBadTokenUrl_ShouldThrowException() throws FinbourneTokenException, ApiConfigurationException {
        ApiConfiguration apiConfiguration = new ApiConfigurationBuilder().build(TestConstants.DUMMY_CREDENTIALS_FILE);
        OkHttpClient httpClient = new OkHttpClient.Builder().build();
        apiConfiguration.setTokenUrl("invalidTokenUrl");

        HttpFinbourneTokenProvider httpFinbourneTokenProvider = new HttpFinbourneTokenProvider(apiConfiguration,
                httpClient);
        httpFinbourneTokenProvider.get(Optional.empty());
    }
}
