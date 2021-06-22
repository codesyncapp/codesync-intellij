package org.intellij.sdk.codesync.clients;

import org.intellij.sdk.codesync.exceptions.WebSocketConnectionError;
import org.intellij.sdk.codesync.files.ConfigRepo;
import org.intellij.sdk.codesync.files.DiffFile;


public class CodeSyncClient {
    public CodeSyncClient() {

    }

    public Boolean isServerUp() {
        return true;
    }

    public WebSocketClient connectWebSocket() throws WebSocketConnectionError {
        return new WebSocketClient();
    }

    public Integer uploadFile(ConfigRepo configRepo, DiffFile diffFile)  {
        return 1;
    }
}
