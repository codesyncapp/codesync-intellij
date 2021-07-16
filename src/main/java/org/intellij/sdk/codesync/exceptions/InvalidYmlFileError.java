package org.intellij.sdk.codesync.exceptions;

/*
 * This exception is raised if yml file is not valid..
 * */
public class InvalidYmlFileError extends Exception {
    public InvalidYmlFileError(String message) {
        super(message);
    }
}
