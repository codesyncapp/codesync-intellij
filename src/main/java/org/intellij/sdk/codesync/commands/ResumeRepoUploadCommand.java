package org.intellij.sdk.codesync.commands;

import com.intellij.openapi.project.Project;
import org.intellij.sdk.codesync.codeSyncSetup.CodeSyncSetup;


public class ResumeRepoUploadCommand implements Command {
    private final Project project;
    private final String branchName, repoPath, repoName;
    private final boolean ignoreSyncIgnoreUpdate;

    public ResumeRepoUploadCommand(Project project, String repoPath, String repoName, String branchName, boolean ignoreSyncIgnoreUpdate) {
        this.project = project;
        this.branchName = branchName;
        this.ignoreSyncIgnoreUpdate = ignoreSyncIgnoreUpdate;
        this.repoPath = repoPath;
        this.repoName = repoName;
    }

    public ResumeRepoUploadCommand(Project project, String repoPath, String repoName, String branchName) {
        this.project = project;
        this.branchName = branchName;
        this.ignoreSyncIgnoreUpdate = true;
        this.repoPath = repoPath;
        this.repoName = repoName;
    }

    public void execute() {
        CodeSyncSetup.resumeRepoUploadAsync(this.project, this.repoPath, this.repoName, this.branchName, this.ignoreSyncIgnoreUpdate);
    }

}
