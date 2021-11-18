package org.intellij.sdk.codesync.exceptions;

import org.intellij.sdk.codesync.exceptions.base.BaseException;

/*
 * This exception is raised if yml file is not valid..
 * */
public class InvalidYmlFileError extends BaseException {
    public InvalidYmlFileError(String message) {
        super(message);
    }
}
