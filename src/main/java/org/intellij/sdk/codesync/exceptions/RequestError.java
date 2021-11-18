package org.intellij.sdk.codesync.exceptions;

import org.intellij.sdk.codesync.exceptions.base.BaseException;

/*
 * This exception is raised if something goes wrong while making a request to the server.
 * */
public class RequestError extends BaseException {
    public RequestError(String message) {
        super(message);
    }
}
