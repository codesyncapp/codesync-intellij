package org.intellij.sdk.codesync.auth;

import com.auth0.jwt.interfaces.Claim;

import org.intellij.sdk.codesync.CodeSyncLogger;
import org.intellij.sdk.codesync.commands.ClearReposToIgnoreCache;
import org.intellij.sdk.codesync.exceptions.InvalidJsonError;
import org.intellij.sdk.codesync.exceptions.RequestError;
import org.intellij.sdk.codesync.models.UserAccount;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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

    /*
    Create user from the access token and id-token and return a boolean showing the status.
    return `true` if everything was a success, `false` otherwise.
     */
    public boolean createUser(String accessToken, String idToken){
        Map<String, Claim> claims;
        CodeSyncLogger.debug("[INTELLIJ_AUTH]: User successfully authenticated, now saving auth token.");

        try {
            DecodedJWT jwt = JWT.decode(idToken);
            claims = jwt.getClaims();
        } catch (JWTDecodeException exception){
            CodeSyncLogger.critical(
                    String.format("[INTELLIJ_AUTH_ERROR]: Error while decoding jwt. Token: '%s'", idToken)
            );
            return false;
        }
        JSONObject payload = new JSONObject();
        payload.putAll(claims);
        JSONObject jsonResponse;

        try {
            jsonResponse = ClientUtils.sendPost(API_USERS, payload, accessToken).getJsonResponse();
        } catch (RequestError | InvalidJsonError error) {
            CodeSyncLogger.critical(
                    String.format("[INTELLIJ_AUTH_ERROR]: Error while creating the user. access token: '%s'", accessToken)
            );
            return false;
        }

        if (jsonResponse.containsKey("error")) {
            CodeSyncLogger.critical(
                    String.format("[INTELLIJ_AUTH_ERROR]: Error while creating the user. server error: '%s'", jsonResponse.get("error"))
            );
            return false;
        }
        UserAccount userAccount;
        userAccount = new UserAccount();
        String userEmail = claims.get("email").asString();

        // Clear any cache that depends on user authentication status.
        new ClearReposToIgnoreCache().execute();
        userAccount.setActiveUser(userEmail, accessToken);

        CodeSyncLogger.debug("[INTELLIJ_AUTH]: User completed login flow.");
        return true;
    }
}
