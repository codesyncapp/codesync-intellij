package org.intellij.sdk.codesync.exceptions.database;

import org.intellij.sdk.codesync.exceptions.base.NotFound;

public class RepoFileNotFound extends NotFound {
    public RepoFileNotFound(String message) {
        super(message);
    }
}
