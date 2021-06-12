package org.intellij.sdk.codesync;

import org.intellij.sdk.codesync.clients.CodeSyncClient;
import org.intellij.sdk.codesync.clients.WebSocketClient;
import org.intellij.sdk.codesync.exceptions.*;
import org.intellij.sdk.codesync.files.ConfigFile;
import org.intellij.sdk.codesync.files.ConfigRepo;
import org.intellij.sdk.codesync.files.ConfigRepoBranch;
import org.intellij.sdk.codesync.files.DiffFile;

import java.io.*;
import java.util.Arrays;
import java.util.HashSet;

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

    public static void handleBuffer() {
        ConfigFile config;
        HashSet<String> newFiles = new HashSet<>();

        DiffFile[] diffFiles = getDiffFiles(DIFFS_REPO, ".yml");
        if (diffFiles.length == 0) {
            return;
        }

        CodeSyncClient client =new CodeSyncClient();
        if (!client.isServerUp()) {
            return;
        }

        diffFiles = Arrays.copyOfRange(
                diffFiles, 0, diffFiles.length >= DIFFS_PER_ITERATION ? DIFFS_PER_ITERATION : diffFiles.length
        );

        try {
            config = new ConfigFile(CONFIG_PATH);
        } catch (InvalidConfigFile error) {
            System.out.printf("Config file error, %s%n", error.getMessage());
            return;
        }

        for (DiffFile diffFile: diffFiles) {
            if (!diffFile.isValid()) {
                String filePath =  String.format("%s/%s", diffFile.repoPath, diffFile.fileRelativePath );
                System.out.printf("Skipping invalid diff file: %s. data: %s", filePath, diffFile.diff);

                diffFile.delete();
                continue;
            }

            if (!config.repos.containsKey(diffFile.repoPath)) {
                System.out.printf("Repo `%s` is in buffer.yml but not in config.yml", diffFile.repoPath);
                continue;
            }

            ConfigRepo repo = config.getRepo(diffFile.repoPath);
            if (!repo.branches.containsKey(diffFile.branch))  {
                System.out.printf("Branch: `%s` is not synced for Repo `%s`", diffFile.branch, diffFile.repoPath);
                continue;
            }

            WebSocketClient webSocketClient;
            try {
                webSocketClient = client.connectWebSocket();
            } catch (WebSocketConnectionError error) {
                System.out.printf("Failed to connect to websocket endpoint: %s", WEBSOCKET_ENDPOINT);
                continue;
            }

            Boolean authenticated = webSocketClient.authenticate(repo.token);
            if (!authenticated) {
                System.out.println("Could noy authenticate user with token");
                continue;
            }

            if(diffFile.isDirRename) {
                handleDirRename(diffFile);
                diffFile.delete();
                continue;
            }

            if (Utils.shouldIgnoreFile(diffFile.fileRelativePath, diffFile.repoPath)) {
                diffFile.delete();
                continue;
            }

            if (diffFile.isNewFile) {
                newFiles.add(diffFile.fileRelativePath);
                handleNewFile(repo, diffFile);
                diffFile.delete();
            }

            if (newFiles.contains(diffFile.fileRelativePath))  {
                // Skip the changes diffs if relevant file was uploaded in the same iteration, wait for next iteration
                continue;
            }

            ConfigRepoBranch configRepoBranch = repo.getRepoBranch(diffFile.branch);
            if (diffFile.isRename) {
                if (newFiles.contains(diffFile.oldRelativePath)) {
                    // If old_rel_path uploaded in the same iteration, wait for next iteration
                    continue;
                }

                Integer oldFileId = configRepoBranch.getFileId(diffFile.oldRelativePath);
                // TODO: old_file_id = config_files.pop(old_rel_path, "") maybe we need to update config file here.
                if (oldFileId == null) {
                    System.out.printf(
                            "old_file: %s was not synced for rename of %s/%s",
                            diffFile.oldRelativePath, diffFile.repoPath, diffFile.fileRelativePath
                    );
                    diffFile.delete();
                }

                handleFileRename(configRepoBranch, diffFile, oldFileId);

            }

            if (!diffFile.isBinary && !diffFile.isDeleted && diffFile.diff.length() == 0) {
                System.out.printf("Empty diff found in file: %s. Removing...", diffFile.fileRelativePath);
                // Delete empty diff files.
                diffFile.delete();
                continue;
            }

            Integer fileId = configRepoBranch.getFileId(diffFile.fileRelativePath);
            if (fileId == null && !diffFile.isRename &&  !diffFile.isDeleted) {
                System.out.printf("File ID not found for; %s", diffFile.fileRelativePath);
                continue;
            }
            if (fileId == null && diffFile.isDeleted) {
                if (!Utils.isDirectoryDelete(diffFile.repoPath, diffFile.branch, diffFile.fileRelativePath)) {
                    System.out.printf("is_deleted non-synced file found: %s/%s", diffFile.repoPath, diffFile.fileRelativePath);
                }
                diffFile.delete();

                continue;
            }

            if (diffFile.isDeleted){
                // TODO: See of this can be moved to DiffFile.
                diffFile.setDiff(getDiffOfDeletedFile(repo, diffFile));
            }

            Boolean is_success = webSocketClient.sendDiff(diffFile);
            if (!is_success) {
                continue;
            }
            diffFile.delete();

            System.out.println("Testings");
        }

    }

    public static void handleDirRename(DiffFile diffFile) {

    }

    public static void handleNewFile(ConfigRepo repo, DiffFile diffFile) {

    }

    public static void handleFileRename(ConfigRepoBranch configRepoBranch, DiffFile diffFile, Integer oldFileId) {

    }

    public static String getDiffOfDeletedFile(ConfigRepo repo, DiffFile diffFile ) {
        return "";
    }
}
