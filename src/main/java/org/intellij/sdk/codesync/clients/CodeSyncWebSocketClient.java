package org.intellij.sdk.codesync.clients;

import org.intellij.sdk.codesync.files.DiffFile;

import java.net.URI;
import java.net.URISyntaxException;


public class CodeSyncWebSocketClient {
    URI uri;
    WebSocketClientEndpoint webSocketClientEndpoint;

    public CodeSyncWebSocketClient(String uri) {
        try {
            this.uri = new URI(uri);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public CodeSyncWebSocketClient(URI uri) {
        this.uri = uri;
    }

    public void connect () {
        this.webSocketClientEndpoint = new WebSocketClientEndpoint(this.uri);
    }

    public Boolean authenticate(String token) {
        this.webSocketClientEndpoint.setMessageHandler(new WebSocketClientEndpoint.MessageHandler() {
            public void handleMessage(String message) {
                System.out.println(message);
            }
        });

        this.webSocketClientEndpoint.sendMessage(token);
        return token != null;
    }

    public Boolean sendDiff(DiffFile diffFile) {
        return true;
    }
}
