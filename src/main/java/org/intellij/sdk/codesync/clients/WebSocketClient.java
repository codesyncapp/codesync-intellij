package org.intellij.sdk.codesync.clients;

import org.intellij.sdk.codesync.files.DiffFile;


public class WebSocketClient {
    public WebSocketClient() {

    }

    public Boolean authenticate(String token) {
        return token != null;
    }

    public Boolean sendDiff(DiffFile diffFile) {
        return true;
    }
}
