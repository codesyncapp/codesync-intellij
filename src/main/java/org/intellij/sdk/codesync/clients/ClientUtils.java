package org.intellij.sdk.codesync.clients;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.intellij.sdk.codesync.exceptions.InvalidJsonError;
import org.intellij.sdk.codesync.exceptions.RequestError;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.IOException;
import java.io.UnsupportedEncodingException;


public class ClientUtils {
    public static JSONObject sendPost(String url, JSONObject payload, String accessToken) throws RequestError, InvalidJsonError {
        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost(url);

        StringEntity dataStr;
        try {
            dataStr = new StringEntity(payload.toJSONString());
        } catch (UnsupportedEncodingException error) {
            System.out.println("Invalid JSON encoding error while authenticating the user.");
            throw new InvalidJsonError("Invalid JSON encoding error while authenticating the user.");
        }

        post.setEntity(dataStr);
        post.addHeader("content-type", "application/json");
        if (accessToken != null) {
            post.addHeader("Authorization", String.format("Basic %s", accessToken));
        }
        HttpResponse response;

        try {
            response = httpClient.execute(post);
        } catch (IOException e) {
            throw new RequestError("Could not make a successful request to CodeSync server.");
        }

        String responseContent;
        try {
            responseContent = EntityUtils.toString(response.getEntity());
        } catch (IOException | org.apache.http.ParseException error) {
            System.out.printf("Error processing response of the request. Error: %s%n", error.getMessage());
            throw new InvalidJsonError("Error processing response of the request.");
        }

        try {
            return (JSONObject) JSONValue.parseWithException(responseContent);
        } catch (org.json.simple.parser.ParseException | ClassCastException error) {
            System.out.printf("Error processing response of the request. Error: %s%n", error.getMessage());
            throw new InvalidJsonError("Error processing response of the request.");
        }
    }

    public static JSONObject sendPost(String url, JSONObject payload) throws RequestError, InvalidJsonError {
        return sendPost(url, payload, null);
    }

    public static JSONObject sendGet(String url, String accessToken) throws RequestError, InvalidJsonError {
        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet(url);

        get.addHeader("content-type", "application/json");

        if (accessToken != null) {
            get.addHeader("Authorization", String.format("Basic %s", accessToken));
        }

        HttpResponse response;
        try {
            response = httpClient.execute(get);
        } catch (IOException e) {
            throw new RequestError("Could not make a successful request to CodeSync server.");
        }

        String responseContent;
        try {
            responseContent = EntityUtils.toString(response.getEntity());
        } catch (IOException | org.apache.http.ParseException error) {
            System.out.printf("Error processing response of access token request. Error: %s%n", error.getMessage());
            throw new InvalidJsonError("Error processing response of access token request.");
        }

        try {
            return (JSONObject) JSONValue.parseWithException(responseContent);
        } catch (org.json.simple.parser.ParseException | ClassCastException error) {
            System.out.printf("Error processing response of the request. Error: %s%n", error.getMessage());
            throw new InvalidJsonError("Error processing response of the request.");
        }
    }

    public static JSONObject sendGet(String url) throws RequestError, InvalidJsonError {
        return sendGet(url, null);
    }
}
