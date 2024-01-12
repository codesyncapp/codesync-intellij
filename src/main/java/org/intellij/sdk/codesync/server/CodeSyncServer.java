package org.intellij.sdk.codesync.server;


import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.intellij.sdk.codesync.CodeSyncLogger;

import javax.servlet.Servlet;
import java.util.HashMap;
import java.util.Map;

public class CodeSyncServer {
    private static CodeSyncServer singletonInstance;
    private Server server;
    private final Map<String, Class<? extends Servlet>> pathMap = new HashMap<>();

    private CodeSyncServer() throws Exception {
        start();
    }

    public static CodeSyncServer getInstance() throws Exception {
        if (singletonInstance == null) {
            singletonInstance = new CodeSyncServer();
        }
        return singletonInstance;
    }

    public CodeSyncServer addPathMapping(String pathSpec, Class<? extends Servlet> servlet) {
        this.pathMap.put(pathSpec, servlet);
        return this;
    }

    public void start() throws Exception {
        // Return if server is already running.
        if (server.isRunning()) {
            return;
        }
        int maxThreads = 20;
        int minThreads = 1;
        int idleTimeout = 120;

        QueuedThreadPool threadPool = new QueuedThreadPool(maxThreads, minThreads, idleTimeout);

        server = new Server(threadPool);
        try (ServerConnector connector = new ServerConnector(server)) {
            server.setConnectors(new Connector[] { connector });

            ServletHandler servletHandler = new ServletHandler();
            server.setHandler(servletHandler);

            for (Map.Entry<String, Class<? extends Servlet>> entry : pathMap.entrySet()) {
                servletHandler.addServletWithMapping(entry.getValue(), entry.getKey());
            }
            server.start();
        } catch (Exception error) {
            CodeSyncLogger.critical(String.format(
                "[CODESYNC_SERVER]: Unable to start the server. Error: %s", error.getMessage()
            ));
        }
    }

    public String getServerURL(){
        return this.server.getURI().toString();
    }
}
