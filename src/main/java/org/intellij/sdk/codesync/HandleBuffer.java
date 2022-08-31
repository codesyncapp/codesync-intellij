package org.intellij.sdk.codesync;

import com.intellij.openapi.project.Project;
import kotlin.Pair;
import org.intellij.sdk.codesync.clients.CodeSyncClient;
import org.intellij.sdk.codesync.clients.CodeSyncWebSocketClient;
import org.intellij.sdk.codesync.exceptions.*;
import org.intellij.sdk.codesync.files.*;
import org.intellij.sdk.codesync.locks.CodeSyncLock;
import org.intellij.sdk.codesync.repoManagers.DeletedRepoManager;
import org.intellij.sdk.codesync.repoManagers.OriginalsRepoManager;
import org.intellij.sdk.codesync.repoManagers.ShadowRepoManager;
import org.intellij.sdk.codesync.utils.CommonUtils;
import org.intellij.sdk.codesync.utils.FileUtils;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.intellij.sdk.codesync.Constants.*;


public class HandleBuffer {
    /**
     * Returns a list of diff files available in the buffer.
     *
     * @param  path  an absolute path of the director containing diff files.
     * @param  diffFileExtension the extension of the diff files.
     * @return the list of diff files available in the buffer.
     */
    private static final Set<String> diffFilesBeingProcessed = new HashSet<>();
    private static final Set<String> diffReposToIgnore = new HashSet<>();

    public static void clearReposToIgnore() {
        diffReposToIgnore.clear();
    }

    public static boolean shouldSkipDiffFile(DiffFile diffFile, ConfigFile configFile) {
        if (diffReposToIgnore.contains(diffFile.repoPath)) {
            System.out.printf("Ignoring diff file '%s'.%n", diffFile.originalDiffFile.getPath());
            return true;
        }

        if (!diffFile.isValid()) {
            CodeSyncLogger.logEvent(
                    String.format("Skipping invalid diff file: %s, Diff: %s",
                        diffFile.originalDiffFile.getPath(), diffFile.contents
                    ),
                    LogMessageType.INFO
            );
            diffFile.delete();
            return true;
        }

        if (!configFile.hasRepo(diffFile.repoPath)) {
            CodeSyncLogger.logEvent(String.format(
                    "Repo %s is not in config.yml.", diffFile.repoPath
            ), LogMessageType.WARNING);
            diffFile.delete();
            return true;
        }

        if (configFile.isRepoDisconnected(diffFile.repoPath)) {
            CodeSyncLogger.logEvent(String.format(
                    "Repo %s is disconnected.", diffFile.repoPath
            ), LogMessageType.INFO);
            diffFile.delete();
            return true;
        }

        ConfigRepo configRepo = configFile.getRepo(diffFile.repoPath);

        if (!configRepo.containsBranch(diffFile.branch)) {
            CodeSyncLogger.logEvent(
                String.format(
                    "Branch: %s is not synced for Repo %s.", diffFile.branch, diffFile.repoPath
                ),
                configRepo.email,
                LogMessageType.WARNING
            );
            return true;
        }

        return false;
    }

    public static  DiffFile[] getDiffFiles(String path, String diffFileExtension, ConfigFile configFile)  {
        File diffFilesDirectory = new File(path);

        File[] files = diffFilesDirectory.listFiles(
            (dir, name) -> name.toLowerCase().endsWith(diffFileExtension.toLowerCase())
        );
        if (files != null)  {
            return Arrays.stream(files)
                .map(DiffFile::new)
                .filter(diffFile -> !shouldSkipDiffFile(diffFile, configFile))
                .toArray(DiffFile[]::new);
        }
        return new DiffFile[0];
    }

    private static void bufferHandler(final Timer timer, Project project) {
        timer.schedule(new TimerTask() {
            public void run() {
                try {
                    HandleBuffer.handleBuffer(project);
                } catch (Exception e) {
                    System.out.println("handleBuffer with error:");
                    e.printStackTrace();
                }

                bufferHandler(timer, project);
            }
        }, DELAY_BETWEEN_BUFFER_TASKS);
    }

