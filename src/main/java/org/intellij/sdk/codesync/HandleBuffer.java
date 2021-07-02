package org.intellij.sdk.codesync;

import org.intellij.sdk.codesync.clients.CodeSyncClient;
import org.intellij.sdk.codesync.clients.CodeSyncWebSocketClient;
import org.intellij.sdk.codesync.exceptions.*;
import org.intellij.sdk.codesync.files.ConfigFile;
import org.intellij.sdk.codesync.files.ConfigRepo;
import org.intellij.sdk.codesync.files.ConfigRepoBranch;
import org.intellij.sdk.codesync.files.DiffFile;
import org.json.simple.JSONObject;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

import static org.intellij.sdk.codesync.Constants.*;


public class HandleBuffer {
    /**
     * Returns a list of diff files available in the buffer.
     *
     * @param  path  an absolute path of the director containing diff files.
     * @param  diffFileExtension the extension of the diff files.
     * @return the list of diff files available in the buffer.
     */
    public static  DiffFile[] getDiffFiles(String path, String diffFileExtension)  {
        File diffFilesDirectory = new File(path);

        File[] files = diffFilesDirectory.listFiles(
            (dir, name) -> name.toLowerCase().endsWith(diffFileExtension.toLowerCase())
        );
        if (files != null)  {
            return Arrays.stream(files).map(DiffFile::new).toArray(DiffFile[]::new);
        }
        return new DiffFile[0];
    }

