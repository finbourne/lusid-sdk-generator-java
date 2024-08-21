package com.finbourne.TO_BE_REPLACED.unit.extensions;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.finbourne.TO_BE_REPLACED.ApiCallback;
import com.finbourne.TO_BE_REPLACED.ApiClient;
import com.finbourne.TO_BE_REPLACED.ApiException;
import com.finbourne.TO_BE_REPLACED.TestConstants;
import com.finbourne.TO_BE_REPLACED.api.TEST_API;
import com.finbourne.TO_BE_REPLACED.extensions.ApiClientBuilder;
import com.finbourne.TO_BE_REPLACED.extensions.ApiConfiguration;
import com.finbourne.TO_BE_REPLACED.extensions.ApiConfigurationBuilder;
import com.finbourne.TO_BE_REPLACED.extensions.ApiConfigurationException;
import com.finbourne.TO_BE_REPLACED.extensions.ConfigurationOptions;
import com.finbourne.TO_BE_REPLACED.extensions.RateLimitRetryInterceptor;
import com.finbourne.TO_BE_REPLACED.extensions.auth.FinbourneTokenException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;


public class ConfigurationOptionsOverridesTests {

    // used in the asynchronous api calls
    private class TestCallBack<T> implements ApiCallback<T> {

        private CompletableFuture<Void> future;

        public TestCallBack(CompletableFuture<Void> future) {
            this.future = future;

        }

        @Override
        public void onFailure(ApiException e, int statusCode, Map responseHeaders) {
            future.completeExceptionally(e);
        }

        @Override
        public void onSuccess(Object result, int statusCode, Map responseHeaders) {
            System.out.println("call succeeded");
            future.complete(null);
        }

        @Override
        public void onUploadProgress(long bytesWritten, long contentLength, boolean done) {
            future.completeExceptionally(new UnsupportedOperationException("Unimplemented method 'onUploadProgress'"));
        }

        @Override
        public void onDownloadProgress(long bytesRead, long contentLength, boolean done) {
            future.completeExceptionally(new UnsupportedOperationException("Unimplemented method 'onDownloadProgress'"));
        }

    }

    private Response response = new Response.Builder()
        .request(new Request.Builder()
            .url("http://localhost")
            .build())
        .protocol(Protocol.HTTP_2)
        .code(200)
        .message("success")
        .build();

