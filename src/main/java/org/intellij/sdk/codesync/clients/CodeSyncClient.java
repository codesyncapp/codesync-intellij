package org.intellij.sdk.codesync.clients;

import kotlin.Pair;

import org.intellij.sdk.codesync.CodeSyncLogger;
import org.intellij.sdk.codesync.exceptions.*;
import org.intellij.sdk.codesync.exceptions.response.StatusCodeError;
import org.intellij.sdk.codesync.files.ConfigRepo;
import org.intellij.sdk.codesync.files.DiffFile;
import static org.intellij.sdk.codesync.Constants.*;
import org.intellij.sdk.codesync.models.User;

import org.intellij.sdk.codesync.models.UserPlan;
import org.intellij.sdk.codesync.state.PluginState;
import org.intellij.sdk.codesync.state.StateUtils;
import org.intellij.sdk.codesync.utils.CodeSyncDateUtils;
import org.intellij.sdk.codesync.utils.FileUtils;
import org.intellij.sdk.codesync.alerts.PricingAlerts;
import org.json.simple.JSONObject;

import java.io.File;
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
        }
    }

    /*
    Get the user associated with the access token and validate if token is valid or not.

    @return  Pair<Boolean, User>  First item of the pair shows of token is valid and secod is the user instance.
     */
    public Pair<Boolean, User> getUser(String accessToken) throws RequestError {
        JSONObject response;
        try {
            JSONResponse jsonResponse = ClientUtils.sendGet(API_USERS, accessToken);
            jsonResponse.raiseForStatus();
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

        try { JSONObject userPlanObject = (JSONObject) response.get("plan");
            UserPlan userPlan = new UserPlan(
                (Long) userPlanObject.get("SIZE"),
                (Long) userPlanObject.get("FILE_COUNT"),
                (Long) userPlanObject.get("REPO_COUNT")
            );
            User user = new User(
                (String) response.get("email"),
                (Long) response.get("repo_count"),
                userPlan
            );

            return new Pair<>(true, user);
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

    public Integer uploadFile(String accessToken, ConfigRepo configRepo, DiffFile diffFile, File originalsFile) throws FileInfoError, InvalidJsonError, RequestError {
        JSONObject payload = new JSONObject();
        Map<String, Object> fileInfo;
        try {
            fileInfo = FileUtils.getFileInfo(originalsFile.getAbsolutePath());
        } catch (FileInfoError error) {
            throw new FileInfoError(String.format("File Info could not be found for %s. Error: %s", diffFile.fileRelativePath, error.getMessage()));
        }

        payload.put("repo_id", configRepo.id);
        payload.put("branch", diffFile.branch);
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
        }

        try {
            jsonResponse.raiseForStatus();
        } catch (StatusCodeError statusCodeError) {
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
                this.uploadToS3(originalsFile, preSignedURLData);
            }
        } catch (ClassCastException error) {
            System.out.println("Could not upload the file.");
            // this would probably mean that `url` is empty, and we can skip aws upload.
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
    }

    public JSONObject uploadRepo(String accessToken, JSONObject payload) {
        JSONResponse jsonResponse;
        try {
            jsonResponse = ClientUtils.sendPost(API_INIT, payload, accessToken);
        } catch (RequestError | InvalidJsonError error) {
            CodeSyncLogger.critical(String.format("Error while repo init, %s", error.getMessage()));
            return null;
        }

        try {
            jsonResponse.raiseForStatus();
            return jsonResponse.getJsonResponse();
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
    }

    public JSONObject updateRepo(String accessToken, int repoId, JSONObject payload) {
        String url = String.format("%s/%s", CODESYNC_REPO_URL, repoId);
        try {
            JSONResponse jsonResponse = ClientUtils.sendPatch(url, payload, accessToken);
            jsonResponse.raiseForStatus();
            return jsonResponse.getJsonResponse();
        } catch (RequestError | InvalidJsonError | StatusCodeError error) {
            CodeSyncLogger.critical(String.format("Error while repo update, %s", error.getMessage()));

            JSONObject errorResponse = new JSONObject();
            errorResponse.put("error", error.getMessage());
            return errorResponse;
        }
    }

    public JSONObject getRepoPlanInfo(String accessToken, int repoId) {
        String url = String.format("%s/%s/upgrade_plan", CODESYNC_REPO_URL, repoId);

        try {
            JSONResponse jsonResponse = ClientUtils.sendGet(url, accessToken);
            jsonResponse.raiseForStatus();
            return jsonResponse.getJsonResponse();
        } catch (RequestError | InvalidJsonError | StatusCodeError error) {
            CodeSyncLogger.error(String.format("Error while getting plan upgrade information. %s", error.getMessage()));
            return null;
        }
    }

    public JSONObject getTeamActivity(String accessToken) {
        try {
            JSONResponse jsonResponse = ClientUtils.sendGet(TEAM_ACTIVITY_ENDPOINT, accessToken);
            jsonResponse.raiseForStatus();
            return jsonResponse.getJsonResponse();
        } catch (RequestError | InvalidJsonError | StatusCodeError error) {
            CodeSyncLogger.error(String.format("Error while getting team activity data. %s", error.getMessage()));
            return null;
        }
    }
}
