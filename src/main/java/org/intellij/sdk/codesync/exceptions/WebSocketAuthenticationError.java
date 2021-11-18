package org.intellij.sdk.codesync.exceptions;

import org.intellij.sdk.codesync.exceptions.base.BaseException;

public class WebSocketAuthenticationError extends BaseException {
    public WebSocketAuthenticationError(String message) {
        super(message);
    }
}
