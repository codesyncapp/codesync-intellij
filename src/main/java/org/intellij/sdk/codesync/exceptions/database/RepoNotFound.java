package org.intellij.sdk.codesync.exceptions.database;

import org.intellij.sdk.codesync.exceptions.base.NotFound;

public class RepoNotFound extends NotFound {
    public RepoNotFound(String message) {
        super(message);
    }
}
