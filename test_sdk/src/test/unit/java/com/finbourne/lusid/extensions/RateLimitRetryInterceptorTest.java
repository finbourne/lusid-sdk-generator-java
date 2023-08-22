package com.finbourne.lusid.extensions;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import org.junit.Test;

import com.finbourne.lusid.ApiCallback;
import com.finbourne.lusid.ApiClient;
import com.finbourne.lusid.ApiException;
import com.finbourne.lusid.ApiResponse;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

public class RateLimitRetryInterceptorTest {
        @Test
        public void constructInterceptorWithLessThanOneAttemptsThrowsException() {
                assertThrows(IllegalArgumentException.class, () -> new RateLimitRetryInterceptor(0));
        }

        @Test
        public void executeAttemptsNmkinus1TimesThenReturnsResult() throws IOException, ApiException {
                final int MAX_ATTEMPTS = 2;

                MockWebServer server = new MockWebServer();
                MockResponse mockTooManyRequestsResponse = new MockResponse()
                                .setResponseCode(429)
                                .addHeader("Retry-After", 10);
                MockResponse mockSuccessfulResponse = new MockResponse()
                                .setResponseCode(200);

                server.enqueue(mockTooManyRequestsResponse);
                server.enqueue(mockSuccessfulResponse);
                server.start();

                OkHttpClient client = new OkHttpClient.Builder()
                                .addInterceptor(new RateLimitRetryInterceptor(MAX_ATTEMPTS)).build();
                Request request = new Request.Builder()
                                .url(server.url(""))
                                .build();
                Call call = client.newCall(request);
                ApiClient apiClient = new ApiClient(client);
                ApiResponse<Object> response = apiClient.execute(call, null);
                assertEquals(200, response.getStatusCode());
                server.close();
        }

        @Test
        public void executeResponseHasNoRetryHeaderThrowsApiException() throws IOException, ApiException {
                final int MAX_ATTEMPTS = 2;

                MockWebServer server = new MockWebServer();
                MockResponse mockTooManyRequestsResponse = new MockResponse()
                                .setResponseCode(429);

                server.enqueue(mockTooManyRequestsResponse);
                server.start();

                OkHttpClient client = new OkHttpClient.Builder()
                                .addInterceptor(new RateLimitRetryInterceptor(MAX_ATTEMPTS)).build();
                Request request = new Request.Builder()
                                .url(server.url(""))
                                .build();
                Call call = client.newCall(request);
                ApiClient apiClient = new ApiClient(client);
                ApiException expectedException = new ApiException("Retry-After header unavailable", 429,
                                mockTooManyRequestsResponse.getHeaders().toMultimap(), "");
                ApiException exception = assertThrows(ApiException.class, () -> apiClient.execute(call, null));
                assertEquals(expectedException.getMessage(), exception.getMessage());
                server.close();
        }

        @Test
        public void executeResponseWithMalformedRetryHeaderThrowsApiException() throws IOException, ApiException {
                final int MAX_ATTEMPTS = 2;

                MockWebServer server = new MockWebServer();
                MockResponse mockTooManyRequestsResponse = new MockResponse()
                                .setResponseCode(429)
                                .addHeader("Retry-After", "25s");

                server.enqueue(mockTooManyRequestsResponse);
                server.start();

                OkHttpClient client = new OkHttpClient.Builder()
                                .addInterceptor(new RateLimitRetryInterceptor(MAX_ATTEMPTS)).build();
                Request request = new Request.Builder()
                                .url(server.url(""))
                                .build();
                Call call = client.newCall(request);
                ApiClient apiClient = new ApiClient(client);
                ApiException expectedException = new ApiException("Failed to parse Retry-After header", 429,
                                mockTooManyRequestsResponse.getHeaders().toMultimap(), "");
                ApiException exception = assertThrows(ApiException.class, () -> apiClient.execute(call, null));
                assertEquals(expectedException.getMessage(), exception.getMessage());

                server.close();
        }

        @Test
        public void executeAttemptsNTimesThenThrowsAPIException() throws IOException, ApiException {
                final int MAX_ATTEMPTS = 2;

                MockWebServer server = new MockWebServer();
                MockResponse mockTooManyRequestsResponse = new MockResponse()
                                .setResponseCode(429)
                                .addHeader("Retry-After", 10);
                MockResponse mockSuccessfulResponse = new MockResponse()
                                .setResponseCode(200);

                server.enqueue(mockTooManyRequestsResponse);
                server.enqueue(mockTooManyRequestsResponse);
                server.enqueue(mockSuccessfulResponse);
                server.start();

                OkHttpClient client = new OkHttpClient.Builder()
                                .addInterceptor(new RateLimitRetryInterceptor(MAX_ATTEMPTS)).build();
                Request request = new Request.Builder()
                                .url(server.url(""))
                                .build();
                Call call = client.newCall(request);
                ApiClient apiClient = new ApiClient(client);
                assertThrows(ApiException.class, () -> apiClient.execute(call, null));
                server.close();
        }

        @Test
        @SuppressWarnings("unchecked")
        public void executeAsyncAttemptsNmkinus1TimesThenCallsOnSuccess() throws IOException, ApiException {
                final int MAX_ATTEMPTS = 2;
                MockWebServer server = new MockWebServer();
                MockResponse mockTooManyRequestsResponse = new MockResponse()
                                .setResponseCode(429)
                                .addHeader("Retry-After", 10);
                MockResponse mockSuccessfulResponse = new MockResponse()
                                .setResponseCode(200);

                server.enqueue(mockTooManyRequestsResponse);
                server.enqueue(mockSuccessfulResponse);
                server.start();

                OkHttpClient client = new OkHttpClient.Builder()
                                .addInterceptor(new RateLimitRetryInterceptor(MAX_ATTEMPTS)).build();
                Request request = new Request.Builder()
                                .url(server.url(""))
                                .build();
                Call call = client.newCall(request);

                ApiCallback<String> apiCallBackSpy = mock(ApiCallback.class);

                ApiClient apiClient = new ApiClient(client);
                apiClient.executeAsync(call, null, apiCallBackSpy);
                verify(apiCallBackSpy, timeout(100)).onSuccess(any(), eq(200), any());
                server.close();
        }

        @Test
        @SuppressWarnings("unchecked")
        public void executeAsyncAttemptsNTimesThenCallsOnFailure() throws IOException, ApiException {
                final int MAX_ATTEMPTS = 2;
                MockWebServer server = new MockWebServer();
                MockResponse mockTooManyRequestsResponse = new MockResponse()
                                .setResponseCode(429)
                                .addHeader("Retry-After", 10);
                MockResponse mockSuccessfulResponse = new MockResponse()
                                .setResponseCode(200);

                server.enqueue(mockTooManyRequestsResponse);
                server.enqueue(mockTooManyRequestsResponse);
                server.enqueue(mockSuccessfulResponse);
                server.start();

                OkHttpClient client = new OkHttpClient.Builder()
                                .addInterceptor(new RateLimitRetryInterceptor(MAX_ATTEMPTS)).build();
                Request request = new Request.Builder()
                                .url(server.url(""))
                                .build();
                Call call = client.newCall(request);

                ApiCallback<String> apiCallBackSpy = mock(ApiCallback.class);

                ApiClient apiClient = new ApiClient(client);
                apiClient.executeAsync(call, null, apiCallBackSpy);
                verify(apiCallBackSpy, timeout(100)).onFailure(any(), eq(429), any());
                verify(apiCallBackSpy, never()).onSuccess(any(), eq(200), any());
                server.close();
        }

}
