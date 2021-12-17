package org.intellij.sdk.codesync.state;

import com.intellij.openapi.project.Project;
import org.intellij.sdk.codesync.exceptions.InvalidConfigFileError;
import org.intellij.sdk.codesync.exceptions.InvalidYmlFileError;
import org.intellij.sdk.codesync.files.ConfigFile;
import org.intellij.sdk.codesync.files.ConfigRepo;
import org.intellij.sdk.codesync.files.UserFile;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import static org.intellij.sdk.codesync.Constants.CONFIG_PATH;
import static org.intellij.sdk.codesync.Constants.USER_FILE_PATH;

public class StateUtils {
    private static final Map<String, PluginState> projectStateMap = new HashMap<>();

    static public PluginState getState(String projectPath){
        return projectStateMap.get(projectPath);
    }

    static public PluginState getState(Project project){
        if (project != null) {
            return getState(project.getBasePath());
        }

        return null;
    }

    static public void populateState(Project project){
        String repoPath = project.getBasePath();
        PluginState pluginState = new PluginState();

        pluginState.project = project;
        pluginState.repoPath = repoPath;

        try {
            UserFile userFile = new UserFile(USER_FILE_PATH);
            UserFile.User user = userFile.getActiveUser();
            pluginState.isAuthenticated = user != null;
        } catch (FileNotFoundException | InvalidYmlFileError error) {
            pluginState.isAuthenticated = false;
        }


        try {
            ConfigFile configFile = new ConfigFile(CONFIG_PATH);

            if (configFile.hasRepo(repoPath)) {
                ConfigRepo configRepo = configFile.getRepo(repoPath);
                pluginState.isRepoInSync = configRepo.isSuccessfullySynced();
            } else {
                pluginState.isRepoInSync = false;
            }
        } catch (InvalidConfigFileError error) {
            pluginState.isRepoInSync = false;
        }

        projectStateMap.put(repoPath, pluginState);
    }
}
