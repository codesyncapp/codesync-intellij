package org.intellij.sdk.codesync.exceptions.database;

import org.intellij.sdk.codesync.exceptions.base.NotFound;

public class RepoBranchNotFound extends NotFound {
    public RepoBranchNotFound(String message) {
        super(message);
    }
}
