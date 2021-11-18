package org.intellij.sdk.codesync.exceptions.repo;

import org.intellij.sdk.codesync.exceptions.base.BaseException;

public class RepoNotActive extends BaseException {
    public RepoNotActive(String message) {
        super(message);
    }
}