    public static void scheduleBufferHandler(Project project) {
        Timer timer = new Timer(true);
        bufferHandler(timer, project);
    }

    public static void handleBuffer(Project project) {
        CodeSyncLock codeSyncLock = new CodeSyncLock(DIFFS_DAEMON_LOCK_KEY);
        if (!codeSyncLock.acquireLock(project.getName())) {
            return;
        }

        ConfigFile configFile;
        HashSet<String> newFiles = new HashSet<>();
        System.out.println("handleBuffer called:");

        try {
            configFile = new ConfigFile(CONFIG_PATH);
        } catch (InvalidConfigFileError error) {
            CodeSyncLogger.logEvent(String.format("Config file error, %s.\n", error.getMessage()), LogMessageType.CRITICAL);
            return;
        }

        DiffFile[] diffFiles = getDiffFiles(DIFFS_REPO, ".yml", configFile);
        ArrayList<Pair<Integer, DiffFile>> diffsToSend = new ArrayList<>();

        // We only process one repo per handleBuffer call.
        String currentRepo = null;

        if (diffFiles.length == 0) {
            return;
        }

        CodeSyncClient client = new CodeSyncClient();
        if (!client.isServerUp()) {
            CodeSyncLogger.logEvent(CONNECTION_ERROR_MESSAGE, LogMessageType.CRITICAL);
            return;
        }

        diffFiles = Arrays.copyOfRange(
            diffFiles, 0, diffFiles.length >= DIFFS_PER_ITERATION ? DIFFS_PER_ITERATION : diffFiles.length
        );

        for (final DiffFile diffFile : diffFiles) {
            if (currentRepo == null) {
                currentRepo = diffFile.repoPath;
            }

            if (!currentRepo.equals(diffFile.repoPath)) {
                // We only process one repo per handleBuffer call.
                continue;
            }

            if (diffFilesBeingProcessed.contains(diffFile.originalDiffFile.getPath())) {
                System.out.printf("Skipping diff file: %s.\n", diffFile.originalDiffFile.getPath());
                // Skip this file.
                continue;
            } else {
                diffFilesBeingProcessed.add(diffFile.originalDiffFile.getPath());
            }

            System.out.printf("Processing diff file: %s.\n", diffFile.originalDiffFile.getPath());

            if (!configFile.repos.containsKey(diffFile.repoPath)) {
                CodeSyncLogger.logEvent(String.format("Repo `%s` is in buffer but not in configFile.yml.\n", diffFile.repoPath), LogMessageType.ERROR);
                diffFilesBeingProcessed.remove(diffFile.originalDiffFile.getPath());
                continue;
            }

            ConfigRepo configRepo = configFile.getRepo(diffFile.repoPath);
            String accessToken = UserFile.getAccessToken(configRepo.email);

            if (accessToken == null) {
                CodeSyncLogger.logEvent(String.format(
                        "Access token for user '%s' not present so skipping diff file '%s'.",
                        configRepo.email, diffFile.originalDiffFile.getPath()
                    ),
                    LogMessageType.CRITICAL
                );
                diffFilesBeingProcessed.remove(diffFile.originalDiffFile.getPath());
                diffReposToIgnore.add(diffFile.repoPath);
                continue;
            } else {
                diffReposToIgnore.remove(diffFile.repoPath);
            }

            if (!configRepo.branches.containsKey(diffFile.branch)) {
                CodeSyncLogger.logEvent(String.format("Branch: `%s` is not synced for Repo `%s`.\n", diffFile.branch, diffFile.repoPath), LogMessageType.WARNING);
                diffFilesBeingProcessed.remove(diffFile.originalDiffFile.getPath());
                continue;
            }

            if (FileUtils.shouldIgnoreFile(diffFile.fileRelativePath, diffFile.repoPath)) {
                diffFile.delete();
                diffFilesBeingProcessed.remove(diffFile.originalDiffFile.getPath());
                continue;
            }

            ConfigRepoBranch configRepoBranch = configRepo.getRepoBranch(diffFile.branch);
            if (diffFile.isNewFile) {
                newFiles.add(diffFile.fileRelativePath);
                boolean isSuccess = handleNewFile(client, accessToken, diffFile, configFile, configRepo, configRepoBranch);
                if (isSuccess) {
                    System.out.printf("Diff file '%s' successfully processed.\n", diffFile.originalDiffFile.getPath());
                    diffFile.delete();

                    // We also need to disconnect existing connections here,
                    // otherwise the server cache causes an error and file updates end in error until the IDE restarts.
                    client.getWebSocketClient(accessToken).disconnect();
                    diffFilesBeingProcessed.remove(diffFile.originalDiffFile.getPath());
                    continue;
                }
            }

            if (newFiles.contains(diffFile.fileRelativePath)) {
                // Skip the changes diffs if relevant file was uploaded in the same iteration, wait for next iteration
                diffFilesBeingProcessed.remove(diffFile.originalDiffFile.getPath());
                continue;
            }

            if (diffFile.isRename) {
                if (newFiles.contains(diffFile.oldRelativePath)) {
                    // If old_rel_path uploaded in the same iteration, wait for next iteration
                    diffFilesBeingProcessed.remove(diffFile.originalDiffFile.getPath());
                    continue;
                }

                Integer oldFileId = configRepoBranch.getFileId(diffFile.oldRelativePath);
                if (oldFileId == null) {
                    CodeSyncLogger.logEvent(String.format("old_file: %s was not synced for rename of %s/%s.\n",
                            diffFile.oldRelativePath, diffFile.repoPath, diffFile.fileRelativePath
                    ), LogMessageType.WARNING);
                    diffFile.delete();
                    diffFilesBeingProcessed.remove(diffFile.originalDiffFile.getPath());
                    continue;
                }

                boolean isSuccess = handleFileRename(configFile, configRepo, configRepoBranch, diffFile, oldFileId);
                if (!isSuccess) {
                    System.out.printf("Diff file '%s' could not be processed.\n", diffFile.originalDiffFile.getPath());
                    diffFilesBeingProcessed.remove(diffFile.originalDiffFile.getPath());
                    // Skip this iteration
                    continue;
                }
            }

            if (!diffFile.isBinary && !diffFile.isDeleted && diffFile.diff.length() == 0) {
                System.out.printf("Empty diff found in file: %s. Removing...\n", diffFile.fileRelativePath);
                diffFilesBeingProcessed.remove(diffFile.originalDiffFile.getPath());
                // Delete empty diff files.
                diffFile.delete();
                continue;
            }

            Integer fileId = configRepoBranch.getFileId(diffFile.fileRelativePath);
            if (fileId == null) {
                if (diffFile.isDeleted) {
                    ShadowRepoManager shadowRepoManager = new ShadowRepoManager(diffFile.repoPath, diffFile.branch);
                    cleanUpDeletedDiff(
                            configFile, configRepo, configRepoBranch, diffFile,
                            shadowRepoManager.getFilePath(diffFile.fileRelativePath)
                    );
                    diffFile.delete();
                    diffFilesBeingProcessed.remove(diffFile.originalDiffFile.getPath());
                    continue;
                }
                if (diffFile.isRename) {
                    forceUploadNullFile(client, accessToken, diffFile, configFile, configRepo, configRepoBranch);
                    diffFilesBeingProcessed.remove(diffFile.originalDiffFile.getPath());
                    continue;
                }
                Path filePath = Paths.get(diffFile.repoPath, diffFile.fileRelativePath);

                // Delete the diff file if actual file whose updates are in the diff file is now non-existent.
                if (!filePath.toFile().exists()) {
                    diffFile.delete();
                } else {
                    // If file exists, then we need to upload this file to the server.
                    forceUploadNullFile(client, accessToken, diffFile, configFile, configRepo, configRepoBranch);
                }

                diffFilesBeingProcessed.remove(diffFile.originalDiffFile.getPath());
                continue;
            }

            if (diffFile.isDeleted) {
                diffFile.setDiff(
                    getDiffOfDeletedFile(configFile, configRepo, configRepoBranch, diffFile)
                );
            }
            diffsToSend.add(new Pair<>(fileId, diffFile));
        }

        if (configFile.isRepoDisconnected(currentRepo)) {
            CodeSyncLogger.logEvent("Repo is disconnected so, skipping the diffs.", LogMessageType.INFO);
            return;
        }

        // Send Diffs in a single request.

        ConfigRepo configRepo = configFile.getRepo(currentRepo);
        String accessToken = UserFile.getAccessToken(configRepo.email);
        if (accessToken ==  null) {
            CodeSyncLogger.logEvent(String.format(
                    "Access token for user '%s' not present so skipping diffs for repo '%s'.",
                    configRepo.email, currentRepo),
                    LogMessageType.WARNING
            );
            return;
        } else {
            diffReposToIgnore.remove(currentRepo);
        }

        CodeSyncWebSocketClient codeSyncWebSocketClient = client.getWebSocketClient(accessToken);
        codeSyncWebSocketClient.connect(isConnected -> {
            if (isConnected) {
                try {
                    codeSyncWebSocketClient.sendDiffs(diffsToSend, (successfullyTransferred, diffFilePath) -> {
                        diffFilesBeingProcessed.remove(diffFilePath);
                        if (!successfullyTransferred) {
                            CodeSyncLogger.logEvent("Error while sending the diff files to the server.", configRepo.email, LogMessageType.ERROR);
                            return;
                        }
                        System.out.printf("Diff file '%s' successfully processed.\n", diffFilePath);
                        DiffFile.delete(diffFilePath);
                    });
                } catch (WebSocketConnectionError error) {
                    diffFilesBeingProcessed.clear();
                    CodeSyncLogger.logEvent(String.format("Connection error while sending diff to the server at %s.\n", WEBSOCKET_ENDPOINT), configRepo.email, LogMessageType.CRITICAL);
                }
            } else {
                diffFilesBeingProcessed.clear();
                CodeSyncLogger.logEvent(String.format("Failed to connect to websocket endpoint: %s.\n", WEBSOCKET_ENDPOINT), configRepo.email, LogMessageType.ERROR);
            }
        });
    }

