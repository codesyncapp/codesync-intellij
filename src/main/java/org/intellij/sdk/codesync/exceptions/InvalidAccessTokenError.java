package org.intellij.sdk.codesync.exceptions;

/*
This exception is raised at places where access token is not valid.
*/
public class InvalidAccessTokenError extends Exception {
    public InvalidAccessTokenError(String message) {
        super(message);
    }
}
