package org.intellij.sdk.codesync.commands;

import com.intellij.openapi.project.Project;
import org.intellij.sdk.codesync.codeSyncSetup.CodeSyncSetup;


public class ResumeRepoUploadCommand implements Command {
    private final Project project;
    private final String branchName;
    private final boolean ignoreSyncIgnoreUpdate;

    public ResumeRepoUploadCommand(Project project, String branchName, boolean ignoreSyncIgnoreUpdate) {
        this.project = project;
        this.branchName = branchName;
        this.ignoreSyncIgnoreUpdate = ignoreSyncIgnoreUpdate;
    }

    public ResumeRepoUploadCommand(Project project, String branchName) {
        this.project = project;
        this.branchName = branchName;
        this.ignoreSyncIgnoreUpdate = true;
    }

    public void execute() {
        CodeSyncSetup.resumeRepoUploadAsync(this.project, this.branchName, this.ignoreSyncIgnoreUpdate);
    }

}
