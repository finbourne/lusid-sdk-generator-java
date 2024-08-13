package com.finbourne.TO_BE_REPLACED.unit.extensions;

import com.finbourne.TO_BE_REPLACED.extensions.*;
import com.finbourne.TO_BE_REPLACED.ApiCallback;
import com.finbourne.TO_BE_REPLACED.ApiClient;
import com.finbourne.TO_BE_REPLACED.ApiException;
import com.finbourne.TO_BE_REPLACED.Pair;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import com.finbourne.TO_BE_REPLACED.extensions.auth.FinbourneToken;
import com.finbourne.TO_BE_REPLACED.extensions.auth.FinbourneTokenException;
import com.finbourne.TO_BE_REPLACED.extensions.auth.RefreshingTokenProvider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class AuthenticationInterceptorTest {
        // mock dependencies
        private RefreshingTokenProvider tokenProvider;

        // mock tokens
        private FinbourneToken FinbourneToken = new FinbourneToken("access_01", "refresh_01", LocalDateTime.now());
        private FinbourneToken anotherFinbourneToken = new FinbourneToken("access_02", "refresh_01",
                        LocalDateTime.now());

        @Rule
        public ExpectedException thrown = ExpectedException.none();

        @Before
        public void setUp() throws FinbourneTokenException {
                tokenProvider = mock(RefreshingTokenProvider.class);

                doReturn(FinbourneToken).when(tokenProvider).get();

        }

        @Test
        public void intercept_ShouldUpdateAuthHeader() throws ApiException, InterruptedException {
                MockWebServer server = new MockWebServer();
                server.enqueue(new MockResponse());
                OkHttpClient httpClient = new OkHttpClient.Builder()
                                .addInterceptor(new AuthenticationInterceptor(tokenProvider))
                                .build();
                Request request = new Request.Builder().url(server.url("")).build();
                Call call = httpClient.newCall(request);
                ApiClient client = new ApiClient(httpClient);
                client.execute(call, null);
                RecordedRequest request1 = server.takeRequest();
                assertEquals("Bearer access_01", request1.getHeader("Authorization"));
        }

        @Test
        public void intercept_ShouldUpdateAuthHeaderOnEveryCall()
                        throws ApiException, FinbourneTokenException, InterruptedException, IOException {
                MockWebServer server = new MockWebServer();
                server.enqueue(new MockResponse());
                server.enqueue(new MockResponse());
                OkHttpClient httpClient = new OkHttpClient.Builder()
                                .addInterceptor(new AuthenticationInterceptor(tokenProvider))
                                .build();
                Request request = new Request.Builder().url(server.url(""))
                                .addHeader("Authorization", "Bearer badtoken")
                                .build();
                Call call = httpClient.newCall(request);
                ApiClient client = new ApiClient(httpClient);
                client.execute(call, null);
                RecordedRequest request1 = server.takeRequest();
                assertEquals("Bearer access_01", request1.getHeader("Authorization"));
                // mock our token expiring and we now have an updated token to call api with
                doReturn(anotherFinbourneToken).when(tokenProvider).get();
                client.execute(call.clone(), null);
                RecordedRequest request2 = server.takeRequest();
                assertEquals("Bearer access_02", request2.getHeader("Authorization"));
                server.close();
        }

        @Test
        public void buildCall_OnExceptionRetrievingToken_ShouldThrowIOException()
                        throws FinbourneTokenException, IOException, ApiException {
                // mocking behaviour of an exception being thrown when attempting to retrieve
                // access token
                MockWebServer server = new MockWebServer();
                OkHttpClient httpClient = new OkHttpClient.Builder()
                                .addInterceptor(new AuthenticationInterceptor(tokenProvider))
                                .build();
                Request request = new Request.Builder().url(server.url(""))
                                .addHeader("Authorization", "Bearer badtoken")
                                .build();
                FinbourneTokenException FinbourneTokenException = new FinbourneTokenException(
                                "Failed to create token for some reason");
                doThrow(FinbourneTokenException).when(tokenProvider).get();
                Call call = httpClient.newCall(request);
                ApiClient client = new ApiClient(httpClient);

                thrown.expect(ApiException.class);
                thrown.expectCause(instanceOf(IOException.class));

                client.execute(call, null);
                server.close();
        }

}
