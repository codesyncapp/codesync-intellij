package org.intellij.sdk.codesync.exceptions.base;

import java.io.IOException;


public class BaseIOException extends IOException {
    public BaseIOException(String message) {
        super(message);
    }
}
