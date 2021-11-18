package org.intellij.sdk.codesync.exceptions;


import org.intellij.sdk.codesync.exceptions.base.BaseException;

public class FileInfoError extends BaseException {
    public FileInfoError(String message) {
        super(message);
    }
}
