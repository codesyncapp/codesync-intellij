package org.intellij.sdk.codesync.server.servlets;

import org.intellij.sdk.codesync.CodeSyncLogger;
import org.intellij.sdk.codesync.state.StateUtils;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ReactivateAccountHandler extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_OK);

        // Mark account as active.
        StateUtils.reactivateAccount();
        try {
            response.getWriter().print("<body><h1 class=\"\" style=\"text-align: center;\" >Your account has been activated, you can close this window now.</h1></body>\n");
        } catch (IOException error) {
            CodeSyncLogger.critical(String.format(
                "[REACTIVATE_ACCOUNT]: Error while activating the account. %nError: %s", error.getMessage()
            ));
        }
    }
}