    public static boolean handleNewFile(CodeSyncClient client, String accessToken, DiffFile diffFile, ConfigFile configFile, ConfigRepo repo, ConfigRepoBranch configRepoBranch) {
        String branchName = Utils.GetGitBranch(repo.repoPath);
        OriginalsRepoManager originalsRepoManager = new OriginalsRepoManager(repo.repoPath, branchName);

        Path originalsFilePath = originalsRepoManager.getFilePath(diffFile.fileRelativePath);
        File originalsFile = originalsFilePath.toFile();

        if (!originalsFile.exists()) {
            System.out.printf("Original file: %s not found.\n", originalsFilePath);
        }

        System.out.printf("Uploading new file: %s .\n", diffFile.fileRelativePath);
        try {
            Integer fileId = client.uploadFile(accessToken, repo, diffFile, originalsFile);
            configRepoBranch.updateFileId(diffFile.fileRelativePath, fileId);
            try {
                configFile.publishBranchUpdate(repo, configRepoBranch);
            } catch (InvalidConfigFileError error)  {
                CodeSyncLogger.logEvent(String.format("Error while updating the config file with new file ID. \n%s", error.getMessage()), LogMessageType.CRITICAL);
                error.printStackTrace();
                return false;
            }
        } catch (FileInfoError error) {
            CodeSyncLogger.logEvent(String.format("Error while getting file information. \n%s", error.getMessage()), LogMessageType.ERROR);
            error.printStackTrace();
            return false;
        } catch (RequestError | InvalidJsonError error) {
            CodeSyncLogger.logEvent(String.format("Error while uploading a new file '%s'. \n%s", diffFile.fileRelativePath, error.getMessage()), LogMessageType.ERROR);
            error.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean handleFileRename(ConfigFile configFile, ConfigRepo configRepo, ConfigRepoBranch configRepoBranch, DiffFile diffFile, Integer oldFileId) {
        ShadowRepoManager shadowRepoManager = new ShadowRepoManager(diffFile.repoPath, diffFile.branch);
        shadowRepoManager.renameFile(diffFile.oldRelativePath, diffFile.fileRelativePath);

        configRepoBranch.updateFileId(diffFile.fileRelativePath, oldFileId);

        try {
            configFile.publishBranchUpdate(configRepo, configRepoBranch);
            return true;
        } catch (InvalidConfigFileError error) {
            error.printStackTrace();
            return false;
        }
    }

    public static String getDiffOfDeletedFile(ConfigFile configFile, ConfigRepo configRepo, ConfigRepoBranch configRepoBranch, DiffFile diffFile ) {
        ShadowRepoManager shadowRepoManager = new ShadowRepoManager(diffFile.repoPath, diffFile.branch);
        Path shadowPath = shadowRepoManager.getFilePath(diffFile.fileRelativePath);

        File shadowFile = shadowPath.toFile();
        String diff = "";

        if (!shadowFile.exists()) {
            cleanUpDeletedDiff(configFile,  configRepo, configRepoBranch, diffFile, shadowPath);
            return diff;
        }
        try {
            Map<String, Object> fileInfo = FileUtils.getFileInfo(shadowPath.toString());
            if ((Boolean) fileInfo.get("isBinary")) {
                cleanUpDeletedDiff(configFile,  configRepo, configRepoBranch, diffFile, shadowPath);
                return diff;
            }
        } catch (FileInfoError error) {
            error.printStackTrace();
        }
        String shadowText = FileUtils.readLineByLineJava8(shadowPath);
        diff = CommonUtils.computeDiff(shadowText, "");
        cleanUpDeletedDiff(configFile,  configRepo, configRepoBranch, diffFile, shadowPath);

        return diff;
    }

    public static void cleanUpDeletedDiff (ConfigFile configFile, ConfigRepo configRepo, ConfigRepoBranch configRepoBranch, DiffFile diffFile, Path shadowPath) {
        OriginalsRepoManager originalsRepoManager = new OriginalsRepoManager(diffFile.repoPath, diffFile.branch);
        DeletedRepoManager deletedRepoManager = new DeletedRepoManager(diffFile.repoPath, diffFile.branch);

        File originalsFile = originalsRepoManager.getFilePath(diffFile.fileRelativePath).toFile();
        File deletedFile = deletedRepoManager.getFilePath(diffFile.fileRelativePath).toFile();
        File shadowFile = shadowPath.toFile();

        if (originalsFile.exists()) {
            originalsFile.delete();
        }
        if (deletedFile.exists()) {
            deletedFile.delete();
        }
        if (shadowFile.exists()) {
            shadowFile.delete();
        }
        try {
            configRepoBranch.removeFileId(diffFile.fileRelativePath);
            configFile.publishBranchUpdate(configRepo, configRepoBranch);
        } catch (InvalidConfigFileError error) {
            error.printStackTrace();
        }
    }

    public static boolean forceUploadNullFile(CodeSyncClient client, String accessToken, DiffFile diffFile, ConfigFile configFile, ConfigRepo repo, ConfigRepoBranch configRepoBranch) {
        OriginalsRepoManager originalsRepoManager = new OriginalsRepoManager(diffFile.repoPath, diffFile.branch);
        File originalsFile = originalsRepoManager.getFilePath(diffFile.fileRelativePath).toFile();

        // Copy the file to originals directory if it is not already in there.
        if (!originalsFile.exists()) {
            Path filePath = Paths.get(diffFile.repoPath, diffFile.fileRelativePath);
            originalsRepoManager.copyFiles(new String[]{filePath.toString()});
        }
        return handleNewFile(client, accessToken, diffFile, configFile, repo, configRepoBranch);
    }
}
