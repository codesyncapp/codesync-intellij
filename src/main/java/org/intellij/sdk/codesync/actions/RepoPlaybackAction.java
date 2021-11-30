package org.intellij.sdk.codesync.actions;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.intellij.sdk.codesync.CodeSyncLogger;
import org.intellij.sdk.codesync.NotificationManager;
import org.intellij.sdk.codesync.exceptions.InvalidConfigFileError;
import org.intellij.sdk.codesync.files.ConfigFile;
import org.intellij.sdk.codesync.files.ConfigRepo;
import org.intellij.sdk.codesync.state.PluginState;
import org.intellij.sdk.codesync.state.StateUtils;
import org.intellij.sdk.codesync.utils.FileUtils;
import org.jetbrains.annotations.NotNull;


import static org.intellij.sdk.codesync.Constants.*;

public class RepoPlaybackAction extends AnAction {
    @Override
    public void update(AnActionEvent e) {
        PluginState pluginState = StateUtils.getState(e.getProject());
        // Disable the button if repo is not in sync.
        if (pluginState != null && !pluginState.isRepoInSync) {
            e.getPresentation().setEnabled(false);
        }
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if(project == null) {
            NotificationManager.notifyError("An error occurred trying to perform repo playback action.");
            CodeSyncLogger.logEvent("An error occurred trying to perform repo playback action. e.getProject() is null.");

            return;
        }
        String repoPath = FileUtils.normalizeFilePath(project.getBasePath());

        ConfigRepo configRepo;
        try {
            ConfigFile configFile = new ConfigFile(CONFIG_PATH);
             configRepo = configFile.getRepo(repoPath);
        } catch (InvalidConfigFileError error) {
            NotificationManager.notifyError("An error occurred trying to perform repo playback action.");
            CodeSyncLogger.logEvent(String.format(
                    "An error occurred trying to perform repo playback action. Invalid Config File. Error: %s",
                    error.getMessage()
            ));

            return;
        }

        if (configRepo == null) {
            NotificationManager.notifyError("An error occurred trying to perform repo playback action.");
            CodeSyncLogger.logEvent(String.format(
                    "An error occurred trying to perform repo playback action. Repo '%s' not found in the config file.",
                    repoPath
            ));

            return;
        }
        Integer repoId = configRepo.id;

        String url = String.format(REPO_PLAYBACK_LINK, repoId);
        BrowserUtil.browse(url);
    }
}
