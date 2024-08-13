package com.finbourne.TO_BE_REPLACED.integration.extensions.auth;

import com.finbourne.TO_BE_REPLACED.extensions.*;
import com.finbourne.TO_BE_REPLACED.extensions.auth.*;
import com.finbourne.TO_BE_REPLACED.integration.extensions.CredentialsSource;

import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.mockito.Mockito.*;

public class RefreshingTokenProviderIT {

    private RefreshingTokenProvider tokenProvider;
    private HttpFinbourneTokenProvider httpFinbourneTokenProvider;

    @Before
    public void setUp() throws ApiConfigurationException {
        ApiConfiguration apiConfiguration = new ApiConfigurationBuilder().build(CredentialsSource.credentialsFile);
        OkHttpClient httpClient = new OkHttpClient.Builder().build();
        httpFinbourneTokenProvider = new HttpFinbourneTokenProvider(apiConfiguration, httpClient);
        RefreshingTokenProvider instanceToSpy = new RefreshingTokenProvider(httpFinbourneTokenProvider);
        tokenProvider = spy(instanceToSpy);
    }

    @Test
    public void get_OnNoCurrentToken_ShouldReturnNewToken() throws FinbourneTokenException {
        FinbourneToken FinbourneToken = tokenProvider.get();
        assertThat(FinbourneToken.getAccessToken(), not(isEmptyOrNullString()));
        assertThat(FinbourneToken.getRefreshToken(), not(isEmptyOrNullString()));
        assertThat(FinbourneToken.getExpiresAt(), not(nullValue()));
    }

    @Test
    public void get_OnNonExpiredCurrentToken_ShouldReturnSameToken() throws FinbourneTokenException {
        // first call should create a token
        FinbourneToken FinbourneToken = tokenProvider.get();

        // mock token not expired
        doReturn(false).when(tokenProvider).isTokenExpired(FinbourneToken);

        // second call return same token as it has not expired
        FinbourneToken nextFinbourneToken = tokenProvider.get();

        assertThat(nextFinbourneToken, sameInstance(FinbourneToken));
    }

    @Test
    public void get_OnExpiredCurrentToken_ShouldReturnNewToken() throws FinbourneTokenException {
        // first call should create a token
        FinbourneToken FinbourneToken = tokenProvider.get();

        // mock token expired
        doReturn(true).when(tokenProvider).isTokenExpired(FinbourneToken);

        // second call should return a new token as the current one has expired
        FinbourneToken nextFinbourneToken = tokenProvider.get();

        assertThat(nextFinbourneToken.getAccessToken(), not(isEmptyOrNullString()));
        assertThat(nextFinbourneToken.getRefreshToken(), not(isEmptyOrNullString()));
        assertThat(nextFinbourneToken.getExpiresAt(), not(nullValue()));

        assertThat(nextFinbourneToken, not(equalTo(FinbourneToken)));
        assertThat(nextFinbourneToken.getAccessToken(), not(equalTo(FinbourneToken.getAccessToken())));
        assertThat(nextFinbourneToken.getExpiresAt(), not(equalTo(FinbourneToken.getExpiresAt())));
        // although a new token the refresh token parameter should remain constant
        assertThat(nextFinbourneToken.getRefreshToken(), equalTo(FinbourneToken.getRefreshToken()));
    }

    @Test
    public void get_OnFailedRefreshToken_ShouldGetNewToken() throws FinbourneTokenException {

        httpFinbourneTokenProvider = mock(HttpFinbourneTokenProvider.class);

        // synthesise an error getting the refresh token
        IOException ioException = new IOException("bad-connection");
        FinbourneTokenException tokenException = new FinbourneTokenException("token-error", ioException);

        // mock retrieving initial token that then expires
        FinbourneToken expiredToken = new FinbourneToken("access_01", "refresh_01", LocalDateTime.MIN);
        FinbourneToken refreshedToken = new FinbourneToken("access_02", "refresh_02", LocalDateTime.MAX);
        doReturn(expiredToken, refreshedToken).when(httpFinbourneTokenProvider).get(Optional.empty());

        // exception when attempting to get the refresh token
        doThrow(tokenException).when(httpFinbourneTokenProvider).get(Optional.of(expiredToken.getRefreshToken()));

        RefreshingTokenProvider instanceToSpy = new RefreshingTokenProvider(httpFinbourneTokenProvider);
        tokenProvider = spy(instanceToSpy);

        // get the initial token
        tokenProvider.get();

        // attempt to get the refresh token
        FinbourneToken nextToken = tokenProvider.get();

        // failed refresh attempt results in getting a new token
        assertThat(nextToken, equalTo(refreshedToken));
    }

}
