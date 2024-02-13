package org.intellij.sdk.codesync.exceptions.response;
import org.intellij.sdk.codesync.exceptions.base.BaseException;

/*
Exception class for raising status code exceptions.
*/
public class StatusCodeError extends BaseException {
    private final int statusCode;
    private final int customErrorCode;

    public StatusCodeError(int statusCode, String message) {
        this(statusCode, 0, message);
    }

    public StatusCodeError(int statusCode, int customErrorCode, String message) {
        super(message);
        this.statusCode = statusCode;
        this.customErrorCode = customErrorCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public int getCustomErrorCode() {
        return customErrorCode;
    }
}
