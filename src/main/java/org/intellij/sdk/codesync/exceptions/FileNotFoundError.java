package org.intellij.sdk.codesync.exceptions;

import java.io.IOException;


/*
This exception is raised in places where a file is not found.
*/
public class FileNotFoundError extends IOException {
    public FileNotFoundError(String message) {
        super(message);
    }
}
