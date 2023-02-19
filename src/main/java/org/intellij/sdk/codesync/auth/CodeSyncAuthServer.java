package org.intellij.sdk.codesync.auth;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import org.apache.http.client.utils.URIBuilder;
import org.intellij.sdk.codesync.CodeSyncLogger;
import org.intellij.sdk.codesync.commands.Command;

import java.net.URISyntaxException;
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

        servletHandler.addServletWithMapping(Authenticator.class, "/login-success");

        server.start();
    }

    public String getServerURL(){
        return String.format("%slogin-success", this.server.getURI().toString());
    }

    public String getAuthorizationUrl() {
        try {
            URIBuilder uriBuilder = new URIBuilder(CODESYNC_AUTHORIZE_URL);
            uriBuilder.addParameter("redirect_uri", getServerURL());
            uriBuilder.addParameter("source", DIFF_SOURCE);
            uriBuilder.addParameter("v", PLUGIN_VERSION);

            return uriBuilder.toString();
        } catch (URISyntaxException e) {
            CodeSyncLogger.critical(
                    String.format("[INTELLIJ_AUTH_ERROR]: Invalid `CODESYNC_AUTHORIZE_URL` settings. Error: %s", e.getMessage())
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