    // set up the enqueue method to run the callback on another thread
    private Call getMockedAsyncCall(CompletableFuture<Void> future) {
        Call call = mock(Call.class);
        doAnswer(invocation -> {
            Callback callback = (Callback)invocation.getArguments()[0];
            Thread thread = new Thread(() -> {
                try {
                    callback.onResponse(call, response);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            });
            thread.start();
            return null;
        }).when(call).enqueue(any());
        return call;
    }

    @Test
    public void ConfigurationOptionsSetForSyncRequest() throws ApiException, IOException, ApiConfigurationException, FinbourneTokenException, InterruptedException, ExecutionException {
        // setup a mock call we can use to avoid hitting the api
        // have it return a dummy response on execute
        Call call = mock(Call.class);
        when(call.execute()).thenReturn(response);

        // use the synchronous method to call the api
        ConfigurationOptionsSetForRequest(call, (apiInstance, opts) -> {
            try {
                apiInstance.TEST_METHOD.execute(opts);
            } catch (Exception e) {
                e.printStackTrace();
                Assertions.fail(e);
            }
            return null;
        });
    }

    @Test
    public void ConfigurationOptionsSetForAsyncRequest() throws ApiException, IOException, ApiConfigurationException, FinbourneTokenException, InterruptedException, ExecutionException {
        // setup a mock call we can use to avoid hitting the api
        CompletableFuture<Void> future = new CompletableFuture<>();
        Call call = getMockedAsyncCall(future);

        // use the asynchronous method to call the api
        ConfigurationOptionsSetForRequest(call, (apiInstance, opts) -> {
            try {
                apiInstance.TEST_METHOD.executeAsync(new TestCallBack<>(future), opts);
                future.get();
            } catch (Exception e) {
                e.printStackTrace();
                Assertions.fail(e);
            }
            return null;
        });
    }

    private void ConfigurationOptionsSetForRequest(
        Call call,
        BiFunction<TEST_API, ConfigurationOptions, Void> callApiFunction
    ) throws ApiException, IOException, ApiConfigurationException, FinbourneTokenException, InterruptedException, ExecutionException {
        // create an http client that's a partial mock so that we can perform verifications on the objects created from it
        ApiConfiguration apiConfiguration = new ApiConfigurationBuilder().build(TestConstants.DUMMY_CREDENTIALS_FILE);
        OkHttpClient httpClient = spy(new ApiClientBuilder().build(apiConfiguration).getHttpClient());

        // create the api instance from an ApiClient using the partial mock http client
        ApiClient apiClient = new ApiClient(httpClient);
        TEST_API apiInstance = new TEST_API(apiClient);

        // setup the http client to return a partial mock builder
        // the partial mock builder will create a new partial mock httpclient we can spy on
        // and the newCall method of the partial mock httpClient is mocked to avoid calling the api
        ArrayList<OkHttpClient> createdHttpClients = new ArrayList<>();
        when(httpClient.newBuilder()).thenAnswer(invocation -> {
            // create a partially mocked builder from the real builder
            Builder builder = spy((Builder)invocation.callRealMethod());

            // set it up so that we can access the http client it creates
            // but override newCall to avoid actually having to call the api
            when(builder.build()).thenAnswer(invocation2 -> {
                OkHttpClient secondHttpClient = spy((OkHttpClient)invocation2.callRealMethod());
                doReturn(call).when(secondHttpClient).newCall(any());
                createdHttpClients.add(secondHttpClient);
                return secondHttpClient;
            });

            return builder;
        });

        // act
        ConfigurationOptions opts = new ConfigurationOptions(4000, 1000, 2000, 3000, 5);
        callApiFunction.apply(apiInstance, opts);

        // assert that the created real http client has the expected overrides
        assertThat(createdHttpClients.size(), equalTo(1));
        OkHttpClient createdHttpClient = createdHttpClients.getFirst();
        verifyHttpClientConfig(createdHttpClient, opts);

        // assert that the initial http client still has the previous configuration
        OkHttpClient clientInApiInstance = apiInstance.getApiClient().getHttpClient();
        verifyHttpClientConfig(clientInApiInstance, apiConfiguration);
    }

    @Test
    public void NoIssuesWhenNoOverride_SyncRequest() throws ApiConfigurationException, FinbourneTokenException, ApiException, IOException {
        Call call = mock(Call.class);
        when(call.execute()).thenReturn(response);

        NoIssuesWhenNoOverride(call, apiInstance -> {
            try {
                apiInstance.TEST_METHOD.execute();
            } catch (Exception e) {
                e.printStackTrace();
                Assertions.fail(e);
            }
            return null;
        });
    }

    @Test
    public void NoIssuesWhenNoOverride_AsyncRequest() throws ApiConfigurationException, FinbourneTokenException, ApiException, IOException {
        CompletableFuture<Void> future = new CompletableFuture<>();
        Call call = getMockedAsyncCall(future);
        
        NoIssuesWhenNoOverride(call, apiInstance -> {
            try {
                apiInstance.TEST_METHOD.executeAsync(new TestCallBack<>(future));
                future.get();
            } catch (Exception e) {
                e.printStackTrace();
                Assertions.fail(e);
            }
            return null;
        });
    }

    private void NoIssuesWhenNoOverride(
        Call call,
        Function<TEST_API, Void> callApiFunction
    ) throws ApiConfigurationException, FinbourneTokenException, ApiException, IOException {
        // create an http client that's a partial mock so that we can override the api call
        ApiConfiguration apiConfiguration = new ApiConfigurationBuilder().build(TestConstants.DUMMY_CREDENTIALS_FILE);
        OkHttpClient httpClient = spy(new ApiClientBuilder().build(apiConfiguration).getHttpClient());

        // create the api instance from an ApiClient using the partial mock http client
        ApiClient apiClient = new ApiClient(httpClient);
        TEST_API apiInstance = new TEST_API(apiClient);

        // override newcall in the partial mock http client to avoid calling the api
        doReturn(call).when(httpClient).newCall(any());

        // act
        callApiFunction.apply(apiInstance);

        // assert that the http client still has the expected configuration
        verifyHttpClientConfig(httpClient, apiConfiguration);
    }

    @Test
    public void EmptyConfigurationOptionsIgnored_AsyncRequest() throws ApiConfigurationException, FinbourneTokenException, ApiException, IOException {
        CompletableFuture<Void> future = new CompletableFuture<>();
        Call call = getMockedAsyncCall(future);

        EmptyConfigurationOptionsIgnored(call, (apiInstance, opts) -> {
            try {
                apiInstance.TEST_METHOD.executeAsync(new TestCallBack<>(future), opts);
                future.get();
            } catch (Exception e) {
                e.printStackTrace();
                Assertions.fail(e);
            }
            return null;
        });
    }

    @Test
    public void EmptyConfigurationOptionsIgnored_SyncRequest() throws ApiConfigurationException, FinbourneTokenException, ApiException, IOException {
        Call call = mock(Call.class);

        EmptyConfigurationOptionsIgnored(call, (apiInstance, opts) -> {
            try {
                apiInstance.TEST_METHOD.execute(opts);
            } catch (Exception e) {
                e.printStackTrace();
                Assertions.fail(e);
            }
            return null;
        });
    }

    private void EmptyConfigurationOptionsIgnored(
        Call call,
        BiFunction<TEST_API, ConfigurationOptions, Void> callApiFunction
    ) throws ApiConfigurationException, FinbourneTokenException, ApiException, IOException {
        // create an http client that's a partial mock so that we can override the api call
        ApiConfiguration apiConfiguration = new ApiConfigurationBuilder().build(TestConstants.DUMMY_CREDENTIALS_FILE);
        OkHttpClient httpClient = spy(new ApiClientBuilder().build(apiConfiguration).getHttpClient());

        // create the api instance from an ApiClient using the partial mock http client
        ApiClient apiClient = new ApiClient(httpClient);
        TEST_API apiInstance = new TEST_API(apiClient);

        // override newcall in the partial mock http client to avoid calling the api
        doReturn(call).when(httpClient).newCall(any());
        when(call.execute()).thenReturn(response);

        // act
        ConfigurationOptions opts = new ConfigurationOptions();
        callApiFunction.apply(apiInstance, opts);

        // assert that the http client still has the expected configuration
        verifyHttpClientConfig(httpClient, apiConfiguration);
    }

    private void verifyHttpClientConfig(OkHttpClient httpClient, ConfigurationOptions opts) {
        assertThat(httpClient.callTimeoutMillis(), equalTo(opts.getTotalTimeoutMs()));
        assertThat(httpClient.connectTimeoutMillis(), equalTo(opts.getConnectTimeoutMs()));
        assertThat(httpClient.readTimeoutMillis(), equalTo(opts.getReadTimeoutMs()));
        assertThat(httpClient.writeTimeoutMillis(), equalTo(opts.getWriteTimeoutMs()));
        verifyRateLimitRetries(httpClient, opts.getRateLimitRetries());
    }

    private void verifyHttpClientConfig(OkHttpClient httpClient, ApiConfiguration apiConfiguration) {
        assertThat(httpClient.callTimeoutMillis(), equalTo(apiConfiguration.getTotalTimeoutMs()));
        assertThat(httpClient.connectTimeoutMillis(), equalTo(apiConfiguration.getConnectTimeoutMs()));
        assertThat(httpClient.readTimeoutMillis(), equalTo(apiConfiguration.getReadTimeoutMs()));
        assertThat(httpClient.writeTimeoutMillis(), equalTo(apiConfiguration.getWriteTimeoutMs()));
        verifyRateLimitRetries(httpClient, apiConfiguration.getRateLimitRetries());
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