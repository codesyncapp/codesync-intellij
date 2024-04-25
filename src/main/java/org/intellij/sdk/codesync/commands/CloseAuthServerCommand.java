package org.intellij.sdk.codesync.commands;


import org.intellij.sdk.codesync.auth.CodeSyncAuthServer;

public class CloseAuthServerCommand implements Command {
    CodeSyncAuthServer codeSyncAuthServer;

    public CloseAuthServerCommand(CodeSyncAuthServer codeSyncAuthServer) {
        this.codeSyncAuthServer = codeSyncAuthServer;
    }

    public void execute() {
        // Execute close function.
        codeSyncAuthServer.close();
    }
}
