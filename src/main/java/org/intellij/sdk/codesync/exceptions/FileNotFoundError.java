package org.intellij.sdk.codesync.exceptions;

import org.intellij.sdk.codesync.exceptions.base.BaseIOException;


/*
This exception is raised in places where a file is not found.
*/
public class FileNotFoundError extends BaseIOException {
    public FileNotFoundError(String message) {
        super(message);
    }
}
