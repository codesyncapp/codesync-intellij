package org.intellij.sdk.codesync.commands;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.project.Project;
import org.intellij.sdk.codesync.CodeSyncLogger;
import org.intellij.sdk.codesync.auth.CodeSyncAuthServer;
import org.intellij.sdk.codesync.codeSyncSetup.CodeSyncSetup;
import org.intellij.sdk.codesync.database.models.User;
import org.intellij.sdk.codesync.exceptions.InvalidAccessTokenError;
import org.intellij.sdk.codesync.utils.CommonUtils;

/*
This command first checks the existence of access token in user.yml file and if it is present
resumes the execution of code sync setup flow.

This command is useful in cases where code sync setup needs to be resumed after successful authentication.
*/
public class ResumeCodeSyncCommand implements Command {
    private final String repoPath, repoName;
    private boolean skipIfAuthError = false;
    private final boolean skipSyncPrompt, isSyncingBranch;
    private final Project project;

    public ResumeCodeSyncCommand(Project project, String repoPath, String repoName, boolean skipSyncPrompt, boolean isSyncingBranch) {
        this.project = project;
        this.repoPath = repoPath;
        this.repoName = repoName;
        this.skipSyncPrompt = skipSyncPrompt;
        this.isSyncingBranch = isSyncingBranch;
    }

    public ResumeCodeSyncCommand(Project project, String repoPath, String repoName, boolean skipSyncPrompt, boolean isSyncingBranch, boolean skipIfAuthError) {
        this.project = project;
        this.repoPath = repoPath;
        this.repoName = repoName;
        this.skipSyncPrompt = skipSyncPrompt;
        this.isSyncingBranch = isSyncingBranch;
        this.skipIfAuthError = skipIfAuthError;
    }

    public void execute() {
        String accessToken = User.getTable().getAccessToken();

        try {
            if (accessToken != null && CodeSyncSetup.validateAccessToken(accessToken)) {
                CodeSyncLogger.debug("[INTELLIJ_AUTH]: Repo sync resumed after login.");

                CodeSyncSetup.setupCodeSyncRepoAsync(project, this.repoPath, this.repoName, this.skipSyncPrompt, this.isSyncingBranch);
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
                CodeSyncLogger.debug(
                    "[INTELLIJ_AUTH]: User about to be redirected again to the login page because of invalid access token."
                );

                CodeSyncAuthServer codeSyncAuthServer = CodeSyncAuthServer.getInstance();
                BrowserUtil.browse(codeSyncAuthServer.getLoginURL());
                CodeSyncAuthServer.registerPostAuthCommand(new ResumeCodeSyncCommand(
                    project, this.repoPath, this.repoName, this.skipSyncPrompt, this.isSyncingBranch, true
                ));
                CodeSyncAuthServer.registerPostAuthCommand(new ReloadStateCommand(project));
                CodeSyncAuthServer.registerPostAuthCommand(new CloseAuthServerCommand(codeSyncAuthServer));

                CodeSyncLogger.debug(
                    "[INTELLIJ_AUTH]: User redirected again to the login page because of invalid access token."
                );
            } catch (Exception e) {
                CodeSyncLogger.critical(String.format(
                    "[RESUME_CODESYNC_COMMAND] could not instantiate CodeSync auth server. Error: %s",
                    CommonUtils.getStackTrace(e)
                ));
            }
        }
    }
}
