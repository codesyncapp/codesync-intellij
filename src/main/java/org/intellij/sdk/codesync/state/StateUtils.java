package org.intellij.sdk.codesync.state;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.intellij.sdk.codesync.CodeSyncLogger;
import org.intellij.sdk.codesync.database.models.Repo;
import org.intellij.sdk.codesync.database.models.User;
import org.intellij.sdk.codesync.exceptions.database.RepoNotFound;
import org.intellij.sdk.codesync.ui.notifications.DeactivatedAccountNotification;
import org.intellij.sdk.codesync.utils.FileUtils;
import org.intellij.sdk.codesync.utils.ProjectUtils;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;


public class StateUtils {
    private static final Map<String, PluginState> projectStateMap = new HashMap<>();
    private static final PluginState globalState = new PluginState();

    static public PluginState getState(String projectPath){
        return projectStateMap.get(projectPath);
    }
    static public PluginState getGlobalState(){
        return globalState;
    }

    static public void populateGlobalState (Project project) {
        globalState.project = project;

        try {
            User user = User.getTable().getActive();
            globalState.isAuthenticated = user != null;
            if (user != null) {
                globalState.userEmail = user.getEmail();
            }
        } catch (SQLException e) {
            globalState.isAuthenticated = false;
        }
    }

    static public void populateState (Project project) {
        // Populate global state.
        populateGlobalState(project);

        VirtualFile[] contentRoots = ProjectUtils.getAllContentRoots(project);

        // Populate state for all the opened modules. Module is the term used for projects opened using "Attach" option
        // in the IDE open dialog box.
        for (VirtualFile contentRoot: contentRoots) {
            String repoPath = FileUtils.normalizeFilePath(contentRoot.getPath());
            StateUtils.populateModuleState(repoPath, project);
        }
    }

    /*
    Reload global and project state.

    This is useful in cases when auth action or repo sync action is performed and we want to make sure the state
    has to correct values for these things.
     */
    static public void reloadState(Project project) {
        populateGlobalState(project);
        populateState(project);
    }

    /*
    This method is dependent on `globalState` and must execute after that has already executed.
     */
    static private void populateModuleState(String repoPath, Project project){
        PluginState pluginState = new PluginState();

        pluginState.project = project;
        pluginState.repoPath = repoPath;
        pluginState.isAuthenticated = globalState.isAuthenticated;
        pluginState.userEmail = globalState.userEmail;
        pluginState.isAccountDeactivated = globalState.isAccountDeactivated;

        try {
            Repo repo = Repo.getTable().get(repoPath);
            switch (repo.getState()) {
                case SYNCED:
                    if (repo.hasSyncedBranches()){
                        pluginState.repoStatus = RepoStatus.IN_SYNC;
                        pluginState.isRepoInSync = true;
                    } else {
                        pluginState.repoStatus = RepoStatus.NOT_SYNCED;
                        pluginState.isRepoInSync = false;
                    }
                    break;
                case NOT_SYNCED:
                    pluginState.repoStatus = RepoStatus.NOT_SYNCED;
                    pluginState.isRepoInSync = false;
                    break;
                case DELETED:
                case DISCONNECTED:
                    pluginState.repoStatus = RepoStatus.DISCONNECTED;
                    pluginState.isRepoInSync = false;
                    break;
                default:
                    pluginState.repoStatus = RepoStatus.UNKNOWN;
                    pluginState.isRepoInSync = false;
                    break;
            }
        } catch (SQLException error) {
            CodeSyncLogger.error(String.format("Error getting repo from database. Error: %s", error.getMessage()));
            pluginState.repoStatus = RepoStatus.UNKNOWN;
            pluginState.isRepoInSync = false;
        } catch (RepoNotFound e) {
            pluginState.repoStatus = RepoStatus.NOT_SYNCED;
            pluginState.isRepoInSync = false;
        }

        projectStateMap.put(repoPath, pluginState);
    }

    public static RepoStatus getRepoStatus(String repoPath) {
        PluginState pluginState = StateUtils.getState(repoPath);
        if (pluginState == null) {
            return RepoStatus.UNKNOWN;
        }
        return pluginState.repoStatus;
    }
    public static void updateRepoStatus(String repoPath, RepoStatus repoStatus){
        PluginState pluginState = getState(repoPath);
        if(pluginState != null){
            pluginState.setRepoStatus(repoStatus);
        }
    }

    public static void deactivateAccount() {
        PluginState pluginState = getGlobalState();

        // Do nothing if account already deactivated.
        if (pluginState.isAccountDeactivated) {
            return;
        }
        DeactivatedAccountNotification deactivatedAccountNotification = new DeactivatedAccountNotification(pluginState.project);
        deactivatedAccountNotification.showAlert();
        pluginState.isAccountDeactivated = true;
        reloadState(pluginState.project);
    }

    public static void reactivateAccount() {
        PluginState pluginState = getGlobalState();
        pluginState.isAccountDeactivated = false;
        reloadState(pluginState.project);
    }
}
