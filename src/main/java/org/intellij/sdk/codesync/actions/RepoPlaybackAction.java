package org.intellij.sdk.codesync.actions;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.intellij.sdk.codesync.CodeSyncLogger;
import org.intellij.sdk.codesync.NotificationManager;
import org.intellij.sdk.codesync.database.models.Repo;
import org.intellij.sdk.codesync.exceptions.common.FileNotInModuleError;
import org.intellij.sdk.codesync.exceptions.database.RepoNotFound;
import org.intellij.sdk.codesync.utils.FileUtils;
import org.intellij.sdk.codesync.utils.ProjectUtils;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

import static org.intellij.sdk.codesync.Constants.*;

public class RepoPlaybackAction extends BaseModuleAction {

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

        // Check if any of the modules inside this project are synced with codesync or not.
        // We will show repo playback button if even a single repo is synced.
        VirtualFile[] contentRoots = ProjectUtils.getAllContentRoots(project);
        if (contentRoots.length > 1) {
            // If more than one module are present in the project then a file must be open to show repo playback
            // this is needed because without the file we can not determine the correct repo to show.
            try {
                VirtualFile virtualFile = e.getRequiredData(CommonDataKeys.PSI_FILE).getVirtualFile();
                e.getPresentation().setEnabled(
                    this.isRepoInSync(virtualFile, project)
                );
            } catch (AssertionError | FileNotInModuleError error) {
                e.getPresentation().setEnabled(false);
            }
        } else if (contentRoots.length == 1) {
            // If there is only one module then we can simply show the repo playback for the only repo present.
            // Disable the button if repo is not in sync.
            e.getPresentation().setEnabled(
                this.isRepoInSync(contentRoots[0])
            );
        } else {
            e.getPresentation().setEnabled(false);
        }
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if(project == null) {
            NotificationManager.getInstance().notifyError("An error occurred trying to perform repo playback action.");
            CodeSyncLogger.warning("An error occurred trying to perform repo playback action. e.getProject() is null.");

            return;
        }
        String repoPath, repoName;

        VirtualFile[] contentRoots = ProjectUtils.getAllContentRoots(project);
        if (contentRoots.length > 1) {
            // If more than one module are present in the project then a file must be open to show repo playback
            // this is needed because without the file we can not determine the correct repo to show.
            try {
                VirtualFile virtualFile = e.getRequiredData(CommonDataKeys.PSI_FILE).getVirtualFile();
                repoPath = ProjectUtils.getRepoPath(virtualFile, project);
                repoName = ProjectUtils.getRepoName(virtualFile, project);
            } catch (AssertionError | FileNotInModuleError error) {
                NotificationManager.getInstance().notifyError(
                    "An error occurred trying to perform repo playback action. " +
                            "We could not detect the module of the opened file.", project
                );
                CodeSyncLogger.warning(String.format(
                    "An error occurred trying to perform repo playback action. " +
                    "We could not detect the module of the opened file. Error: %s",
                    error.getMessage()
                ));

                return;
            }
        } else if (contentRoots.length == 1) {
            // If there is only one module then we can simply show the repo playback for the only repo present.
            repoPath = FileUtils.normalizeFilePath(contentRoots[0]);
            repoName = contentRoots[0].getName();
        } else {
            NotificationManager.getInstance().notifyError(
        "An error occurred trying to perform repo playback action. " +
                "We could not detect the module of the opened file.", project
            );
            CodeSyncLogger.warning(
                "An error occurred trying to perform repo playback action. 0 modules returned." +
                "We could not detect the module of the opened file."
            );
            return;
        }
        Repo repo;
        try {
            repo  = Repo.getTable().get(repoPath);
        } catch (SQLException error) {
            NotificationManager.getInstance().notifyError(
                "An error occurred trying to perform repo playback action. Could not get repo record from the database.", project
            );
            CodeSyncLogger.critical(String.format(
                "An error occurred trying to perform repo playback action. Error while fetching repo record. Error: %s",
                error.getMessage()
            ));

            return;
        } catch (RepoNotFound ex) {
            NotificationManager.getInstance().notifyError(
                String.format(
                    "An error occurred trying to perform repo playback action. Because Repo '%s' is not being synced.",
                    repoName
                ),
                project
            );
            CodeSyncLogger.warning(String.format(
                "An error occurred trying to perform repo playback action. Repo '%s' not found in the database.",
                repoPath
            ));

            return;
        }
        BrowserUtil.browse(String.format(REPO_PLAYBACK_LINK, repo.getServerRepoId()));
    }
}
