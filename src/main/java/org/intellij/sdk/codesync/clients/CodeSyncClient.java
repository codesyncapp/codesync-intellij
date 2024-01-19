package org.intellij.sdk.codesync.clients;

import kotlin.Pair;

import org.intellij.sdk.codesync.CodeSyncLogger;
import org.intellij.sdk.codesync.codeSyncSetup.S3FileUploader;
import org.intellij.sdk.codesync.exceptions.*;
import org.intellij.sdk.codesync.exceptions.response.StatusCodeError;
import org.intellij.sdk.codesync.files.ConfigRepo;
import org.intellij.sdk.codesync.files.DiffFile;
import static org.intellij.sdk.codesync.Constants.*;

import org.intellij.sdk.codesync.state.PluginState;
import org.intellij.sdk.codesync.state.StateUtils;
import org.intellij.sdk.codesync.utils.CodeSyncDateUtils;
import org.intellij.sdk.codesync.utils.FileUtils;
import org.intellij.sdk.codesync.alerts.PricingAlerts;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.net.HttpURLConnection;


public class CodeSyncClient {
    final String filesURL = FILES_API_ENDPOINT;
    static Map<String, CodeSyncWebSocketClient> codeSyncWebSocketClients = new HashMap<>();

    public CodeSyncClient() {

    }

    public Boolean isServerUp() {
        try {
            JSONResponse jsonResponse = ClientUtils.sendGet(API_HEALTHCHECK);
            return jsonResponse.getStatusCode() == 200;
        } catch (InvalidJsonError | RequestError error) {
            return false;
        } catch (StatusCodeError error) {
            CodeSyncLogger.error(
                String.format("Server returned %s status code on health check endpoint.", error.getStatusCode())
            );
            return false;
        }
    }

