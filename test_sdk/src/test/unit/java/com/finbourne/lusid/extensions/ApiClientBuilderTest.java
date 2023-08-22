package com.finbourne.lusid.extensions;

import com.finbourne.lusid.ApiClient;
import com.finbourne.lusid.extensions.auth.FinbourneToken;
import com.finbourne.lusid.extensions.auth.FinbourneTokenException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import static org.mockito.Mockito.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;

public class ApiClientBuilderTest {

    private ApiClientBuilder apiClientBuilder;

    // mock dependencies
    private ApiClient apiClient;
    private OkHttpClient httpClient;
    private ApiConfiguration apiConfiguration;
    private FinbourneToken FinbourneToken;
    private String accessTokenResponseBody;

    // test helpers
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        httpClient = mock(OkHttpClient.class);
        apiConfiguration = mock(ApiConfiguration.class);
        FinbourneToken = mock(FinbourneToken.class);
        apiClient = mock(ApiClient.class);
        apiClientBuilder = spy(new ApiClientBuilder());
        accessTokenResponseBody = "{\"access_token\":\"token\", \"refresh_token\":\"refresh\", \"expires_in\":21}";

        // mock default well formed lusid token
        doReturn("access_token_01").when(FinbourneToken).getAccessToken();
        doReturn("benjals").when(apiConfiguration).getUsername();
        doReturn("dennison").when(apiConfiguration).getPassword();
        doReturn("shhhh").when(apiConfiguration).getClientSecret();
    }

    @Test
    public void createApiClient_OnProxyAddress_ShouldSetProxySettings()
            throws FinbourneTokenException, InterruptedException, IOException {
        MockWebServer server = new MockWebServer();
        String proxyHost = server.url("proxy").host();
        int proxyPort = (server.getPort());
        doReturn(proxyHost).when(apiConfiguration).getProxyAddress();
        doReturn(proxyPort).when(apiConfiguration).getProxyPort();

        server.enqueue(new MockResponse().setResponseCode(200)
                .setBody(accessTokenResponseBody));

        doReturn(server.url("").toString()).when(apiConfiguration).getTokenUrl();

        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
        ApiClient client = apiClientBuilder.build(apiConfiguration, 10, 10, 10, 10, clientBuilder);
        SocketAddress expectedSocketAddress = new InetSocketAddress(proxyHost, proxyPort);
        assertEquals(
                expectedSocketAddress.toString(),
                client.getHttpClient().proxy().address().toString());
        server.close();
    }

    @Test
    public void createApiClient_OnNoApplicationName_ShouldNotSetApplicationHeader() throws FinbourneTokenException {
        doReturn(null).when(apiConfiguration).getApplicationName();
        doReturn("http://example.com").when(apiConfiguration).getApiUrl();
        doReturn("").when(apiConfiguration).getPersonalAccessToken();

        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
        ApiClient client = apiClientBuilder.build(apiConfiguration, 10, 10, 10, 10, clientBuilder);
        Request.Builder requestBuilder = new Request.Builder().url("http://example.com");
        client.processHeaderParams(new HashMap<String, String>(), requestBuilder);
        Request result = requestBuilder.build();
        assertFalse(result.headers().names().contains("X-LUSID-Application"));
    }
}
