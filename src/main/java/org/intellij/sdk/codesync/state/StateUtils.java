package org.intellij.sdk.codesync.state;

import com.intellij.openapi.components.ServiceManager;
import org.intellij.sdk.codesync.CodeSyncLogger;
import org.intellij.sdk.codesync.exceptions.InvalidConfigFileError;
import org.intellij.sdk.codesync.exceptions.InvalidYmlFileError;
import org.intellij.sdk.codesync.files.ConfigFile;
import org.intellij.sdk.codesync.files.ConfigRepo;
import org.intellij.sdk.codesync.files.UserFile;

import java.io.FileNotFoundException;

import static org.intellij.sdk.codesync.Constants.CONFIG_PATH;
import static org.intellij.sdk.codesync.Constants.USER_FILE_PATH;

public class StateUtils {
    static public PluginState getState(){
        PluginStateService pluginStateService = ServiceManager.getService(PluginStateService.class);
        return pluginStateService.getState();
    }

    static public void populateState(String repoPath){
        PluginStateService pluginStateService = ServiceManager.getService(PluginStateService.class);
        PluginState state = pluginStateService.getState();

        if (state == null) {
            CodeSyncLogger.logEvent("Error while populating plugin state.");
            return;
        }
        state.repoPath = repoPath;

        try {
            UserFile userFile = new UserFile(USER_FILE_PATH);
            UserFile.User user = userFile.getUser();
            state.isAuthenticated = user != null;
        } catch (FileNotFoundException | InvalidYmlFileError error) {
            state.isAuthenticated = false;
        }


        try {
            ConfigFile configFile = new ConfigFile(CONFIG_PATH);
            ConfigRepo configRepo = configFile.getRepo(repoPath);
            state.isRepoInSync = configRepo.isSuccessfullySynced();
        } catch (InvalidConfigFileError error) {
            state.isRepoInSync = false;
        }

        pluginStateService.loadState(state);
    }
}
