package org.intellij.sdk.codesync.auth;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import org.apache.http.client.utils.URIBuilder;
import org.intellij.sdk.codesync.CodeSyncLogger;
import org.intellij.sdk.codesync.commands.Command;
import org.intellij.sdk.codesync.utils.CommonUtils;

import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.Queue;

import static org.intellij.sdk.codesync.Constants.*;


public class CodeSyncAuthServer {
    private static CodeSyncAuthServer singletonInstance;
    private Server server;
    private static final Queue<Command> commandQueue = new LinkedList<>();

    private CodeSyncAuthServer() throws Exception {
        start();
    }

    public static CodeSyncAuthServer getInstance() throws Exception {
        if (singletonInstance == null) {
            singletonInstance = new CodeSyncAuthServer();
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

        servletHandler.addServletWithMapping(Authenticator.class, "/login-callback");
        servletHandler.addServletWithMapping(Authenticator.class, "/logout-callback");

        server.start();
    }

    public String getServerURL(String path) {
        return Paths.get(this.server.getURI().toString(), path).toString();
    }

    public String getLoginURL() {
        try {
            URIBuilder uriBuilder = new URIBuilder(WEBAPP_AUTHORIZE_URL);
            uriBuilder.addParameter("utm_medium", "plugin");
            uriBuilder.addParameter("utm_source", DIFF_SOURCE);
            uriBuilder.addParameter("v", PLUGIN_VERSION);
            uriBuilder.addParameter("login-callback", getServerURL("login-callback"));

            return uriBuilder.toString();
        } catch (URISyntaxException e) {
            CodeSyncLogger.critical(
                String.format(
                    "[INTELLIJ_AUTH]: Invalid `WEBAPP_AUTHORIZE_URL` settings. Error: %s",
                    CommonUtils.getStackTrace(e)
                )
            );
            return null;
        }
    }

    public String getLogoutURL() {
        try {
            URIBuilder uriBuilder = new URIBuilder(WEBAPP_LOGOUT_URL);
            uriBuilder.addParameter("utm_medium", "plugin");
            uriBuilder.addParameter("utm_source", DIFF_SOURCE);
            uriBuilder.addParameter("v", PLUGIN_VERSION);
            uriBuilder.addParameter("logout-callback", getServerURL("logout-callback"));

            return uriBuilder.toString();
        } catch (URISyntaxException e) {
            CodeSyncLogger.critical(
                String.format(
                    "[INTELLIJ_AUTH]: Invalid `CODESYNC_LOGOUT_URL` settings. Error: %s",
                    CommonUtils.getStackTrace(e)
                )
            );
            return null;
        }
    }

    public static void registerPostAuthCommand(Command command) {
        commandQueue.add(command);
    }

    public static void executePostAuthCommands() {
        for (Command command: commandQueue) {
            command.execute();
        }

        // Clear the queue.
        commandQueue.clear();
    }
}
