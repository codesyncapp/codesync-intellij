package org.intellij.sdk.codesync.server.servlets;

import org.intellij.sdk.codesync.CodeSyncLogger;
import org.intellij.sdk.codesync.NotificationManager;
import org.intellij.sdk.codesync.clients.ClientUtils;
import org.intellij.sdk.codesync.exceptions.InvalidJsonError;
import org.intellij.sdk.codesync.exceptions.RequestError;
import org.intellij.sdk.codesync.exceptions.SQLiteDBConnectionError;
import org.intellij.sdk.codesync.exceptions.response.StatusCodeError;
import org.intellij.sdk.codesync.database.models.UserAccount;
import org.intellij.sdk.codesync.state.StateUtils;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;

import static org.intellij.sdk.codesync.Constants.API_USERS;

public class ReactivateAccountHandler extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_OK);
        String accessToken = request.getParameter("access_token");
        String errorMessage = "Error while activating account, please restart IDE if problem persists.";

        JSONObject jsonObject;
        try {
            jsonObject = ClientUtils.sendPost(API_USERS, accessToken).getJsonResponse();
        } catch (RequestError | InvalidJsonError | StatusCodeError error) {
            CodeSyncLogger.critical(
                String.format("[INTELLIJ_REACTIVATE_ACCOUNT_ERROR]: Error while fetching the user. access token: '%s'", accessToken)
            );
            NotificationManager.getInstance().notifyError(errorMessage);
            return;
        }

        if (jsonObject.containsKey("error")) {
            CodeSyncLogger.critical(
                String.format("[INTELLIJ_REACTIVATE_ACCOUNT_ERROR]: Error while fetching the user. server error: '%s'", jsonObject.get("error"))
            );
            NotificationManager.getInstance().notifyError(errorMessage);
            return;
        }
        JSONObject userDetails = (JSONObject) jsonObject.get("user");
        if (userDetails == null) {
            CodeSyncLogger.critical(
                "[INTELLIJ_REACTIVATE_ACCOUNT_ERROR]: Error while fetching the user. User not returned by the server"
            );
            NotificationManager.getInstance().notifyError(errorMessage);
            return;
        }
        String email = (String) userDetails.get("email");
        UserAccount userAccount;
        try {
            userAccount = new UserAccount();
        } catch (SQLiteDBConnectionError error) {
            CodeSyncLogger.critical(
                String.format("[INTELLIJ_AUTH_ERROR]: SQLite Database Connection Error. Error: %s", error.getMessage())
            );
            NotificationManager.getInstance().notifyError(errorMessage);
            return;
        }

        String message;
        if (Objects.equals(userAccount.getActiveUser().getUserEmail(), email)) {
            // Mark account as active.
            StateUtils.reactivateAccount();
            message = "Your account has been activated, you can close this window now.";
        } else {
            message = "Invalid access token in the request parameters, please try again.";
        }

        try {
            response.getWriter().print(
                String.format("<body><h1 class=\"\" style=\"text-align: center;\" >%s</h1></body>\n", message)
            );
        } catch (IOException error) {
            CodeSyncLogger.critical(String.format(
                "[REACTIVATE_ACCOUNT]: Error while activating the account. %nError: %s", error.getMessage()
            ));
            NotificationManager.getInstance().notifyError(errorMessage);
        }
    }
}
