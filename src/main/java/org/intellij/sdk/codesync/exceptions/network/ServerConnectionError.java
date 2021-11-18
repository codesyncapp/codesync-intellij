package org.intellij.sdk.codesync.exceptions.network;

import org.intellij.sdk.codesync.exceptions.base.BaseNetworkException;

public class ServerConnectionError extends BaseNetworkException {
    public ServerConnectionError(String message) {
        super(message);
    }
}
