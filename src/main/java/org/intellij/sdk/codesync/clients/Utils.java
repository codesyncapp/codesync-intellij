package org.intellij.sdk.codesync.clients;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class Utils {
    public static JSONObject sendPost(String url, JSONObject payload, String accessToken) {
        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost(url);

        StringEntity dataStr;
        try {
            dataStr = new StringEntity(payload.toJSONString());
        } catch (UnsupportedEncodingException error) {
            System.out.println("Invalid JSON encoding error while authenticating the user.");
            return null;
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
            return null;
        }

        String responseContent;
        try {
            responseContent = EntityUtils.toString(response.getEntity());
        } catch (IOException | org.apache.http.ParseException error) {
            System.out.printf("Error processing response of access token request. Error: %s%n", error.getMessage());
            return null;
        }

        try {
            return (JSONObject) JSONValue.parseWithException(responseContent);
        } catch (org.json.simple.parser.ParseException | ClassCastException error) {
            System.out.printf("Error processing response of the file upload  request. Error: %s%n", error.getMessage());
            return null;
        }
    }

    public static JSONObject sendPost(String url, JSONObject payload) {
        return sendPost(url, payload, null);
    }
}
