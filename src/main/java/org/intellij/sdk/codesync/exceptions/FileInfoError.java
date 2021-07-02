package org.intellij.sdk.codesync.exceptions;

/*
* This exception is raised if file metadata is not accessible for some reason.
* */
public class FileInfoError extends Exception {
    public FileInfoError(String message) {
        super(message);
    }
}
