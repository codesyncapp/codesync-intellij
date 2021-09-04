package org.intellij.sdk.codesync.commands;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.project.Project;
import org.intellij.sdk.codesync.CodeSyncLogger;
import org.intellij.sdk.codesync.auth.CodeSyncAuthServer;
import org.intellij.sdk.codesync.codeSyncSetup.CodeSyncSetup;
import org.intellij.sdk.codesync.exceptions.InvalidAccessTokenError;
import org.intellij.sdk.codesync.files.UserFile;

/*
This command first checks the existence of access token in user.yml file and if it is present
resumes the execution of code sync setup flow.

This command is useful in cases where code sync setup needs to be resumed after successful authentication.
*/
public class ResumeCodeSyncCommand implements Command {
    private final String branchName;
    private boolean skipIfAuthError = false;
    private final Project project;

    public ResumeCodeSyncCommand(Project project, String branchName) {
        this.project = project;
        this.branchName = branchName;
    }

    public ResumeCodeSyncCommand(Project project, String branchName, boolean skipIfAuthError) {
        this.project = project;
        this.branchName = branchName;
        this.skipIfAuthError = skipIfAuthError;
    }

    public void execute() {
        String accessToken = UserFile.getAccessToken();

        try {
            if (accessToken != null && CodeSyncSetup.validateAccessToken(accessToken)) {
                CodeSyncSetup.syncRepoAsync(project, branchName);
            }
        } catch (InvalidAccessTokenError error) {
            if (this.skipIfAuthError) {
                // if skipIfAuthError is set to true then do not try to ask for user authentication again.
                // This is useful to avoid loops.
                return;
            }

            // For some reason user authentication did not result in a valid token,
            // we should retry with authentication.
            try {
                CodeSyncAuthServer codeSyncAuthServer = CodeSyncAuthServer.getInstance();
                BrowserUtil.browse(codeSyncAuthServer.getAuthorizationUrl());
                CodeSyncAuthServer.registerPostAuthCommand(new ResumeCodeSyncCommand(
                        project, this.branchName, true
                ));
            } catch (Exception e) {
                CodeSyncLogger.logEvent(String.format(
                        "[RESUME_CODESYNC_COMMAND] could not instantiate codesync auth server. Error: %s",
                        e.getMessage()
                ));
            }
        }
    }
}
