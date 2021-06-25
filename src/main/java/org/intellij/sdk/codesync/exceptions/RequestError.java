package org.intellij.sdk.codesync.exceptions;

/*
 * This exception is raised if something goes wrong while making a request to the server.
 * */
public class RequestError extends  Exception {
    public RequestError(String message) {
        super(message);
    }
}
