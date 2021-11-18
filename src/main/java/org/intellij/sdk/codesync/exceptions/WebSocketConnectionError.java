package org.intellij.sdk.codesync.exceptions;

import org.intellij.sdk.codesync.exceptions.base.BaseException;

public class WebSocketConnectionError extends BaseException {
    public WebSocketConnectionError(String message) {
        super(message);
    }
}
