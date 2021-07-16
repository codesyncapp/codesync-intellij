package org.intellij.sdk.codesync.clients;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.intellij.sdk.codesync.Utils;
import org.intellij.sdk.codesync.exceptions.*;
import org.intellij.sdk.codesync.files.ConfigRepo;
import org.intellij.sdk.codesync.files.DiffFile;
import static org.intellij.sdk.codesync.Constants.*;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.net.HttpURLConnection;


public class CodeSyncClient {
    final String filesURL = FILES_API_ENDPOINT;
    static Map<String, CodeSyncWebSocketClient> codeSyncWebSocketClients = new HashMap<>();

    public CodeSyncClient() {

    }

    public Boolean isServerUp() {
        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpGet healthCheck = new HttpGet(API_HEALTHCHECK);
        try {
            HttpResponse response = httpClient.execute(healthCheck);
            return response.getStatusLine().getStatusCode() == 200;
        } catch (IOException e) {
            return false;
        }
    }

    public CodeSyncWebSocketClient getWebSocketClient(String token) {
        if (codeSyncWebSocketClients.containsKey(token)) {
            return codeSyncWebSocketClients.get(token);
        } else {
            CodeSyncWebSocketClient codeSyncWebSocketClient = new CodeSyncWebSocketClient(token, WEBSOCKET_ENDPOINT);
            codeSyncWebSocketClients.put(token, codeSyncWebSocketClient);
            return codeSyncWebSocketClient;
        }
    }

    public Integer uploadFile(ConfigRepo configRepo, DiffFile diffFile, File originalsFile) throws FileInfoError, InvalidJsonError, RequestError {
        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost(this.filesURL);
        JSONObject data = new JSONObject();
        Map<String, Object> fileInfo;
        try {
            fileInfo = Utils.getFileInfo(originalsFile.getAbsolutePath());
        } catch (FileInfoError error) {
            throw new FileInfoError(String.format("File Info could not be found for %s", diffFile.fileRelativePath));
        }

        data.put("repo_id", configRepo.id);
        data.put("branch", diffFile.branch);
        data.put("is_binary", (Boolean) fileInfo.get("isBinary"));
        data.put("size", (long) fileInfo.get("size") + 1);
        data.put("file_path", diffFile.fileRelativePath);
        data.put("created_at", Utils.formatDate(diffFile.createdAt));

        StringEntity dataStr;
        try {
            dataStr = new StringEntity(data.toJSONString());
        } catch (UnsupportedEncodingException error) {
            throw new InvalidJsonError("Invalid JSON encoding error raised while uploading file to the server.");
        }

        post.setEntity(dataStr);
        post.setHeader("Content-type", "application/json");
        post.setHeader("Authorization", String.format("Basic %s", configRepo.token));

        HttpResponse response;
        try {
            response = httpClient.execute(post);
        } catch (IOException e) {
            throw new RequestError(String.format("Error uploading file. Error: %s", e.getMessage()));
        }

        String responseContent;
        try {
            responseContent = EntityUtils.toString(response.getEntity());
        } catch (IOException | org.apache.http.ParseException error) {
            throw new RequestError(String.format("Error processing response of the file upload  request. Error: %s", error.getMessage()));
        }

        Long fileId;
        JSONObject jsonResponse;
        try {
            jsonResponse = (JSONObject) JSONValue.parseWithException(responseContent);
            fileId = (Long) jsonResponse.get("id");
        } catch (org.json.simple.parser.ParseException | ClassCastException error) {
            throw new RequestError(String.format("Error processing response of the file upload  request. Error: %s", error.getMessage()));
        }

        Map<String, Object> preSignedURLData;
        try {
            preSignedURLData = (Map<String, Object>) jsonResponse.get("url");

            long fileSize =  (long) fileInfo.get("size") + 1;
            if (fileSize > 0) {
                this.uploadToS3(originalsFile, preSignedURLData);
            }
        } catch (ClassCastException error) {
            System.out.println("Could not upload the file.");
            // this would probably mean that `url` is empty and we can skip aws upload.
        }

        originalsFile.delete();
        return fileId.intValue();
    }

    public void uploadToS3(File originalsFile, Map<String, Object> preSignedURLData) throws RequestError {
        if (!originalsFile.exists()) {
            System.out.printf("Failed uploading new file, path: %s not found.", originalsFile.getPath());
            return;
        }
        Map<String, String> fields;
        try {
            fields = (Map<String, String>) preSignedURLData.get("fields");
        } catch (ClassCastException e) {
            fields = new HashMap<>();
        }

        String charset = "UTF-8";
        try {
            MultipartUtility multipart = new MultipartUtility((String) preSignedURLData.get("url"), charset);
            multipart.addHeaderField("User-Agent", "IntelliJ Plugin");

            for (Map.Entry<String, String> fieldEntry : fields.entrySet()) {
                if (fieldEntry.getValue() != null) {
                    multipart.addFormField(fieldEntry.getKey(), fieldEntry.getValue());
                }

            }

            multipart.addFilePart("file", originalsFile);
            multipart.finish(HttpURLConnection.HTTP_NO_CONTENT);
        } catch (IOException e) {
            throw new RequestError(String.format("Error uploading file. Error: %s", e.getMessage()));
        }

//        HttpClient httpClient = HttpClientBuilder.create().build();
//        HttpPost post = new HttpPost((String) preSignedURLData.get("url"));
//        HttpEntity entity = MultipartEntityBuilder.create().addPart("file", new FileBody(originalsFile)).build();
//        post.setEntity(entity);
//
//        HttpResponse response;
//        try {
//            response = httpClient.execute(post);
//        } catch (IOException e) {
//            throw new RequestError(String.format("Error uploading file. Error: %s", e.getMessage()));
//        }
//        if (response.getStatusLine().getStatusCode() == 204) {
//            System.out.printf("Successfully uploaded new file: %s.", originalsFile.getPath());
//        } else {
//            System.out.printf("Error uploading file. Error: %s.", originalsFile.getPath());
//        }
    }
}
