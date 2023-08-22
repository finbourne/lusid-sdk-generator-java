package com.finbourne.lusid.extensions;

import com.finbourne.lusid.ApiCallback;
import com.finbourne.lusid.ApiClient;
import com.finbourne.lusid.ApiException;
import com.finbourne.lusid.Pair;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import com.finbourne.lusid.extensions.auth.FinbourneToken;
import com.finbourne.lusid.extensions.auth.FinbourneTokenException;
import com.finbourne.lusid.extensions.auth.RefreshingTokenProvider;
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

import static org.mockito.Mockito.*;

public class AuthenticationInterceptorTest {
        // mock dependencies
        private ApiClient defaultApiClient;
        private RefreshingTokenProvider tokenProvider;

        // mock tokens
        private FinbourneToken FinbourneToken = new FinbourneToken("access_01", "refresh_01", LocalDateTime.now());
        private FinbourneToken anotherFinbourneToken = new FinbourneToken("access_02", "refresh_01",
                        LocalDateTime.now());

        // call params
        private String path = "/get_portfolios";
        private String method = "GET";
        private List<Pair> queryParams = new ArrayList<>();
        private List<Pair> collectionQueryParams = new ArrayList<>();
        private Object body = new Object();
        private Map<String, String> headerParams = new HashMap<>();
        private Map<String, String> cookieParams = new HashMap<>();
        private Map<String, Object> formParams = new HashMap<>();
        private String[] authNames = new String[] {};
        private ApiCallback apiCallback = mock(ApiCallback.class);

        @Rule
        public ExpectedException thrown = ExpectedException.none();

        @Before
        public void setUp() throws FinbourneTokenException {
                defaultApiClient = mock(ApiClient.class);
                tokenProvider = mock(RefreshingTokenProvider.class);

                doReturn(FinbourneToken).when(tokenProvider).get();

        }

        @Test
        public void intercept_ShouldUpdateAuthHeader() throws ApiException, InterruptedException {
                MockWebServer server = new MockWebServer();
                OkHttpClient httpClient = new OkHttpClient.Builder()
                                .addInterceptor(new AuthenticationInterceptor(tokenProvider))
                                .build();
                Request request = new Request.Builder().url(server.url("")).build();
                Call call = httpClient.newCall(request);
                ApiClient client = new ApiClient(httpClient);
                client.execute(call, null);
                RecordedRequest request1 = server.takeRequest();
                assert request1.getHeader("Authorization") == FinbourneToken.getAccessToken();
        }

        @Test
        public void intercept_ShouldUpdateAuthHeaderOnEveryCall()
                        throws ApiException, FinbourneTokenException, InterruptedException, IOException {
                MockWebServer server = new MockWebServer();
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
                assert request1.getHeader("Authorization") == "Bearer access_01";
                // mock our token expiring and we now have an updated token to call api with
                doReturn(anotherFinbourneToken).when(tokenProvider).get();
                client.execute(call.clone(), null);
                RecordedRequest request2 = server.takeRequest();
                assert request2.getHeader("Authorization") == "Bearer access_02";
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
