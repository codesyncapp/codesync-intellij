package org.intellij.sdk.codesync.exceptions;

import org.intellij.sdk.codesync.exceptions.base.BaseException;

/*
This exception is raised at places where access token is not valid.
*/
public class InvalidAccessTokenError extends BaseException {
    public InvalidAccessTokenError(String message) {
        super(message);
    }
}
