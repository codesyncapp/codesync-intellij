package org.intellij.sdk.codesync.server;

import org.apache.http.client.utils.URIBuilder;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.intellij.sdk.codesync.CodeSyncLogger;
import org.intellij.sdk.codesync.commands.Command;
import org.intellij.sdk.codesync.server.servlets.ReactivateAccountHandler;
import org.intellij.sdk.codesync.utils.CommonUtils;

import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.Queue;

import static org.intellij.sdk.codesync.Constants.*;


public class CodeSyncReactivateAccountServer {
    private static CodeSyncReactivateAccountServer singletonInstance;
    private Server server;
    private static final Queue<Command> commandQueue = new LinkedList<>();

    private CodeSyncReactivateAccountServer() throws Exception {
        start();
    }

    public static CodeSyncReactivateAccountServer getInstance() throws Exception {
        if (singletonInstance == null) {
            singletonInstance = new CodeSyncReactivateAccountServer();
        }
        return singletonInstance;
    }

    public void start() throws Exception {
        int maxThreads = 20;
        int minThreads = 1;
        int idleTimeout = 120;

        QueuedThreadPool threadPool = new QueuedThreadPool(maxThreads, minThreads, idleTimeout);

        server = new Server(threadPool);
        ServerConnector connector = new ServerConnector(server);
        server.setConnectors(new Connector[] { connector });

        ServletHandler servletHandler = new ServletHandler();
        server.setHandler(servletHandler);

        servletHandler.addServletWithMapping(ReactivateAccountHandler.class, "/reactivate-callback");

        server.start();
    }

    public String getServerURL() {
        return this.server.getURI().toString();
    }

    public String getReactivateAccountUrl() {
        try {
            String callbackURL = String.format("%sreactivate-callback", this.getServerURL());
            URIBuilder uriBuilder = new URIBuilder(SETTINGS_PAGE_URL);
            uriBuilder.addParameter("callback", callbackURL);

            return uriBuilder.toString();
        } catch (URISyntaxException e) {
            CodeSyncLogger.critical(
                String.format(
                    "[INTELLIJ_ACTIVATE_ACCOUNT_ERROR]: Invalid `SETTINGS_PAGE_URL` settings. Error: %s",
                    CommonUtils.getStackTrace(e)
                )
            );
            return null;
        }
    }
}
