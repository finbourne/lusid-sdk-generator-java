package com.finbourne.TO_BE_REPLACED.unit.extensions.auth;

import com.finbourne.TO_BE_REPLACED.extensions.*;
import com.finbourne.TO_BE_REPLACED.extensions.auth.HttpFinbourneTokenProvider;

import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

public class HttpFinbourneTokenProviderTest {

    private HttpFinbourneTokenProvider httpFinbourneTokenProvider;

    @Before
    public void setUp() {
        ApiConfiguration apiConfiguration = mock(ApiConfiguration.class);
        OkHttpClient httpClient = mock(OkHttpClient.class);
        httpFinbourneTokenProvider = new HttpFinbourneTokenProvider(apiConfiguration, httpClient);
    }

    @Test
    public void calculateExpiryAtTime_ShouldApplyThirtySecondCut() {
        LocalDateTime authTime = LocalDateTime.of(2020, 01, 01, 00, 00, 00);
        int expiryIn = 3600;

        LocalDateTime expiryAt = httpFinbourneTokenProvider.calculateExpiryAtTime(authTime, expiryIn);

        assertThat("Expiry At time should be an hour ahead of the time of auth call minus a 30 second gap for " +
                "to allow for retrieving for a new token before expiry token.",
                expiryAt,
                equalTo(LocalDateTime.of(2020, 01, 01, 00, 59, 30)));
    }

}
