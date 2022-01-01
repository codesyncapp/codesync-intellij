package org.intellij.sdk.codesync.clients;

import java.io.IOException;
import java.net.URI;

import jakarta.websocket.*;


@ClientEndpoint
public class WebSocketClientEndpoint {

    Session userSession = null;
    private MessageHandler messageHandler;
    private URI endpointURI;
    private WebSocketContainer container;

    public WebSocketClientEndpoint(URI endpointURI) {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        this.endpointURI = endpointURI;

        try {
            Thread.currentThread().setContextClassLoader(WebSocketClientEndpoint.class.getClassLoader());
            this.container = ContainerProvider.getWebSocketContainer();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    public Session connectToServer() throws RuntimeException {
        try {
            return this.container.connectToServer(this, this.endpointURI);
        } catch (DeploymentException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @OnOpen
    public void onOpen(Session userSession) {
        System.out.println("opening websocket");
        this.userSession = userSession;
    }

    @OnClose
    public void onClose(Session userSession, CloseReason reason) {
        System.out.println("closing websocket");
        this.userSession = null;
    }

    @OnMessage
    public void onMessage(String message) {
        if (this.messageHandler != null) {
            this.messageHandler.handleMessage(message);
        }
    }

    public void setMessageHandler(MessageHandler msgHandler) {
        this.messageHandler = msgHandler;
    }

    public void sendMessage(String message) {
        this.userSession.getAsyncRemote().sendText(message);
    }

    public static interface MessageHandler {
        public void handleMessage(String message);
    }
}