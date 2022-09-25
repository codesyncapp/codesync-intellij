package org.intellij.sdk.codesync.codeSyncSetup;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.intellij.sdk.codesync.CodeSyncLogger;
import org.intellij.sdk.codesync.NotificationManager;
import org.intellij.sdk.codesync.actions.BaseModuleAction;
import org.intellij.sdk.codesync.exceptions.base.BaseException;
import org.intellij.sdk.codesync.exceptions.base.BaseNetworkException;
import org.intellij.sdk.codesync.exceptions.common.FileNotInModuleError;
import org.intellij.sdk.codesync.utils.FileUtils;
import org.intellij.sdk.codesync.utils.ProjectUtils;
import org.jetbrains.annotations.NotNull;

import static org.intellij.sdk.codesync.Constants.*;

public class CodeSyncSetupAction extends BaseModuleAction {
    @Override
    public void update(@NotNull AnActionEvent e) {
        // Check if any of the modules inside this project are synced with codesync or not.
        // We will show repo playback button if even a single repo is synced.
        Project project = e.getProject();
        if (project ==  null) {
            e.getPresentation().setEnabled(false);
        }
        VirtualFile[] contentRoots = ProjectUtils.getAllContentRoots(e.getProject());
        if (contentRoots.length > 1) {
            // If more than one module are present in the project then a file must be open to show repo setup action
            // this is needed because without the file we can not determine the correct repo sync.
            try {
                VirtualFile virtualFile = e.getRequiredData(CommonDataKeys.PSI_FILE).getVirtualFile();
                if (this.isRepoInSync(virtualFile, e.getProject())) {
                    Presentation presentation = e.getPresentation();
                    presentation.setText("Disconnect Repo...");
                    presentation.setDescription("Disconnect repo...");
                } else {
                    Presentation presentation = e.getPresentation();
                    presentation.setText("Connect Repo");
                    presentation.setDescription("Connect repo with codeSync");
                }
            } catch (AssertionError | FileNotInModuleError error) {
                e.getPresentation().setEnabled(false);
            }
        } else if (contentRoots.length == 1) {
            // If there is only one module then we can simply show the repo playback for the only repo present.
            // Disable the button if repo is not in sync.
            if (this.isRepoInSync(contentRoots[0])) {
                Presentation presentation = e.getPresentation();
                presentation.setText("Disconnect Repo...");
                presentation.setDescription("Disconnect repo.");
            } else {
                Presentation presentation = e.getPresentation();
                presentation.setText("Connect Repo");
                presentation.setDescription("Connect repo with codeSync");
            }
        } else {
            e.getPresentation().setEnabled(false);
        }
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();

        // Check if any of the modules inside this project are synced with codesync or not.
        // We will show repo playback button if even a single repo is synced.
        VirtualFile[] contentRoots = ProjectUtils.getAllContentRoots(e.getProject());
        if (contentRoots.length > 1) {
            // If more than one module are present in the project then a file must be open to show repo setup action
            // this is needed because without the file we can not determine the correct repo sync.
            try {
                VirtualFile virtualFile = e.getRequiredData(CommonDataKeys.PSI_FILE).getVirtualFile();
                String repoPath = ProjectUtils.getRepoPath(virtualFile, project);
                String repoName = ProjectUtils.getRepoName(virtualFile, project);

                if (this.isRepoInSync(virtualFile, e.getProject())) {
                    try {
                        CodeSyncSetup.disconnectRepo(project, repoPath, repoName);
                    } catch (BaseException | BaseNetworkException error) {
                        NotificationManager.notifyError(Notification.REPO_UNSYNC_FAILED, project);
                        NotificationManager.notifyError(error.getMessage(), project);
                        CodeSyncLogger.critical(
                            String.format("Could not disconnect the repo. Error: %s", error.getMessage())
                        );
                    }
                } else {
                    CodeSyncSetup.setupCodeSyncRepoAsync(project, repoPath, repoName, true, false);
                }
            } catch (AssertionError | FileNotInModuleError error) {
                NotificationManager.notifyError(Notification.REPO_UNSYNC_FAILED, project);
                NotificationManager.notifyError(error.getMessage(), project);
                CodeSyncLogger.error(
                    String.format("Could not disconnect the repo. Error: %s", error.getMessage())
                );
            }
        } else if (contentRoots.length == 1) {
            // If there is only one module then we can simply show the repo playback for the only repo present.
            // Disable the button if repo is not in sync.
            VirtualFile contentRoot = contentRoots[0];
            String repoPath = FileUtils.normalizeFilePath(contentRoot.getPath());
            String repoName = contentRoot.getName();

            if (this.isRepoInSync(contentRoot)) {
                try {
                    CodeSyncSetup.disconnectRepo(project, repoPath, repoName);
                } catch (BaseException | BaseNetworkException error) {
                    NotificationManager.notifyError(Notification.REPO_UNSYNC_FAILED, project);
                    NotificationManager.notifyError(error.getMessage(), project);
                    CodeSyncLogger.critical(
                        String.format("Could not disconnect the repo. Error: %s", error.getMessage())
                    );
                }
            } else {
                CodeSyncSetup.setupCodeSyncRepoAsync(project, repoPath, repoName, true, false);
            }
        } else {
            NotificationManager.notifyError(Notification.REPO_UNSYNC_FAILED, project);
            CodeSyncLogger.error("Could not disconnect the repo. 0 module returned for the given project.");
        }
     }
}