    /*
    Get the user email associated with the access token and validate if token is valid or not.

    @return  Pair<Boolean, String>  First item of the pair shows of token is valid and second is the user email.
     */
    public Pair<Boolean, String> getUser(String accessToken) throws RequestError {
        JSONObject response;
        try {
            JSONResponse jsonResponse = ClientUtils.sendGet(API_USERS, accessToken);
            response = jsonResponse.getJsonResponse();
        } catch (RequestError | InvalidJsonError error) {
            CodeSyncLogger.error(
                String.format("Could not make a successful request to CodeSync server. Error: %s", error.getMessage())
            );
            throw new RequestError("Could not make a successful request to CodeSync server.");
        } catch (StatusCodeError error) {
            CodeSyncLogger.error(
                String.format("Could not make a successful request to CodeSync server. Error: %s", error.getMessage())
            );
            throw new RequestError(
                String.format("Could not make a successful request to CodeSync server. %s", error.getMessage())
            );
        }

        // If server returned an error then token is not valid.
        boolean isTokenValid = !response.containsKey("error");
        if (!isTokenValid) {
            return new Pair<>(false, null);
        }

        try {
            String userEmail = (String) response.get("email");
            return new Pair<>(true, userEmail);
        } catch (ClassCastException err) {
            CodeSyncLogger.critical(String.format(
                "Error parsing the response of /users endpoint. Error: %s", err.getMessage()
            ));
            throw new RequestError("Error parsing the response from the server.");
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

    public Integer uploadFile(String accessToken, ConfigRepo configRepo, DiffFile diffFile, File originalsFile) throws FileInfoError, InvalidJsonError, RequestError, InvalidUsage {
        JSONObject payload = new JSONObject();
        Map<String, Object> fileInfo;
        try {
            fileInfo = FileUtils.getFileInfo(originalsFile.getAbsolutePath());
        } catch (FileInfoError error) {
            throw new FileInfoError(String.format("File Info could not be found for %s. Error: %s", diffFile.fileRelativePath, error.getMessage()));
        }

        payload.put("repo_id", configRepo.id);
        payload.put("branch", diffFile.branch);
        payload.put("commit_hash", diffFile.commitHash);
        payload.put("is_binary", (Boolean) fileInfo.get("isBinary"));
        payload.put("size", (long) fileInfo.get("size"));
        payload.put("file_path", diffFile.fileRelativePath);
        payload.put("created_at", CodeSyncDateUtils.formatDate(diffFile.createdAt));

        if (diffFile.addedAt != null) {
            payload.put("added_at", CodeSyncDateUtils.formatDate(diffFile.addedAt));
        }
        JSONResponse jsonResponse;
        try {
            jsonResponse = ClientUtils.sendPost(this.filesURL, payload, accessToken);
        } catch (RequestError | InvalidJsonError error) {
            throw new RequestError(String.format("Error processing response of the file upload  request. Error: %s", error.getMessage()));
        } catch (StatusCodeError statusCodeError) {
            if (statusCodeError.getStatusCode() == ErrorCodes.INVALID_USAGE) {
                throw new InvalidUsage(statusCodeError.getMessage());
            }

            if (statusCodeError.getStatusCode()  == ErrorCodes.REPO_SIZE_LIMIT_REACHED) {
                PluginState pluginState = StateUtils.getState(configRepo.repoPath);
                PricingAlerts.setPlanLimitReached(
                    accessToken,
                    configRepo.id,
                    pluginState != null ? pluginState.project: null
                );
            } else {
                PricingAlerts.resetPlanLimitReached();
            }

            throw new RequestError(String.format("Error processing response of the file upload  request. Error: %s", statusCodeError.getMessage()));
        }

        Long fileId;
        JSONObject responseObject = jsonResponse.getJsonResponse();
        if(responseObject.containsKey("error")) {
            throw new RequestError((String) ((JSONObject)responseObject.get("error")).get("message"));
        }

        try {
            fileId = (Long) responseObject.get("id");
        } catch (ClassCastException error) {
            throw new RequestError(String.format("Error processing response of the file upload  request. Error: %s", error.getMessage()));
        }

        if (fileId == null) {
            throw new RequestError(String.format(
                "Error processing response of the file upload request. fileId is null for '%s'.",
                diffFile.fileRelativePath
            ));
        }

        Map<String, Object> preSignedURLData;
        try {
            preSignedURLData = (Map<String, Object>) responseObject.get("url");

            long fileSize =  (long) fileInfo.get("size");
            if (fileSize > 0) {
                Map<String, Object> filePathAndURLs = new HashMap<>();
                filePathAndURLs.put(diffFile.fileRelativePath, preSignedURLData);
                S3FileUploader s3FileUploader = new S3FileUploader(configRepo.repoPath, diffFile.branch, filePathAndURLs);
                s3FileUploader.saveURLs();
            }
        } catch (ClassCastException error) {
            CodeSyncLogger.logConsoleMessage("Could not upload the file.");
            // this would probably mean that `url` is empty, and we can skip aws upload.
        } catch (InvalidYmlFileError | FileNotFoundException error) {
            CodeSyncLogger.critical(
                String.format("Error creating S3 upload queue file. Error: %s", error.getMessage())
            );
        }

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
    }

    public JSONObject uploadRepo(String accessToken, JSONObject payload) {
        JSONResponse jsonResponse;
        try {
            jsonResponse = ClientUtils.sendPost(API_INIT, payload, accessToken);
        } catch (RequestError | InvalidJsonError error) {
            CodeSyncLogger.critical(String.format("Error while repo init, %s", error.getMessage()));
            return null;
        } catch (StatusCodeError statusCodeError) {
            if (statusCodeError.getStatusCode()  == ErrorCodes.REPO_SIZE_LIMIT_REACHED) {
                PricingAlerts.setPlanLimitReached();
            } else {
                PricingAlerts.resetPlanLimitReached();
            }
            // In case of error status code, repo upload should stop.
            JSONObject errorResponse = new JSONObject();
            errorResponse.put("error", statusCodeError.getMessage());
            return errorResponse;
        }
        return jsonResponse.getJsonResponse();
    }

    public JSONObject updateRepo(String accessToken, int repoId, JSONObject payload) {
        String url = String.format("%s/%s?source=%s&v=%s", CODESYNC_REPO_URL, repoId, DIFF_SOURCE, PLUGIN_VERSION);
        try {
            JSONResponse jsonResponse = ClientUtils.sendPatch(url, payload, accessToken);
            return jsonResponse.getJsonResponse();
        } catch (RequestError | InvalidJsonError | StatusCodeError error) {
            CodeSyncLogger.critical(String.format("Error while repo update, %s", error.getMessage()));

            JSONObject errorResponse = new JSONObject();
            errorResponse.put("error", error.getMessage());
            return errorResponse;
        }
    }

    public JSONObject getRepoPlanInfo(String accessToken, int repoId) {
        String url = String.format("%s/%s/upgrade_plan?source=%s&v=%s", CODESYNC_REPO_URL, repoId, DIFF_SOURCE, PLUGIN_VERSION);

        try {
            JSONResponse jsonResponse = ClientUtils.sendGet(url, accessToken);
            return jsonResponse.getJsonResponse();
        } catch (RequestError | InvalidJsonError | StatusCodeError error) {
            CodeSyncLogger.error(String.format("Error while getting plan upgrade information. %s", error.getMessage()));
            return null;
        }
    }

    public JSONObject getTeamActivity(String accessToken) {
        try {
            JSONResponse jsonResponse = ClientUtils.sendGet(TEAM_ACTIVITY_ENDPOINT, accessToken);
            return jsonResponse.getJsonResponse();
        } catch (RequestError | InvalidJsonError | StatusCodeError error) {
            CodeSyncLogger.error(String.format("Error while getting team activity data. %s", error.getMessage()));
            return null;
        }
    }

}
