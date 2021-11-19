package org.intellij.sdk.codesync.actions;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import org.intellij.sdk.codesync.CodeSyncLogger;
import org.intellij.sdk.codesync.NotificationManager;
import org.intellij.sdk.codesync.Utils;
import org.intellij.sdk.codesync.exceptions.InvalidConfigFileError;
import org.intellij.sdk.codesync.files.ConfigFile;
import org.intellij.sdk.codesync.files.ConfigRepo;
import org.intellij.sdk.codesync.files.ConfigRepoBranch;
import org.intellij.sdk.codesync.utils.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.regex.Pattern;

import static org.intellij.sdk.codesync.Constants.CONFIG_PATH;
import static org.intellij.sdk.codesync.Constants.FILE_PLAYBACK_LINK;

public class FilePlaybackAction extends AnAction {
    @Override
    public void update(AnActionEvent e) {
        // Only enable file playback button when some file is opened in the editor.
        try {
            // This file may seem to have no effect but this is the most important line here.
            // This will raise AssertionError if no file is opened.
            e.getRequiredData(CommonDataKeys.PSI_FILE);
            e.getPresentation().setEnabled(true);
        } catch (AssertionError error) {
            e.getPresentation().setEnabled(false);
        }
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if(project == null) {
            NotificationManager.notifyError("An error occurred trying to perform file playback action.");
            CodeSyncLogger.logEvent("An error occurred trying to perform file playback action. e.getProject() is null.");

            return;
        }
        String repoPath = FileUtils.normalizeFilePath(project.getBasePath());
        String openedFilePath = e.getRequiredData(CommonDataKeys.PSI_FILE).getVirtualFile().getPath();
        openedFilePath = FileUtils.normalizeFilePath(openedFilePath);
        String relativeFilePath = openedFilePath.replace(repoPath, "")
                .replaceFirst(Pattern.quote(String.valueOf(File.separatorChar)), "");

        ConfigRepo configRepo;
        try {
            ConfigFile configFile = new ConfigFile(CONFIG_PATH);
            configRepo = configFile.getRepo(repoPath);
        } catch (InvalidConfigFileError error) {
            NotificationManager.notifyError("An error occurred trying to perform file playback action.");
            CodeSyncLogger.logEvent(String.format(
                    "An error occurred trying to perform file playback action. Invalid Config File. Error: %s",
                    error.getMessage()
            ));

            return;
        }

        if (configRepo == null) {
            NotificationManager.notifyError("An error occurred trying to perform file playback action.");
            CodeSyncLogger.logEvent(String.format(
                    "An error occurred trying to perform file playback action. Repo '%s' not found in the config file.",
                    repoPath
            ));

            return;
        }
        String branchName = Utils.GetGitBranch(repoPath);
        ConfigRepoBranch configRepoBranch = configRepo.getRepoBranch(branchName);

        if (configRepoBranch == null) {
            NotificationManager.notifyError("An error occurred trying to perform file playback action.");
            CodeSyncLogger.logEvent(String.format(
                    "An error occurred trying to perform file playback action. " +
                            "Branch '%s' not found in the repo '%s' of the config file.",
                    branchName,
                    repoPath
            ));

            return;
        }

        Integer fileId = configRepoBranch.getFileId(relativeFilePath);

        if (fileId == null) {
            NotificationManager.notifyError("An error occurred trying to perform file playback action.");
            CodeSyncLogger.logEvent(String.format(
                    "An error occurred trying to perform file playback action. " +
                            "File '%s' not found in the repo '%s' of the config file.",
                    relativeFilePath,
                    repoPath
            ));

            return;
        }
        String url = String.format(FILE_PLAYBACK_LINK, fileId);
        BrowserUtil.browse(url);
    }
}
