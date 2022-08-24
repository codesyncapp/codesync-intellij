package org.intellij.sdk.codesync.auth;

import com.auth0.jwt.interfaces.Claim;

import org.intellij.sdk.codesync.CodeSyncLogger;
import org.intellij.sdk.codesync.commands.ClearReposToIgnoreCache;
import org.intellij.sdk.codesync.exceptions.FileLockedError;
import org.intellij.sdk.codesync.exceptions.InvalidJsonError;
import org.intellij.sdk.codesync.exceptions.InvalidYmlFileError;
import org.intellij.sdk.codesync.exceptions.RequestError;
import org.intellij.sdk.codesync.files.UserFile;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import com.auth0.jwt.*;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;

import org.intellij.sdk.codesync.clients.ClientUtils;
import static org.intellij.sdk.codesync.Constants.*;


public class Authenticator extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String accessToken = request.getParameter("access_token");
        String idToken = request.getParameter("id_token");
        createUser(accessToken, idToken);

        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_OK);

        // Execute all registered post authentication commands.
        CodeSyncAuthServer.executePostAuthCommands();

        response.getWriter().print("<body><h1 class=\"\" style=\"text-align: center;\" >You are logged in, you can close this window now.</h1></body>\n");
    }

    public void createUser(String accessToken, String idToken){
        Map<String, Claim> claims;

        try {
            DecodedJWT jwt = JWT.decode(idToken);
            claims = jwt.getClaims();
        } catch (JWTDecodeException exception){
            CodeSyncLogger.logEvent(
                    String.format("[INTELLIJ_AUTH_ERROR]: Error while decoding jwt. Token: '%s'", idToken)
            );
            return;
        }
        JSONObject payload = new JSONObject();
        payload.putAll(claims);
        JSONObject jsonResponse;

        try {
            jsonResponse = ClientUtils.sendPost(API_USERS, payload, accessToken);
        } catch (RequestError | InvalidJsonError error) {
            CodeSyncLogger.logEvent(
                    String.format("[INTELLIJ_AUTH_ERROR]: Error while creating the user. access token: '%s'", accessToken)
            );
            return;
        }

        if (jsonResponse.containsKey("error")) {
            CodeSyncLogger.logEvent(
                    String.format("[INTELLIJ_AUTH_ERROR]: Error while creating the user. server error: '%s'", jsonResponse.get("error"))
            );
            return;
        }
        UserFile userFile;
        try {
            userFile = new UserFile(USER_FILE_PATH);
        } catch (FileNotFoundException e) {
            if(UserFile.createFile(USER_FILE_PATH)){
                try {
                    userFile = new UserFile(USER_FILE_PATH);
                } catch (FileNotFoundException | InvalidYmlFileError error) {
                    CodeSyncLogger.logEvent(
                            String.format("[INTELLIJ_AUTH_ERROR]: Error opening auth file. Error: %s", error.getMessage())
                    );
                    // Could not open user file.
                    return;
                }
            } else {
                // Could not create user file.
                return;
            }
            CodeSyncLogger.logEvent(
                    String.format("[INTELLIJ_AUTH_ERROR]: auth file not found. Error: %s", e.getMessage())
            );
        } catch (InvalidYmlFileError error) {
            error.printStackTrace();
            CodeSyncLogger.logEvent(
                    String.format("[INTELLIJ_AUTH_ERROR]: Invalid auth file. Error: %s", error.getMessage())
            );
            // Could not read user file.
            return;
        }
        String userEmail = claims.get("email").asString();
        userFile.setActiveUser(userEmail, accessToken);

        // Clear any cache that depends on user authentication status.
        new ClearReposToIgnoreCache().execute();
        try {
            userFile.writeYml();
        } catch (FileNotFoundException | InvalidYmlFileError | FileLockedError e) {
            CodeSyncLogger.logEvent(
                    String.format("[INTELLIJ_AUTH_ERROR]: Could write to auth file. Error: %s", e.getMessage())
            );
        }
    }
}
