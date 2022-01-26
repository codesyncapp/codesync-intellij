package org.intellij.sdk.codesync.exceptions.common;

import org.intellij.sdk.codesync.exceptions.base.BaseProjectException;

public class FileNotInModuleError extends BaseProjectException {
    public FileNotInModuleError (String message) {
        super(message);
    }
}
