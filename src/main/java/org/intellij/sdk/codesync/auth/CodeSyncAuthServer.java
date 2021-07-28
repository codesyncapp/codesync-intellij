package org.intellij.sdk.codesync.auth;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import org.apache.http.client.utils.URIBuilder;

import java.net.URISyntaxException;

import static org.intellij.sdk.codesync.Constants.*;


public class CodeSyncAuthServer {
    private static CodeSyncAuthServer singletonInstance;
    private Server server;

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
        int maxThreads = 100;
        int minThreads = 10;
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

    public String getAuthorizationUrl(boolean skipConnectPrompt) {
        try {
            URIBuilder uriBuilder = new URIBuilder(CODESYNC_AUTHORIZE_URL);
            uriBuilder.addParameter("redirect_uri", getServerURL());

            return uriBuilder.toString();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getAuthorizationUrl() {
        return getAuthorizationUrl(false);
    }
}
