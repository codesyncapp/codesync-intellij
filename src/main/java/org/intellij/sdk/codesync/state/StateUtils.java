package org.intellij.sdk.codesync.state;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.intellij.sdk.codesync.exceptions.InvalidConfigFileError;
import org.intellij.sdk.codesync.exceptions.SQLiteDBConnectionError;
import org.intellij.sdk.codesync.files.ConfigFile;
import org.intellij.sdk.codesync.files.ConfigRepo;
import org.intellij.sdk.codesync.models.UserAccount;
import org.intellij.sdk.codesync.ui.notifications.DeactivatedAccountNotification;
import org.intellij.sdk.codesync.utils.FileUtils;
import org.intellij.sdk.codesync.utils.ProjectUtils;

import java.util.HashMap;
import java.util.Map;

import static org.intellij.sdk.codesync.Constants.CONFIG_PATH;

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
            UserAccount userAccount = new UserAccount();
            userAccount = userAccount.getActiveUser();
            globalState.isAuthenticated = userAccount != null;
            if (userAccount != null) {
                globalState.userEmail = userAccount.getUserEmail();
            }
        } catch (SQLiteDBConnectionError error) {
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
            ConfigFile configFile = new ConfigFile(CONFIG_PATH);

            if (configFile.hasRepo(repoPath)) {
                ConfigRepo configRepo = configFile.getRepo(repoPath);
                pluginState.isRepoInSync = configRepo.isSynced() && !configRepo.isDisconnected;

                if (configRepo.isSynced() && !configRepo.isDisconnected) {
                    pluginState.repoStatus = RepoStatus.IN_SYNC;
                } else if (!configRepo.isSynced()) {
                    pluginState.repoStatus = RepoStatus.NOT_SYNCED;
                } else if (configRepo.isDisconnected) {
                    pluginState.repoStatus = RepoStatus.DISCONNECTED;
                }
            } else {
                pluginState.repoStatus = RepoStatus.NOT_SYNCED;
                pluginState.isRepoInSync = false;
            }
        } catch (InvalidConfigFileError error) {
            pluginState.repoStatus = RepoStatus.UNKNOWN;
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
