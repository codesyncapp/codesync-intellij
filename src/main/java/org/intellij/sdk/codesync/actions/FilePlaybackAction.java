package org.intellij.sdk.codesync.actions;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.intellij.sdk.codesync.CodeSyncLogger;
import org.intellij.sdk.codesync.NotificationManager;
import org.intellij.sdk.codesync.database.models.Repo;
import org.intellij.sdk.codesync.database.models.RepoFile;
import org.intellij.sdk.codesync.exceptions.common.FileNotInModuleError;
import org.intellij.sdk.codesync.exceptions.database.RepoFileNotFound;
import org.intellij.sdk.codesync.state.PluginState;
import org.intellij.sdk.codesync.state.RepoStatus;
import org.intellij.sdk.codesync.state.StateUtils;
import org.intellij.sdk.codesync.utils.CommonUtils;
import org.intellij.sdk.codesync.utils.FileUtils;
import org.intellij.sdk.codesync.utils.GitUtils;
import org.intellij.sdk.codesync.utils.ProjectUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.sql.SQLException;
import java.util.regex.Pattern;

import static org.intellij.sdk.codesync.Constants.*;

public class FilePlaybackAction extends BaseModuleAction {
    @Override
    public void update(AnActionEvent e) {
        Project project = e.getProject();
        if (project ==  null) {
            e.getPresentation().setEnabled(false);
            return;
        }

        if (this.isAccountDeactivated()) {
            e.getPresentation().setEnabled(false);
            return;
        }

        RepoStatus repoStatus = RepoStatus.UNKNOWN;

        // Only enable file playback button when some file is opened in the editor.
        try {
            VirtualFile virtualFile = e.getRequiredData(CommonDataKeys.PSI_FILE).getVirtualFile();
            repoStatus = this.getRepoStatus(virtualFile, project);
        } catch (AssertionError | FileNotInModuleError error) {
            // Ignore, this means that the file is not in any module.
        }

        this.setButtonRepresentation(e, repoStatus);
    }

    private void setButtonRepresentation(AnActionEvent e, RepoStatus repoStatus) {
        switch (repoStatus) {
            case SYNCED_VIA_PARENT:
            case IN_SYNC:
                e.getPresentation().setEnabled(true);
                break;
            case SYNC_IN_PROGRESS:
            case DISCONNECTED:
            case NOT_SYNCED:
            case UNKNOWN:
                e.getPresentation().setEnabled(false);
                break;
        }
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();

        if( project == null ) {
            NotificationManager.getInstance().notifyError("An error occurred trying to perform file playback action.");
            CodeSyncLogger.warning("An error occurred trying to perform file playback action. e.getProject() is null.");

            return;
        }

        String repoPath;
        VirtualFile virtualFile = e.getRequiredData(CommonDataKeys.PSI_FILE).getVirtualFile();
        String openedFilePath = virtualFile.getPath();

        try {
            repoPath = ProjectUtils.getRepoPath(virtualFile, project);
        } catch (FileNotInModuleError error) {
            NotificationManager.getInstance().notifyError("An error occurred trying to perform file playback action.", project);
            CodeSyncLogger.warning(String.format(
                    "An error occurred trying to perform file playback action. file '%s' is not present in the project.",
                    virtualFile.getPath()
            ));

            return;
        }

        openedFilePath = FileUtils.normalizeFilePath(openedFilePath);
        String relativeFilePath = openedFilePath.replace(repoPath, "")
                .replaceFirst(Pattern.quote(String.valueOf(File.separatorChar)), "");

        String branchName = GitUtils.getBranchName(repoPath);
        RepoFile repoFile;

        PluginState repoState = StateUtils.getState(repoPath);
        // If repo is synced via parent repo then update repoPath to parent repo path.
        if (repoState != null && repoState.repoStatus == RepoStatus.SYNCED_VIA_PARENT) {
            try {
                Repo repo = Repo.getTable().getParentRepo(repoPath);
                repoPath = repo.getPath();
            } catch (SQLException ex) {
                NotificationManager.getInstance().notifyError(
                    "An error occurred trying to perform repo playback action. Could not get parent repo record from the database.",
                    project
                );
                CodeSyncLogger.error(String.format(
                    "An error occurred trying to perform repo playback action. Error while fetching parent repo record. Error: %s",
                    CommonUtils.getStackTrace(ex)
                ));

                return;
            }
        }

        try {
            repoFile = RepoFile.getTable().get(repoPath, branchName, relativeFilePath);
        } catch (SQLException ex) {
            NotificationManager.getInstance().notifyError(
                "An error occurred trying to perform file playback action. Could not get file record from the database.",
                project
            );
            CodeSyncLogger.critical(String.format(
                "An error occurred trying to perform file playback action. Error while fetching file record. Error: %s",
                CommonUtils.getStackTrace(ex)
            ));

            return;
        } catch (RepoFileNotFound ex) {
            NotificationManager.getInstance().notifyError(
                "An error occurred trying to perform file playback action. This file is not yet synced with CodeSync servers.",
                project
            );
            CodeSyncLogger.warning(String.format(
                "An error occurred trying to perform file playback action. " +
                        "File '%s' not found in the database for repo '%s'.",
                relativeFilePath,
                repoPath
            ));

            return;
        }
        String url = String.format(FILE_PLAYBACK_LINK, repoFile.getServerFileId());
        BrowserUtil.browse(url);
    }
}
