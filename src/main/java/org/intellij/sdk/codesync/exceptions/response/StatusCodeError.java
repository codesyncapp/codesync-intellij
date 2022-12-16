package org.intellij.sdk.codesync.exceptions.response;
import org.intellij.sdk.codesync.exceptions.base.BaseException;

/*
Exception class for raising status code exceptions.
*/
public class StatusCodeError extends BaseException {
    private final int statusCode;
    public StatusCodeError(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
