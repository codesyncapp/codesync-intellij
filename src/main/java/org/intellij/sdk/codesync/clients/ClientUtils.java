package org.intellij.sdk.codesync.clients;

import io.netty.channel.ConnectTimeoutException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.intellij.sdk.codesync.exceptions.InvalidJsonError;
import org.intellij.sdk.codesync.exceptions.RequestError;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;


public class ClientUtils {
    /*
    We will be using default timeout of 120 seconds for the following operations.
        1. `setSocketTimeout` will set timeout for http request.
        2. `setSocketTimeout` will set timeout for http connection.
        3. `setConnectionRequestTimeout` will set timeout for http connection request made to the request pool.
    */
    private static RequestConfig getRequestConfig () {
        return RequestConfig.custom()
            .setSocketTimeout(120 * 1000)
            .setConnectTimeout(120 * 1000)
            .setConnectionRequestTimeout(120 * 1000)
            .build();
    }
    private static HttpClientBuilder getHttpClientBuilder() {
        RequestConfig requestConfig = getRequestConfig();
        return HttpClientBuilder.create().setDefaultRequestConfig(requestConfig);
    }

    private static StringEntity getStringEntityFromJSONObject(JSONObject payload) throws InvalidJsonError {
        try {
            return new StringEntity(payload.toJSONString());
        } catch (UnsupportedEncodingException error) {
            throw new InvalidJsonError(
                String.format("Invalid JSON encoding error while authenticating the user. Error: %s", error.getMessage())
            );
        }
    }

    private static HttpGet getHttpGet(String url, String accessToken) {
        HttpGet httpGet = new HttpGet(url);

        // Make sure to mark all requests with content type "application/json".
        httpGet.addHeader("content-type", "application/json");

        if (accessToken != null) {
            httpGet.addHeader("Authorization", String.format("Basic %s", accessToken));
        }

        return httpGet;
    }

    private static HttpPost getHttpPost(String url, JSONObject JSONPayload, String accessToken) throws InvalidJsonError {
        HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(getStringEntityFromJSONObject(JSONPayload));

        // Make sure to mark all requests with content type "application/json".
        httpPost.addHeader("content-type", "application/json");

        if (accessToken != null) {
            httpPost.addHeader("Authorization", String.format("Basic %s", accessToken));
        }

        return httpPost;
    }

    private static HttpPatch getHttpPatch(String url, JSONObject JSONPayload, String accessToken) throws InvalidJsonError {
        HttpPatch httpPatch = new HttpPatch(url);
        httpPatch.setEntity(getStringEntityFromJSONObject(JSONPayload));

        // Make sure to mark all requests with content type "application/json".
        httpPatch.addHeader("content-type", "application/json");

        if (accessToken != null) {
            httpPatch.addHeader("Authorization", String.format("Basic %s", accessToken));
        }

        return httpPatch;
    }

    public static JSONResponse sendGet(String url, String accessToken) throws RequestError, InvalidJsonError {
        try (CloseableHttpClient httpClient = getHttpClientBuilder().build()) {
            // Build HTTP GET request instance.
            HttpGet httpGet = getHttpGet(url, accessToken);

            try (CloseableHttpResponse httpResponse = httpClient.execute(httpGet)) {
                return JSONResponse.from(httpResponse);
            } catch (SocketTimeoutException | ConnectTimeoutException error) {
                throw new RequestError("Request to CodeSync server timed out.");
            } catch (IOException error) {
                throw new RequestError("Could not make a successful request to CodeSync server.");
            }
        } catch (IOException error) {
            throw new RequestError(
                String.format("Could not make a successful request to CodeSync server. Error: %s", error.getMessage())
            );
        }
    }

    public static JSONResponse sendGet(String url) throws RequestError, InvalidJsonError {
        return sendGet(url, null);
    }

    public static JSONResponse sendPost(String url, JSONObject payload, String accessToken) throws RequestError, InvalidJsonError {
        try (CloseableHttpClient httpClient = getHttpClientBuilder().build()) {
            // Build HTTP POST request instance.
            HttpPost httpPost = getHttpPost(url, payload, accessToken);

            try (CloseableHttpResponse httpResponse = httpClient.execute(httpPost)) {
                return JSONResponse.from(httpResponse);
            } catch (SocketTimeoutException | ConnectTimeoutException error) {
                throw new RequestError("Request to CodeSync server timed out.");
            } catch (IOException error) {
                throw new RequestError("Could not make a successful request to CodeSync server.");
            }
        } catch (IOException error) {
            throw new RequestError(
                String.format("Could not make a successful request to CodeSync server. Error: %s", error.getMessage())
            );
        }
    }

    public static JSONResponse sendPost(String url, JSONObject payload) throws RequestError, InvalidJsonError {
        return sendPost(url, payload, null);
    }

    public static JSONResponse sendPatch(String url, JSONObject payload, String accessToken) throws RequestError, InvalidJsonError {
        try (CloseableHttpClient httpClient = getHttpClientBuilder().build()) {
            // Build HTTP PATCH request instance.
            HttpPatch httpPatch = getHttpPatch(url, payload, accessToken);

            try (CloseableHttpResponse httpResponse = httpClient.execute(httpPatch)) {
                return JSONResponse.from(httpResponse);
            } catch (SocketTimeoutException | ConnectTimeoutException error) {
                throw new RequestError("Request to CodeSync server timed out.");
            } catch (IOException error) {
                throw new RequestError("Could not make a successful request to CodeSync server.");
            }
        } catch (IOException error) {
            throw new RequestError(
                String.format("Could not make a successful request to CodeSync server. Error: %s", error.getMessage())
            );
        }
    }
}
