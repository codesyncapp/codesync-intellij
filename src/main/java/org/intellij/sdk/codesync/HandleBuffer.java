package org.intellij.sdk.codesync;


import com.intellij.openapi.project.Project;
import kotlin.Pair;
import org.intellij.sdk.codesync.clients.CodeSyncClient;
import org.intellij.sdk.codesync.clients.CodeSyncWebSocketClient;
import org.intellij.sdk.codesync.database.migrations.MigrateRepo;
import org.intellij.sdk.codesync.database.models.Repo;
import org.intellij.sdk.codesync.database.models.RepoBranch;
import org.intellij.sdk.codesync.database.models.RepoFile;
import org.intellij.sdk.codesync.database.models.User;
import org.intellij.sdk.codesync.exceptions.*;
import org.intellij.sdk.codesync.exceptions.database.RepoBranchNotFound;
import org.intellij.sdk.codesync.exceptions.database.RepoFileNotFound;
import org.intellij.sdk.codesync.exceptions.database.UserNotFound;
import org.intellij.sdk.codesync.files.*;
import org.intellij.sdk.codesync.repoManagers.DeletedRepoManager;
import org.intellij.sdk.codesync.repoManagers.OriginalsRepoManager;
import org.intellij.sdk.codesync.repoManagers.ShadowRepoManager;
import org.intellij.sdk.codesync.state.PluginState;
import org.intellij.sdk.codesync.state.StateUtils;
import org.intellij.sdk.codesync.utils.CommonUtils;
import org.intellij.sdk.codesync.utils.FileUtils;
import org.intellij.sdk.codesync.alerts.PricingAlerts;
import org.intellij.sdk.codesync.utils.GitUtils;
import org.intellij.sdk.codesync.utils.ProjectUtils;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
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

    public static boolean shouldSkipDiffFile(DiffFile diffFile, Map<String, Repo> repoMap) {
        // Ignore repos that are being migrated.
        if (MigrateRepo.getInstance().getReposBeingMigrated().contains(diffFile.repoPath)) {
            CodeSyncLogger.info(String.format(
                "Skipping diff file: %s, repo: %s is being migrated.",
                diffFile.originalDiffFile.getPath(),
                diffFile.repoPath
            ));
            return true;
        }
        if (diffReposToIgnore.contains(diffFile.repoPath)) {
            CodeSyncLogger.info(String.format("Ignoring diff file '%s'.%n", diffFile.originalDiffFile.getPath()));
            return true;
        }

        if (!diffFile.isValid()) {
            CodeSyncLogger.info(
                    String.format("Skipping invalid diff file: %s, Diff: %s",
                        diffFile.originalDiffFile.getPath(), diffFile.contents
                    )
            );
            diffFile.delete();
            return true;
        }

        if (!repoMap.containsKey(diffFile.repoPath)) {
            CodeSyncLogger.warning(String.format(
                "Repo %s is not in config.yml.", diffFile.repoPath
            ));
            diffFile.delete();
            return true;
        }

        Repo repo = repoMap.get(diffFile.repoPath);
        try {
            if (!repo.isActive() || !repo.hasSyncedBranches()) {
                CodeSyncLogger.info(String.format(
                    "Repo %s is not active.", diffFile.repoPath
                ));
                diffFile.delete();
                return true;
            }
        } catch (SQLException e) {
            CodeSyncLogger.error(String.format(
                "Error while getting branch information for repo %s. Error: %s",
                diffFile.repoPath,
                CommonUtils.getStackTrace(e)
            ));
            // Skip this file for now, will be processed in the next iteration.
            return true;
        }
        User user;

        try {
            user = repo.getUser();
        } catch (SQLException | UserNotFound e) {
            CodeSyncLogger.error(String.format(
                "Error while getting user information for repo %s. Error: %s",
                diffFile.repoPath,
                CommonUtils.getStackTrace(e)
            ));
            // Skip this file for now, will be processed in the next iteration.
            return true;
        }

        try {
            repo.getBranch(diffFile.branch);
        } catch (SQLException e) {
            CodeSyncLogger.error(String.format(
                "Error while getting branch information for repo %s. Error: %s",
                diffFile.repoPath,
                CommonUtils.getStackTrace(e)
            ));
        } catch (RepoBranchNotFound e) {
            if (FileUtils.isStaleFile(diffFile.originalDiffFile)){
                if (PricingAlerts.getPlanLimitReached()){
                    CodeSyncLogger.error(
                        String.format(
                            "Keeping diff file: branch '%s' is not synced for repo '%s'.",
                            diffFile.branch,
                            diffFile.repoPath
                        ),
                        user.getEmail()
                    );
                } else {
                    CodeSyncLogger.error(
                        String.format(
                            "Removing diff file: branch '%s' is not synced for repo '%s'.",
                            diffFile.branch,
                            diffFile.repoPath
                        ),
                        user.getEmail()
                    );
                    diffFile.delete();
                }
            }
            return true;
        }

        PluginState globalState = StateUtils.getGlobalState();
        if (user.hasValidEmail() && globalState.isAuthenticated) {
            // If diff file is not from the active user then skip it.
            if(!Objects.equals(user.getEmail(), globalState.userEmail)) {
                return true;
            }
        } else {
            return true;
        }

        return false;
    }

    public static  DiffFile[] getDiffFiles(String path, String diffFileExtension, Map<String, Repo> repoMap)  {
        File diffFilesDirectory = new File(path);

        File[] files = diffFilesDirectory.listFiles(
            (dir, name) -> name.toLowerCase().endsWith(diffFileExtension.toLowerCase())
        );
        if (files != null)  {
            return Arrays.stream(files)
                .map(DiffFile::new)
                .filter(diffFile -> !shouldSkipDiffFile(diffFile, repoMap))
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
                    CodeSyncLogger.error(String.format(
                        "HandleBuffer exited with error: %s", CommonUtils.getStackTrace(e)
                    ));
                    if(e.getMessage().contains("Connection failed")){
                        CodeSyncLogger.critical(CONNECTION_ERROR_MESSAGE);
                    }
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
        Map<String, Repo> repoMap = new HashMap<>();
        HashSet<String> newFiles = new HashSet<>();
        int diffsSize = 0;

        boolean canRunDaemon = ProjectUtils.canRunDaemon(
            LockFileType.HANDLE_BUFFER_LOCK,
            DIFFS_DAEMON_LOCK_KEY,
            project.getName()
        );

        if (!canRunDaemon) {
            return;
        }

        // Abort if account has been deactivated.
        if (StateUtils.getGlobalState().isAccountDeactivated) {
            diffFilesBeingProcessed.clear();
            return;
        }

        CodeSyncClient codeSyncClient = new CodeSyncClient();
        if (!codeSyncClient.isServerUp()) {
            diffFilesBeingProcessed.clear();
            return;
        }

        try {
            ArrayList<Repo> repos = Repo.getTable().findAll();
            for (Repo repo : repos) {
                repoMap.put(repo.getPath(), repo);
            }
        } catch (SQLException e) {
            CodeSyncLogger.error(
                String.format("Error while fetching repos from the database: %s", CommonUtils.getStackTrace(e))
            );
            diffFilesBeingProcessed.clear();
            return;
        }

        // Get the list of diffs and shuffle, shuffling is needed to make sure all repos get a chance for processing.
        DiffFile[] diffFiles = getDiffFiles(DIFFS_REPO, ".yml", repoMap);
        List<DiffFile> diffFilesList = Arrays.asList(diffFiles);
        Collections.shuffle(diffFilesList);
        diffFilesList.toArray(diffFiles);

        ArrayList<Pair<Integer, DiffFile>> diffsToSend = new ArrayList<>();

        // We only process one repo per handleBuffer call.
        Repo currentRepo = null;

        if (diffFiles.length == 0) {
            return;
        }

        CodeSyncClient client = new CodeSyncClient();

        diffFiles = Arrays.copyOfRange(
            diffFiles, 0, diffFiles.length >= DIFFS_PER_ITERATION ? DIFFS_PER_ITERATION : diffFiles.length
        );

        CodeSyncLogger.logConsoleMessage(String.format("Processing %d diff files.", diffFiles.length));
        for (final DiffFile diffFile : diffFiles) {
            if (currentRepo == null) {
                try {
                    currentRepo = Repo.getTable().find(diffFile.repoPath);
                } catch (SQLException e) {
                    CodeSyncLogger.error(
                        String.format("Error while fetching repo from the database: %s", CommonUtils.getStackTrace(e))
                    );
                    continue;
                }
            }

            if (!currentRepo.getPath().equals(diffFile.repoPath)) {
                // We only process one repo per handleBuffer call.
                continue;
            }

            if (diffFilesBeingProcessed.contains(diffFile.originalDiffFile.getPath())) {
                System.out.printf("Skipping diff file: %s.\n", diffFile.originalDiffFile.getPath());
                // Skip this file.
                continue;
            }

            if(diffFile.diff != null)
                diffsSize += diffFile.diff.length();

            if(diffsSize > DIFF_SIZE_LIMIT){
                CodeSyncLogger.info("Limit reached for diff batch size, remaining diffs will be processed in next request.");
                break;
            }

            diffFilesBeingProcessed.add(diffFile.originalDiffFile.getPath());

            if (!repoMap.containsKey(diffFile.repoPath)) {
                CodeSyncLogger.error(String.format("Repo `%s` is in buffer but not in configFile.yml.\n", diffFile.repoPath));
                diffFilesBeingProcessed.remove(diffFile.originalDiffFile.getPath());
                continue;
            }

            Repo repo = repoMap.get(diffFile.repoPath);
            User user;
            try {
                user = repo.getUser();
            } catch (SQLException e) {
                CodeSyncLogger.error(
                    String.format("Error while fetching user from the database: %s", CommonUtils.getStackTrace(e))
                );
                diffFilesBeingProcessed.remove(diffFile.originalDiffFile.getPath());
                continue;
            } catch (UserNotFound e) {
                CodeSyncLogger.error(String.format("User not found in the database: %s", CommonUtils.getStackTrace(e)));
                diffFilesBeingProcessed.remove(diffFile.originalDiffFile.getPath());
                continue;
            }

            String accessToken = user.getAccessToken();

            if (accessToken == null) {
                CodeSyncLogger.critical(String.format(
                        "Access token for user '%s' not present so skipping diff file '%s'.",
                        user.getEmail(), diffFile.originalDiffFile.getPath()
                    ), user.getEmail()
                );
                diffFilesBeingProcessed.remove(diffFile.originalDiffFile.getPath());
                diffReposToIgnore.add(diffFile.repoPath);
                continue;
            } else {
                diffReposToIgnore.remove(diffFile.repoPath);
            }

            if (FileUtils.shouldIgnoreFile(diffFile.fileRelativePath, diffFile.repoPath)) {
                diffFile.delete();
                diffFilesBeingProcessed.remove(diffFile.originalDiffFile.getPath());
                continue;
            }
            RepoBranch repoBranch;
            try {
                repoBranch = repo.getBranch(diffFile.branch);
            } catch (SQLException e) {
                CodeSyncLogger.error(
                    String.format("Error while fetching branch from the database: %s", CommonUtils.getStackTrace(e))
                );
                diffFilesBeingProcessed.remove(diffFile.originalDiffFile.getPath());
                continue;
            } catch (RepoBranchNotFound e) {
                // this should never happen as we are already skipping/deleting diff files that satisfy above conditions
                // inside `getDiffFiles`. Adding a log here to make sure that this is the case.
                CodeSyncLogger.warning(
                    String.format(
                        "[HandleBuffer] Branch: `%s` is not synced for Repo `%s`.",
                        diffFile.branch,
                        diffFile.repoPath
                    )
                );
                diffFilesBeingProcessed.remove(diffFile.originalDiffFile.getPath());
                continue;
            }

            if (diffFile.isNewFile) {
                newFiles.add(diffFile.fileRelativePath);
                boolean isSuccess = handleNewFile(client, accessToken, diffFile, repo, repoBranch);
                if (isSuccess) {
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
                RepoFile repoFile;
                try {
                    repoFile = repoBranch.getFile(diffFile.oldRelativePath);
                } catch (SQLException e) {
                    CodeSyncLogger.error(
                        String.format("Error while fetching file from the database: %s", CommonUtils.getStackTrace(e))
                    );
                    diffFilesBeingProcessed.remove(diffFile.originalDiffFile.getPath());
                    continue;
                } catch (RepoFileNotFound e) {
                    CodeSyncLogger.warning(String.format(
                        "File: `%s` not found in the database for repo `%s` branch `%s`.",
                        diffFile.oldRelativePath, diffFile.repoPath, diffFile.branch
                    ));
                    diffFilesBeingProcessed.remove(diffFile.originalDiffFile.getPath());
                    diffFile.delete();
                    continue;
                }

                Integer oldFileId = repoFile.getServerFileId();
                if (oldFileId == null) {
                    CodeSyncLogger.warning(String.format("old_file: %s was not synced for rename of %s/%s.\n",
                            diffFile.oldRelativePath, diffFile.repoPath, diffFile.fileRelativePath
                    ));
                    diffFile.delete();
                    diffFilesBeingProcessed.remove(diffFile.originalDiffFile.getPath());
                    continue;
                }

                boolean isSuccess = handleFileRename(repoBranch, diffFile, oldFileId);
                if (!isSuccess) {
                    CodeSyncLogger.warning(String.format("Diff file '%s' could not be processed.\n", diffFile.originalDiffFile.getPath()));
                    diffFilesBeingProcessed.remove(diffFile.originalDiffFile.getPath());
                    // Skip this iteration
                    continue;
                }
            }

            if (!diffFile.isBinary && !diffFile.isDeleted && diffFile.isEmptyDiff()) {
                CodeSyncLogger.warning(String.format("Empty diff found in file: %s. Removing...\n", diffFile.fileRelativePath));
                diffFilesBeingProcessed.remove(diffFile.originalDiffFile.getPath());
                // Delete empty diff files.
                diffFile.delete();
                continue;
            }

            RepoFile repoFile;
            try {
                repoFile = repoBranch.getFile(diffFile.fileRelativePath);
            } catch (SQLException e) {
                CodeSyncLogger.error(
                    String.format("Error while fetching file from the database: %s", CommonUtils.getStackTrace(e))
                );
                diffFilesBeingProcessed.remove(diffFile.originalDiffFile.getPath());
                continue;
            } catch (RepoFileNotFound e) {
                if (diffFile.isDeleted) {
                    ShadowRepoManager shadowRepoManager = new ShadowRepoManager(diffFile.repoPath, diffFile.branch);
                    cleanUpDeletedDiff(
                        repoBranch, diffFile,
                        shadowRepoManager.getFilePath(diffFile.fileRelativePath)
                    );
                    diffFile.delete();
                    diffFilesBeingProcessed.remove(diffFile.originalDiffFile.getPath());
                    continue;
                }
                if (diffFile.isRename) {
                    forceUploadNullFile(client, accessToken, diffFile, repo, repoBranch);
                    diffFilesBeingProcessed.remove(diffFile.originalDiffFile.getPath());
                    continue;
                }
                Path filePath = Paths.get(diffFile.repoPath, diffFile.fileRelativePath);

                // Delete the diff file if actual file whose updates are in the diff file is now non-existent.
                if (!filePath.toFile().exists()) {
                    diffFile.delete();
                } else {
                    // If file exists, then we need to upload this file to the server.
                    forceUploadNullFile(client, accessToken, diffFile, repo, repoBranch);
                }

                diffFilesBeingProcessed.remove(diffFile.originalDiffFile.getPath());
                continue;

            }

            if (diffFile.isDeleted) {
                diffFile.setDiff(
                    getDiffOfDeletedFile(repoBranch, diffFile)
                );
            }
            diffsToSend.add(new Pair<>(repoFile.getServerFileId(), diffFile));
        }

        if (diffsToSend.isEmpty()) {
            return;
        }

        if (!currentRepo.isActive()) {
            CodeSyncLogger.info("Repo is disconnected so, skipping the diffs.");
            return;
        }

        // Send Diffs in a single request.
        User user;
        try {
            user = currentRepo.getUser();
        } catch (SQLException e) {
            CodeSyncLogger.error(
                String.format("Error while fetching user from the database: %s", CommonUtils.getStackTrace(e))
            );
            return;
        } catch (UserNotFound e) {
            CodeSyncLogger.warning(String.format(
                    "User with id '%s' not present in the database so skipping diffs for repo '%s'.",
                    currentRepo.getUserId(), currentRepo
                )
            );
            return;
        }
        if (user.getAccessToken() ==  null) {
            CodeSyncLogger.warning(String.format(
                    "Access token for user '%s' not present so skipping diffs for repo '%s'.",
                    user.getEmail(), currentRepo
                )
            );
            return;
        } else {
            diffReposToIgnore.remove(currentRepo.getPath());
        }

        CodeSyncWebSocketClient codeSyncWebSocketClient = client.getWebSocketClient(user.getAccessToken());
        codeSyncWebSocketClient.connect(isConnected -> {
            if (isConnected) {
                try {
                    codeSyncWebSocketClient.sendDiffs(diffsToSend, (successfullyTransferred, diffFilePath) -> {
                        diffFilesBeingProcessed.remove(diffFilePath);
                        if (!successfullyTransferred) {
                            CodeSyncLogger.error("Error while sending the diff files to the server.", user.getEmail());
                            return;
                        }
                        DiffFile.delete(diffFilePath);
                    });
                } catch (WebSocketConnectionError error) {
                    diffFilesBeingProcessed.clear();
                    CodeSyncLogger.critical(String.format("Connection error while sending diff to the server at %s.\n", WEBSOCKET_ENDPOINT), user.getEmail());
                }
            } else {
                diffFilesBeingProcessed.clear();
                CodeSyncLogger.error(String.format("Failed to connect to websocket endpoint: %s.\n", WEBSOCKET_ENDPOINT), user.getEmail());
            }
        });
    }

    public static boolean handleNewFile(CodeSyncClient client, String accessToken, DiffFile diffFile, Repo repo, RepoBranch repoBranch) {
        String branchName = GitUtils.getBranchName(repo.getPath());
        OriginalsRepoManager originalsRepoManager = new OriginalsRepoManager(repo.getPath(), branchName);

        Path originalsFilePath = originalsRepoManager.getFilePath(diffFile.fileRelativePath);
        File originalsFile = originalsFilePath.toFile();

        if (!originalsFile.exists()) {
            CodeSyncLogger.error(String.format("Could not find the original file: %s not found.", originalsFilePath));

            // We can not process this file yet, so we need to remove the diff and mark this a successful upload.
            return true;
        }

        // If plan limit is reached then do not process new files.
        if (PricingAlerts.getPlanLimitReached()) {
            return false;
        }

        CodeSyncLogger.info("Uploading new file: %s .\n", diffFile.fileRelativePath);
        try {
            Integer fileId = client.uploadFile(accessToken, repo, diffFile, originalsFile);
            repoBranch.updateFileId(diffFile.fileRelativePath, fileId);
        } catch (FileInfoError error) {
            CodeSyncLogger.error(
                String.format("Error while getting file information. \n%s", CommonUtils.getStackTrace(error))
            );
            return false;
        } catch (RequestError | InvalidJsonError error) {
            CodeSyncLogger.error(
                String.format(
                    "Error while uploading a new file '%s'. \n%s",
                    diffFile.fileRelativePath,
                    CommonUtils.getStackTrace(error)
                )
            );
            return false;
        } catch (InvalidUsage e){
            CodeSyncLogger.error(
                String.format(
                    "Invalid usage error while uploading a new file '%s'. Error: %s",
                    diffFile.fileRelativePath,
                    CommonUtils.getStackTrace(e)
                )
            );
            // We can not process this file because so we need to remove the diff and mark this a successful upload.
            return true;
        } catch (SQLException e) {
            CodeSyncLogger.error(
                String.format("Error while updating file id in the database. Error: %s", CommonUtils.getStackTrace(e))
            );
            return false;
        }
        return true;
    }

    public static boolean handleFileRename(RepoBranch repoBranch, DiffFile diffFile, Integer oldFileId) {
        ShadowRepoManager shadowRepoManager = new ShadowRepoManager(diffFile.repoPath, diffFile.branch);
        shadowRepoManager.renameFile(diffFile.oldRelativePath, diffFile.fileRelativePath);

        try {
            repoBranch.updateFileId(diffFile.fileRelativePath, oldFileId);
            return true;
        } catch (SQLException e) {
            CodeSyncLogger.error(
                String.format("Error while updating file id in the database. Error: %s", CommonUtils.getStackTrace(e))
            );
            return false;
        }
    }

    public static String getDiffOfDeletedFile(RepoBranch repoBranch, DiffFile diffFile ) {
        ShadowRepoManager shadowRepoManager = new ShadowRepoManager(diffFile.repoPath, diffFile.branch);
        Path shadowPath = shadowRepoManager.getFilePath(diffFile.fileRelativePath);

        File shadowFile = shadowPath.toFile();
        String diff = "";

        if (!shadowFile.exists()) {
            cleanUpDeletedDiff(repoBranch, diffFile, shadowPath);
            return diff;
        }
        try {
            Map<String, Object> fileInfo = FileUtils.getFileInfo(shadowPath.toString());
            if ((Boolean) fileInfo.get("isBinary")) {
                cleanUpDeletedDiff(repoBranch, diffFile, shadowPath);
                return diff;
            }
        } catch (FileInfoError error) {
            CodeSyncLogger.error(
                String.format("Error while getting file information. \nError: %s", CommonUtils.getStackTrace(error))
            );
        }
        String shadowText = FileUtils.readFileToString(shadowPath.toFile());
        diff = CommonUtils.computeDiff(shadowText, "");
        cleanUpDeletedDiff(repoBranch, diffFile, shadowPath);

        return diff;
    }

    public static void cleanUpDeletedDiff (RepoBranch repoBranch, DiffFile diffFile, Path shadowPath) {
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
            repoBranch.removeFile(diffFile.fileRelativePath);
        } catch (SQLException e) {
            CodeSyncLogger.error(
                String.format("Error while removing file id in the database. Error: %s", CommonUtils.getStackTrace(e))
            );
        }
    }

    public static boolean forceUploadNullFile(CodeSyncClient client, String accessToken, DiffFile diffFile, Repo repo, RepoBranch repoBranch) {
        OriginalsRepoManager originalsRepoManager = new OriginalsRepoManager(diffFile.repoPath, diffFile.branch);
        File originalsFile = originalsRepoManager.getFilePath(diffFile.fileRelativePath).toFile();

        // Copy the file to originals directory if it is not already in there.
        if (!originalsFile.exists()) {
            Path filePath = Paths.get(diffFile.repoPath, diffFile.fileRelativePath);
            originalsRepoManager.copyFiles(new String[]{filePath.toString()});
        }
        return handleNewFile(client, accessToken, diffFile, repo, repoBranch);
    }
}
