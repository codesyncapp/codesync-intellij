package org.intellij.sdk.codesync.exceptions;

import org.intellij.sdk.codesync.exceptions.base.BaseException;

public class InvalidConfigFileError extends BaseException {
    public InvalidConfigFileError(String message) {
        super(message);
    }
}
