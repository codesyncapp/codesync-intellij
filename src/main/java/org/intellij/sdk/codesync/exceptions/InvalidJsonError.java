package org.intellij.sdk.codesync.exceptions;

/*
 * This exception is raised if json data is not valid.
 * */
public class InvalidJsonError extends Exception {
    public InvalidJsonError(String message) {
        super(message);
    }
}
