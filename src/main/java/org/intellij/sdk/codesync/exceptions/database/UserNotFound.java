package org.intellij.sdk.codesync.exceptions.database;

import org.intellij.sdk.codesync.exceptions.base.NotFound;

public class UserNotFound extends NotFound {
    public UserNotFound(String message) {
        super(message);
    }
}
