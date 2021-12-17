package org.intellij.sdk.codesync.commands;

import org.intellij.sdk.codesync.HandleBuffer;


public class ClearReposToIgnoreCache implements Command {
    public ClearReposToIgnoreCache() { }

    public void execute() {
        HandleBuffer.clearReposToIgnore();
    }

}
