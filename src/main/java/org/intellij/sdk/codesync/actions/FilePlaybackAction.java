package org.intellij.sdk.codesync.actions;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.intellij.sdk.codesync.CodeSyncLogger;
import org.intellij.sdk.codesync.NotificationManager;
import org.intellij.sdk.codesync.Utils;
import org.intellij.sdk.codesync.exceptions.InvalidConfigFileError;
import org.intellij.sdk.codesync.exceptions.common.FileNotInModuleError;
import org.intellij.sdk.codesync.files.ConfigFile;
import org.intellij.sdk.codesync.files.ConfigRepo;
import org.intellij.sdk.codesync.files.ConfigRepoBranch;
import org.intellij.sdk.codesync.utils.FileUtils;
import org.intellij.sdk.codesync.utils.ProjectUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.regex.Pattern;

import static org.intellij.sdk.codesync.Constants.CONFIG_PATH;
import static org.intellij.sdk.codesync.Constants.FILE_PLAYBACK_LINK;

public class FilePlaybackAction extends BaseModuleAction {
    @Override
    public void update(AnActionEvent e) {
        Project project = e.getProject();
        if (project ==  null) {
            e.getPresentation().setEnabled(false);
            return;
        }

        // Only enable file playback button when some file is opened in the editor.
        try {
            VirtualFile virtualFile = e.getRequiredData(CommonDataKeys.PSI_FILE).getVirtualFile();
            e.getPresentation().setEnabled(
                this.isRepoInSync(virtualFile, project)
            );
        } catch (AssertionError | FileNotInModuleError error) {
            e.getPresentation().setEnabled(false);
        }
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();

        if( project == null ) {
            NotificationManager.notifyError("An error occurred trying to perform file playback action.");
            CodeSyncLogger.warning("An error occurred trying to perform file playback action. e.getProject() is null.");

            return;
        }

        String repoPath, repoName;
        VirtualFile virtualFile = e.getRequiredData(CommonDataKeys.PSI_FILE).getVirtualFile();
        String openedFilePath = virtualFile.getPath();

        try {
            repoPath = ProjectUtils.getRepoPath(virtualFile, project);
            repoName = ProjectUtils.getRepoName(virtualFile, project);
        } catch (FileNotInModuleError error) {
            NotificationManager.notifyError("An error occurred trying to perform file playback action.", project);
            CodeSyncLogger.warning(String.format(
                    "An error occurred trying to perform file playback action. file '%s' is not present in the project.",
                    virtualFile.getPath()
            ));

            return;
        }

        openedFilePath = FileUtils.normalizeFilePath(openedFilePath);
        String relativeFilePath = openedFilePath.replace(repoPath, "")
                .replaceFirst(Pattern.quote(String.valueOf(File.separatorChar)), "");

        ConfigRepo configRepo;
        try {
            ConfigFile configFile = new ConfigFile(CONFIG_PATH);
            configRepo = configFile.getRepo(repoPath);
        } catch (InvalidConfigFileError error) {
            NotificationManager.notifyError(
                "An error occurred trying to perform file playback action. CodeSync configuration file is malformed.",
                project
            );
            CodeSyncLogger.critical(String.format(
                    "An error occurred trying to perform file playback action. Invalid Config File. Error: %s",
                    error.getMessage()
            ));

            return;
        }

        if (configRepo == null) {
            NotificationManager.notifyError(
                String.format(
                    "An error occurred trying to perform file playback action. Because Repo '%s' is not being synced.",
                        repoName
                ),
                project
            );
            CodeSyncLogger.warning(String.format(
                "An error occurred trying to perform file playback action. Repo '%s' not found in the config file.",
                repoPath
            ));

            return;
        }
        String branchName = Utils.GetGitBranch(repoPath);
        ConfigRepoBranch configRepoBranch = configRepo.getRepoBranch(branchName);

        if (configRepoBranch == null) {
            NotificationManager.notifyError(
                String.format(
                    "An error occurred trying to perform file playback action. Branch '%s' is not yet synced.",
                    branchName
                ),
                project
            );
            CodeSyncLogger.warning(String.format(
                "An error occurred trying to perform file playback action. " +
                "Branch '%s' not found in the config file repo '%s'.",
                branchName,
                repoPath
            ));

            return;
        }

        Integer fileId = configRepoBranch.getFileId(relativeFilePath);

        if (fileId == null) {   
            NotificationManager.notifyError(
                "An error occurred trying to perform file playback action. This file is not yet synced with CodeSync servers.",
                project
            );
            CodeSyncLogger.warning(String.format(
                "An error occurred trying to perform file playback action. " +
                        "File '%s' not found in the config file repo '%s'.",
                relativeFilePath,
                repoPath
            ));

            return;
        }
        String url = String.format(FILE_PLAYBACK_LINK, fileId);
        BrowserUtil.browse(url);
    }
}
