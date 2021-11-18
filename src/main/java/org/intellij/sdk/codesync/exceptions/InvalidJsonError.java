package org.intellij.sdk.codesync.exceptions;

import org.intellij.sdk.codesync.exceptions.base.BaseException;

/*
 * This exception is raised if json data is not valid.
 * */
public class InvalidJsonError extends BaseException {
    public InvalidJsonError(String message) {
        super(message);
    }
}
