package org.intellij.sdk.codesync.exceptions.file;

import org.intellij.sdk.codesync.exceptions.base.BaseException;

public class UserFileError extends BaseException {
    public UserFileError(String message) {
        super(message);
    }
}
