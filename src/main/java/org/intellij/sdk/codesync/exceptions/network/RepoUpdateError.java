package org.intellij.sdk.codesync.exceptions.network;

import org.intellij.sdk.codesync.exceptions.base.BaseNetworkException;

public class RepoUpdateError extends BaseNetworkException {
    public RepoUpdateError(String message) {
        super(message);
    }
}
