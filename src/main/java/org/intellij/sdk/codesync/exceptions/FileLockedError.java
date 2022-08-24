package org.intellij.sdk.codesync.exceptions;

import org.intellij.sdk.codesync.exceptions.base.BaseException;


public class FileLockedError extends BaseException {
    public FileLockedError(String message) {
        super(message);
    }
}
