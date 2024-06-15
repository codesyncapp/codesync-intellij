package org.intellij.sdk.codesync.auth;

import com.auth0.jwt.interfaces.Claim;

import com.intellij.ide.BrowserUtil;
import org.apache.http.client.utils.URIBuilder;
import org.intellij.sdk.codesync.CodeSyncLogger;
import org.intellij.sdk.codesync.NotificationManager;
import org.intellij.sdk.codesync.commands.ClearReposToIgnoreCache;
import org.intellij.sdk.codesync.database.models.User;
import org.intellij.sdk.codesync.exceptions.InvalidJsonError;
import org.intellij.sdk.codesync.exceptions.RequestError;
import org.intellij.sdk.codesync.exceptions.response.StatusCodeError;
import org.intellij.sdk.codesync.state.StateUtils;
import org.intellij.sdk.codesync.utils.CommonUtils;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.Map;

import com.auth0.jwt.*;
import com.auth0.jwt.exceptions.JWTDecodeException;

import org.intellij.sdk.codesync.clients.ClientUtils;
import static org.intellij.sdk.codesync.Constants.*;


public class Authenticator extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = request.getRequestURI();
        if (path.equals("/login-callback")) {
            loginHandler(request, response);
        } else if (path.equals("/logout-callback")) {
            logoutHandler(request, response);
        }
        // Execute all registered post authentication commands.
        CodeSyncAuthServer.executePostAuthCommands();
    }

    private void loginHandler(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String accessToken = request.getParameter("access_token");
        String idToken = request.getParameter("id_token");
        createUser(accessToken, idToken);

        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_OK);
        try {
            BrowserUtil.browse(getWebAppAuthCallback("login"));
        } catch (URISyntaxException e) {
            CodeSyncLogger.critical(
                String.format(
                    "[INTELLIJ_AUTH]: Invalid `WEB_APP_URL` settings. Error: %s",
                    CommonUtils.getStackTrace(e)
                )
            );
            response.getWriter().print("<body><h1 class=\"\" style=\"text-align: center;\" >You are logged in, you can close this window now.</h1></body>\n");
        }
        NotificationManager.getInstance().notifyInformation("You have been logged in successfully.");
    }

    private String getWebAppAuthCallback(String type) throws URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder(WEB_APP_URL);
        uriBuilder.addParameter("utm_medium", "plugin");
        uriBuilder.addParameter("utm_source", DIFF_SOURCE);
        uriBuilder.addParameter("v", PLUGIN_VERSION);
        uriBuilder.addParameter("type", type);
        return uriBuilder.toString();
    }

    private void logoutHandler(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String accessToken = request.getParameter("access_token");
        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_OK);

        JSONObject jsonResponse;
        try {
            jsonResponse = ClientUtils.sendPost(API_USERS, accessToken).getJsonResponse();
        } catch (RequestError | InvalidJsonError | StatusCodeError error) {
            CodeSyncLogger.critical(
                String.format(
                    "[INTELLIJ_AUTH_ERROR]: Error while logging out the user. access token: '%s'",
                    accessToken
                )
            );
            response.getWriter().print(
                "<body><h1 class=\"\" style=\"text-align: center;\">Token verification failed.</h1></body>\n"
            );
            return;
        }

        if (jsonResponse.containsKey("error")) {
            CodeSyncLogger.critical(
                String.format(
                    "[INTELLIJ_AUTH_ERROR]: Error while logging out the user. server error: '%s'",
                    jsonResponse.get("error")
                )
            );

            response.getWriter().print(
                "<body><h1 class=\"\" style=\"text-align: center;\">Token verification failed.</h1></body>\n"
            );

            return;
        }

        JSONObject user = (JSONObject) jsonResponse.get("user");
        String userEmail = user == null ? null : user.get("email").toString();
        String activeUserEmail = StateUtils.getGlobalState().userEmail;

        if (userEmail ==  null || !userEmail.contentEquals(activeUserEmail)) {
            // User is not the active user, so we don't need to logout.
            response.getWriter().print(
                "<body><h1 class=\"\" style=\"text-align: center;\">Your plugin and web user does not match.</h1></body>\n"
            );
        }

        try {
            User.getTable().markAllInActive();
        } catch (SQLException error){
            NotificationManager.getInstance().notifyError(
                "An error occurred trying to logout the user, please tyr again later."
            );
            CodeSyncLogger.error(
                String.format(
                    "[INTELLIJ_AUTH_ERROR]: Could not write to database due to database error. Error: %s",
                    CommonUtils.getStackTrace(error)
                )
            );
            return;
        }

        try {
            BrowserUtil.browse(getWebAppAuthCallback("logout"));
        } catch (URISyntaxException e) {
            CodeSyncLogger.critical(
                String.format(
                    "[INTELLIJ_AUTH]: Invalid `WEB_APP_URL` settings. Error: %s",
                    CommonUtils.getStackTrace(e)
                )
            );
            response.getWriter().print("<body><h1 class=\"\" style=\"text-align: center;\" >You are logged out, you can close this window now.</h1></body>\n");
        }

        NotificationManager.getInstance().notifyInformation("You have been logged out successfully.");
    }

    /*
    Create user from the access token and id-token and return a boolean showing the status.
    return `true` if everything was a success, `false` otherwise.
     */
    public boolean createUser(String accessToken, String idToken){
        Map<String, Claim> claims;
        CodeSyncLogger.debug("[INTELLIJ_AUTH]: User successfully authenticated, now saving auth token.");

        try {
            claims = JWT.decode(idToken).getClaims();
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
            jsonResponse = ClientUtils.sendPost(API_USERS, accessToken).getJsonResponse();
        } catch (RequestError | InvalidJsonError | StatusCodeError error) {
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

        String userEmail = claims.get("email").asString();

        // Clear any cache that depends on user authentication status.
        new ClearReposToIgnoreCache().execute();
        try {
            User user = User.getTable().find(userEmail);
            if (user != null) {
                user.setAccessToken(accessToken);
            } else {
                user = new User(userEmail, accessToken, null, null, true);
            }
            user.save();

            // We need this to mark all other users as inactive.
            user.makeActive();
        } catch (SQLException e) {
            CodeSyncLogger.error(
                String.format(
                    "[INTELLIJ_AUTH]: Could not write due to SQLite Error. Error: %s",
                    CommonUtils.getStackTrace(e)
                )
            );
            return false;
        }
        NotificationManager.getInstance().notifyInformation("You have been logged in successfully.");
        CodeSyncLogger.debug("[INTELLIJ_AUTH]: User completed login flow.");
        return true;
    }
}