    private static void bufferHandler(final Timer timer) {
        timer.schedule(new TimerTask() {
            public void run() {
                try {
                    HandleBuffer.handleBuffer();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                bufferHandler(timer);
            }
        }, DELAY_BETWEEN_BUFFER_TASKS);
    }

    public static void scheduleBufferHandler() {
        Timer timer = new Timer(true);
        bufferHandler(timer);
    }

    public static void handleBuffer() {
        ConfigFile configFile;
        HashSet<String> newFiles = new HashSet<>();

        DiffFile[] diffFiles = getDiffFiles(DIFFS_REPO, ".yml");
        if (diffFiles.length == 0) {
            return;
        }

        try {
            configFile = new ConfigFile(CONFIG_PATH);
        } catch (InvalidConfigFileError error) {
            System.out.printf("Config file error, %s.\n", error.getMessage());
            return;
        }

        CodeSyncClient client = new CodeSyncClient();
        if (!client.isServerUp()) {
            return;
        }

        diffFiles = Arrays.copyOfRange(
            diffFiles, 0, diffFiles.length >= DIFFS_PER_ITERATION ? DIFFS_PER_ITERATION : diffFiles.length
        );

        // Group diff files by repo.
        Map<String, ArrayList<DiffFile>> diffFilesPerRepo = new HashMap<>();
        for (DiffFile diffFile: diffFiles) {
            ArrayList<DiffFile> _diffFiles = diffFilesPerRepo.get(diffFile.repoPath);

            if (_diffFiles == null) {
                _diffFiles = new ArrayList<>();
                _diffFiles.add(diffFile);
                diffFilesPerRepo.put(diffFile.repoPath, _diffFiles);
            } else {
                _diffFiles.add(diffFile);
            }
        }
        for (Map.Entry<String, ArrayList<DiffFile>> entry : diffFilesPerRepo.entrySet()) {
           String repoPath = entry.getKey();
           final DiffFile[] diffFileArrayList = entry.getValue().toArray(new DiffFile[0]);
           ConfigRepo configRepo = configFile.getRepo(repoPath);

            CodeSyncWebSocketClient codeSyncWebSocketClient = client.getWebSocketClient();
            codeSyncWebSocketClient.connect(configRepo.token, isConnected -> {
                if (isConnected) {
                    processDiffFiles(configFile, newFiles, diffFileArrayList, client, codeSyncWebSocketClient);
                } else {
                    System.out.printf("Failed to connect to websocket endpoint: %s.\n", WEBSOCKET_ENDPOINT);
                }
            });

        }


    }

    private static void processDiffFiles(ConfigFile configFile, HashSet<String> newFiles, DiffFile[] diffFiles, CodeSyncClient client, CodeSyncWebSocketClient codeSyncWebSocketClient) {
        for (DiffFile diffFile: diffFiles) {
            System.out.printf("Processing diff file: %s.\n", diffFile.originalDiffFile.getPath());

            if (!diffFile.isValid()) {
                String filePath =  String.format("%s/%s", diffFile.repoPath, diffFile.fileRelativePath);
                System.out.printf("Skipping invalid diff file: %s. data: %s.\n", filePath, diffFile.diff);

                diffFile.delete();
                continue;
            }

            if (!configFile.repos.containsKey(diffFile.repoPath)) {
                System.out.printf("Repo `%s` is in buffer.yml but not in configFile.yml.\n", diffFile.repoPath);
                continue;
            }

            ConfigRepo configRepo = configFile.getRepo(diffFile.repoPath);
            if (!configRepo.branches.containsKey(diffFile.branch))  {
                System.out.printf("Branch: `%s` is not synced for Repo `%s`.\n", diffFile.branch, diffFile.repoPath);
                continue;
            }

            if (Utils.shouldIgnoreFile(diffFile.fileRelativePath, diffFile.repoPath)) {
                diffFile.delete();
                continue;
            }

            ConfigRepoBranch configRepoBranch = configRepo.getRepoBranch(diffFile.branch);
            if (diffFile.isNewFile) {
                newFiles.add(diffFile.fileRelativePath);
                boolean isSuccess = handleNewFile(client, diffFile, configFile, configRepo, configRepoBranch);
                if (isSuccess) {
                    System.out.printf("Diff file '%s' successfully processed.\n", diffFile.originalDiffFile.getPath());
                    diffFile.delete();
                    continue;
                }
            }

            if (newFiles.contains(diffFile.fileRelativePath))  {
                // Skip the changes diffs if relevant file was uploaded in the same iteration, wait for next iteration
                continue;
            }

            if (diffFile.isRename) {
                if (newFiles.contains(diffFile.oldRelativePath)) {
                    // If old_rel_path uploaded in the same iteration, wait for next iteration
                    continue;
                }

                Integer oldFileId = configRepoBranch.getFileId(diffFile.oldRelativePath);
                if (oldFileId == null) {
                    System.out.printf(
                        "old_file: %s was not synced for rename of %s/%s.\n",
                        diffFile.oldRelativePath, diffFile.repoPath, diffFile.fileRelativePath
                    );
                    diffFile.delete();
                    continue;
                }

                boolean isSuccess = handleFileRename(configFile, configRepo, configRepoBranch, diffFile, oldFileId);
                if (!isSuccess) {
                    System.out.printf("Diff file '%s' successfully processed.\n", diffFile.originalDiffFile.getPath());
                    // Skip this iteration
                    continue;
                }
            }

            if (!diffFile.isBinary && !diffFile.isDeleted && diffFile.diff.length() == 0) {
                System.out.printf("Empty diff found in file: %s. Removing...\n", diffFile.fileRelativePath);
                // Delete empty diff files.
                diffFile.delete();
                continue;
            }

            Integer fileId = configRepoBranch.getFileId(diffFile.fileRelativePath);
            if (fileId == null && !diffFile.isRename &&  !diffFile.isDeleted) {
                System.out.printf("File ID not found for; %s.\n", diffFile.fileRelativePath);
                continue;
            }
            if (fileId == null && diffFile.isDeleted) {
                cleanUpDeletedDiff(
                    configFile, configRepo, configRepoBranch, diffFile,
                    String.format(
                        "%s/%s/%s/%s", SHADOW_REPO, diffFile.repoPath.substring(1), diffFile.branch,
                        diffFile.fileRelativePath
                    )
                );
                diffFile.delete();
                continue;
            }

            if (diffFile.isDeleted){
                diffFile.setDiff(
                    getDiffOfDeletedFile(configFile, configRepo, configRepoBranch, diffFile)
                );
            }

            try {
                codeSyncWebSocketClient.sendDiff(diffFile, fileId, successfullyTransferred -> {
                    if (successfullyTransferred) {
                        System.out.printf("Diff file '%s' successfully processed.\n", diffFile.originalDiffFile.getPath());
                        diffFile.delete();
                    } else {
                        System.out.printf("Error while sending the file to the server: %s.\n", diffFile.fileRelativePath);
                    }
                });
            } catch (WebSocketConnectionError  error) {
                System.out.printf("Failed to connect to websocket endpoint: %s.\n", WEBSOCKET_ENDPOINT);
            }

        }
    }

    public static boolean handleNewFile(CodeSyncClient client, DiffFile diffFile, ConfigFile configFile, ConfigRepo repo, ConfigRepoBranch configRepoBranch) {
        String branch = Utils.GetGitBranch(repo.repoPath);

        String originalsFilePath = String.format(
            "%s/%s/%s/%s", ORIGINALS_REPO, repo.repoPath.substring(1), branch, diffFile.fileRelativePath
        );
        File originalsFile = new File(originalsFilePath);

        if (!originalsFile.exists()) {
            System.out.printf("Original file: %s not found.\n", originalsFilePath);
        }

        System.out.printf("Uploading new file: %s .\n", diffFile.fileRelativePath);
        try {
            Integer fileId = client.uploadFile(repo, diffFile, originalsFile);
            configRepoBranch.updateFileId(diffFile.fileRelativePath, fileId);
            try {
                configFile.publishBranchUpdate(repo, configRepoBranch);
            } catch (InvalidConfigFileError error)  {
                error.printStackTrace();
                return false;
            }
        } catch (FileInfoError error) {
            error.printStackTrace();
            return false;
        } catch (RequestError | InvalidJsonError error) {
            error.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean handleFileRename(ConfigFile configFile, ConfigRepo configRepo, ConfigRepoBranch configRepoBranch, DiffFile diffFile, Integer oldFileId) {
        String oldShadowPath = String.format(
            "%s/%s/%s/%s", SHADOW_REPO, diffFile.repoPath.substring(1), diffFile.branch, diffFile.oldRelativePath
        );
        String newShadowPath = String.format(
            "%s/%s/%s/%s", SHADOW_REPO, diffFile.repoPath.substring(1), diffFile.branch, diffFile.fileRelativePath
        );
        File oldShadowFile = new File(oldShadowPath);
        if (oldShadowFile.exists()) {
            oldShadowFile.renameTo(new File(newShadowPath));
        }
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
        String shadowPath = String.format(
            "%s/%s/%s/%s", SHADOW_REPO, diffFile.repoPath.substring(1), diffFile.branch, diffFile.fileRelativePath
        );
        File shadowFile = new File(shadowPath);
        String diff = "";

        if (!shadowFile.exists()) {
            cleanUpDeletedDiff(configFile,  configRepo, configRepoBranch, diffFile, shadowPath);
            return diff;
        }
        try {
            Map<String, Object> fileInfo = Utils.getFileInfo(shadowPath);
            if ((Boolean) fileInfo.get("isBinary")) {
                cleanUpDeletedDiff(configFile,  configRepo, configRepoBranch, diffFile, shadowPath);
                return diff;
            }
        } catch (FileInfoError error) {
            error.printStackTrace();
        }
        String shadowText = ReadFileToString.readLineByLineJava8(shadowPath);
        diff = Utils.computeDiff(shadowText, "");
        cleanUpDeletedDiff(configFile,  configRepo, configRepoBranch, diffFile, shadowPath);

        return diff;
    }

    public static void cleanUpDeletedDiff (ConfigFile configFile, ConfigRepo configRepo, ConfigRepoBranch configRepoBranch, DiffFile diffFile, String shadowPath) {
        String originalsPath = String.format(
            "%s/%s/%s/%s", ORIGINALS_REPO, diffFile.repoPath.substring(1), diffFile.branch, diffFile.fileRelativePath
        );
        String cacheFilePath = String.format(
            "%s/%s/%s/%s", DELETED_REPO, diffFile.repoPath.substring(1), diffFile.branch, diffFile.fileRelativePath
        );
        File originalsFile = new File(originalsPath);
        File cacheFile = new File(cacheFilePath);
        File shadowFile = new File(shadowPath);

        if (originalsFile.exists()) {
            originalsFile.delete();
        }
        if (cacheFile.exists()) {
            cacheFile.delete();
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
}
