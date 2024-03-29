package org.intellij.sdk.codesync.exceptions;

import org.intellij.sdk.codesync.exceptions.base.BaseIOException;

import java.io.IOException;

/*
This exception is raised in palces where a file could not be created for some reason.
*/
public class FileNotCreatedError extends BaseIOException {
    public FileNotCreatedError(String message) {
        super(message);
    }
}
