package org.intellij.sdk.codesync.codeSyncSetup;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.intellij.sdk.codesync.CodeSyncLogger;
import org.intellij.sdk.codesync.NotificationManager;
import org.intellij.sdk.codesync.actions.BaseModuleAction;
import org.intellij.sdk.codesync.alerts.PricingAlerts;
import org.intellij.sdk.codesync.exceptions.InvalidConfigFileError;
import org.intellij.sdk.codesync.exceptions.SQLiteDBConnectionError;
import org.intellij.sdk.codesync.exceptions.SQLiteDataError;
import org.intellij.sdk.codesync.exceptions.base.BaseException;
import org.intellij.sdk.codesync.exceptions.base.BaseNetworkException;
import org.intellij.sdk.codesync.exceptions.database.UserNotFound;
import org.intellij.sdk.codesync.exceptions.network.RepoUpdateError;
import org.intellij.sdk.codesync.exceptions.network.ServerConnectionError;
import org.intellij.sdk.codesync.state.RepoStatus;
import org.intellij.sdk.codesync.utils.FileUtils;
import org.intellij.sdk.codesync.utils.ProjectUtils;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

import static org.intellij.sdk.codesync.Constants.Notification;

public class CodeSyncSetupAction extends BaseModuleAction {
    @Override
    public void update(@NotNull AnActionEvent e) {
        // Check if any of the modules inside this project are synced with CodeSync or not.
        // We will show repo playback button if even a single repo is synced.
        Project project = e.getProject();
        if (project ==  null) {
            e.getPresentation().setEnabled(false);
            return;
        }

        if (this.isAccountDeactivated()) {
            e.getPresentation().setEnabled(false);
            return;
        }

        VirtualFile repoRoot = this.getRepoRoot(e, project);
        if (repoRoot == null) {
            e.getPresentation().setEnabled(false);
            return;
        }

        // A single file is opened, no need to sync it.
        if (!repoRoot.isDirectory()) {
            e.getPresentation().setEnabled(false);
        }

        RepoStatus repoStatus = this.getRepoStatus(repoRoot);
        this.setButtonRepresentation(e, repoStatus);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();

        if (project == null) {
            NotificationManager.getInstance().notifyError("An error occurred trying to perform repo playback action.");
            CodeSyncLogger.error("An error occurred trying to perform repo playback action. e.getProject() is null.");

            return;
        }

        VirtualFile repoRoot = this.getRepoRoot(e, project);
        if (repoRoot == null) {
            NotificationManager.getInstance().notifyError(Notification.REPO_SYNC_ACTION_FAILED, project);
            CodeSyncLogger.error("Could not disconnect the repo. null module root returned.");
            return;
        }

        String repoPath = FileUtils.normalizeFilePath(repoRoot);
        String repoName = repoRoot.getName();
        RepoStatus repoStatus = getRepoStatus(repoRoot);

        switch (repoStatus){
            case IN_SYNC:
                try {
                    CodeSyncSetup.disconnectRepo(project, repoPath, repoName);
                } catch (BaseException | BaseNetworkException | SQLException error) {
                    NotificationManager.getInstance().notifyError(Notification.REPO_UNSYNC_FAILED, project);
                    NotificationManager.getInstance().notifyError(error.getMessage(), project);
                    CodeSyncLogger.critical(
                        String.format("Could not disconnect the repo. Error: %s", error.getMessage())
                    );
                }
                return;
            case NOT_SYNCED:
            case UNKNOWN:
                if (PricingAlerts.getPlanLimitReached()) {
                    // Show Pricing alert dialog.
                    PricingAlerts.setPlanLimitReached(project);
                } else {
                    CodeSyncSetup.setupCodeSyncRepoAsync(project, repoPath, repoName, true, false);
                }
                break;
            case DISCONNECTED:
                try {
                    CodeSyncSetup.reconnectRepo(project, repoPath, repoName);
                } catch (UserNotFound | ServerConnectionError | RepoUpdateError | SQLException error) {
                    NotificationManager.getInstance().notifyError(Notification.REPO_RECONNECT_FAILED, project);
                    NotificationManager.getInstance().notifyError(error.getMessage(), project);
                    CodeSyncLogger.critical(
                        String.format("Could not reconnect the repo. Error: %s", error.getMessage())
                    );
                }
                break;
        }
    }
    private void setButtonRepresentation(AnActionEvent e, RepoStatus repoStatus) {
        switch (repoStatus) {
            case IN_SYNC:
                 e.getPresentation().setText("Disconnect Repo");
                 e.getPresentation().setDescription("Disconnect repo");
                 break;
            case DISCONNECTED:
                 e.getPresentation().setText("Reconnect Repo");
                 e.getPresentation().setDescription("Reconnect repo");
                 break;
            case SYNC_IN_PROGRESS:
                e.getPresentation().setText("Connecting Repo");
                e.getPresentation().setDescription("Connecting repo");
                e.getPresentation().setEnabled(false);
                break;
            case NOT_SYNCED:
            case UNKNOWN:
                 e.getPresentation().setText("Connect Repo");
                 e.getPresentation().setDescription("Connect repo with CodeSync");
                 break;
        }
    }

    private VirtualFile getRepoRoot(AnActionEvent anActionEvent, Project project) {
        VirtualFile[] contentRoots = ProjectUtils.getAllContentRoots(project);

        if (contentRoots.length > 1) {
            // If more than one module are present in the project then a file must be open to show repo setup action
            // this is needed because without the file we can not determine the correct repo sync.
            VirtualFile virtualFile = anActionEvent.getRequiredData(CommonDataKeys.PSI_FILE).getVirtualFile();
            return ProjectUtils.getRepoRoot(virtualFile, project);
        } else if (contentRoots.length == 1) {
            // If there is only one module then we can simply show the repo playback for the only repo present.
            // Disable the button if repo is not in sync.
            return contentRoots[0];
        }

        return null;
    }
}
