package org.intellij.sdk.codesync.codeSyncSetup;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import kotlin.Pair;
import org.intellij.sdk.codesync.CodeSyncLogger;
import org.intellij.sdk.codesync.NotificationManager;
import org.intellij.sdk.codesync.auth.CodeSyncAuthServer;
import org.intellij.sdk.codesync.clients.CodeSyncClient;
import org.intellij.sdk.codesync.commands.ReloadStateCommand;
import org.intellij.sdk.codesync.commands.ResumeCodeSyncCommand;
import org.intellij.sdk.codesync.database.models.Repo;
import org.intellij.sdk.codesync.database.models.RepoBranch;
import org.intellij.sdk.codesync.database.models.RepoFile;
import org.intellij.sdk.codesync.database.models.User;
import org.intellij.sdk.codesync.enums.RepoState;
import org.intellij.sdk.codesync.exceptions.*;
import org.intellij.sdk.codesync.exceptions.database.UserNotFound;
import org.intellij.sdk.codesync.exceptions.file.UserFileError;
import org.intellij.sdk.codesync.exceptions.network.RepoUpdateError;
import org.intellij.sdk.codesync.exceptions.network.ServerConnectionError;
import org.intellij.sdk.codesync.exceptions.repo.RepoNotActive;
import org.intellij.sdk.codesync.files.*;
import org.intellij.sdk.codesync.state.PluginState;
import org.intellij.sdk.codesync.state.RepoStatus;
import org.intellij.sdk.codesync.state.StateUtils;
import org.intellij.sdk.codesync.ui.dialogs.RepoPublicPrivateDialog;
import org.intellij.sdk.codesync.ui.messages.CodeSyncMessages;
import org.intellij.sdk.codesync.ui.progress.CodeSyncProgressIndicator;
import org.intellij.sdk.codesync.ui.progress.InitRepoMilestones;
import org.intellij.sdk.codesync.repoManagers.OriginalsRepoManager;
import org.intellij.sdk.codesync.repoManagers.ShadowRepoManager;
import org.intellij.sdk.codesync.utils.CommonUtils;
import org.intellij.sdk.codesync.utils.CodeSyncDateUtils;
import org.intellij.sdk.codesync.utils.FileUtils;
import org.intellij.sdk.codesync.alerts.PricingAlerts;
import org.intellij.sdk.codesync.utils.GitUtils;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.intellij.sdk.codesync.Constants.*;

public class CodeSyncSetup {
    public static final Set<String> reposBeingSynced = new HashSet<>();

    /*
    This can be called to disconnect an already connected repo.
     */
    public static void disconnectRepo(Project project, String repoPath, String repoName) throws RepoNotActive, UserFileError, ServerConnectionError, RepoUpdateError, SQLException, UserNotFound {
        // Show confirmation message.
        boolean shouldDisconnect = CodeSyncMessages.showYesNoMessage(
                "Are you sure?",
                Notification.REPO_UNSYNC_CONFIRMATION,
                project
        );

        // User confirmed to disconnect the repo.
        if (shouldDisconnect) {
            Repo repo = Repo.getTable().find(repoPath);
            if (repo == null || !repo.isActive()) {
                throw new RepoNotActive(String.format("Repo '%s' is not active and can not be disconnected.", repoName));
            }
            User user = repo.getUser();

            if (user == null) {
                throw new UserNotFound("User is missing in the database.");
            }

            JSONObject payload = new JSONObject();
            payload.put("is_in_sync", false);
            updateRepo(repo.getServerRepoId(), user.getEmail(), repoName, payload);

            repo.setState(RepoState.DISCONNECTED);
            repo.save();

            StateUtils.reloadState(project);
            NotificationManager.getInstance().notifyInformation(Notification.REPO_UNSYNCED, project);
        }
    }

    public static void reconnectRepo(Project project, String repoPath, String repoName) throws ServerConnectionError, RepoUpdateError, SQLException, UserNotFound {
        Repo repo = Repo.getTable().find(repoPath);
        User user = repo != null ? repo.getUser() : null;

        if (repo == null || repo.getUserId() == null || repo.getServerRepoId() == null || !repo.hasSyncedBranches()) {
            // If repo is not being synced then start the sync from scratch.
            setupCodeSyncRepoAsync(project, repoPath, repoName, true, false);
        } else {
            if (user == null) {
                throw new UserNotFound("User is missing in the database.");
            }
            JSONObject payload = new JSONObject();
            payload.put("is_in_sync", true);
            updateRepo(repo.getServerRepoId(), user.getEmail(), repoName, payload);

            repo.setState(RepoState.SYNCED);
            repo.save();

            StateUtils.reloadState(project);
            NotificationManager.getInstance().notifyInformation(Notification.REPO_RECONNECTED, project);
        }
    }

    public static void updateRepo(Integer repoId, String userEmail, String repoName, JSONObject payload) throws SQLiteDataError, ServerConnectionError, RepoUpdateError {
        String accessToken = User.getTable().getAccessToken(userEmail);
        if (accessToken == null) {
            throw new SQLiteDataError(
                String.format(
                    "Repo '%s' could not be updated because user access token is missing from SQLite database.",
                    repoName
                )
            );
        }
        CodeSyncClient codeSyncClient = new CodeSyncClient();

        if (!codeSyncClient.isServerUp()) {
            throw new ServerConnectionError(
                String.format(
                    "Repo '%s' could not be updated because we could not connect to the CodeSync servers.",
                    repoName
                )
            );
        }

        JSONObject response = codeSyncClient.updateRepo(accessToken, repoId, payload);
        if (response.containsKey("error")) {
            throw new RepoUpdateError(
                String.format(
                    "Repo '%s' could not be updated server returned an error response. Error: %s",
                    repoName,
                    response.get("error")
                )
            );
        }
    }

    public static void reconnectRepoAsync(Project project, String repoPath, String repoName) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "CodeSync repo management"){
            public void run(@NotNull ProgressIndicator progressIndicator) {
                boolean shouldReconnect = CodeSyncMessages.showYesNoMessage(
                    "Do you want to reconnect this repo?",
                    String.format("'%s' has been disconnected!", repoName),
                    project
                );

                if (shouldReconnect) {
                    try {
                        CodeSyncSetup.reconnectRepo(project, repoPath, repoName);
                    } catch (ServerConnectionError | RepoUpdateError | SQLException | UserNotFound error) {
                        NotificationManager.getInstance().notifyError(Notification.REPO_RECONNECT_FAILED, project);
                        NotificationManager.getInstance().notifyError(error.getMessage(), project);
                        CodeSyncLogger.critical(
                            String.format("Could not reconnect the repo. Error: %s", error.getMessage())
                        );
                    }
                }
            }
        });
    }

    public static void setupCodeSyncRepoAsync(Project project, String repoPath, String repoName, boolean skipSyncPrompt, boolean isSyncingBranch) {
        // Update state to show repo sync is in progress.
        StateUtils.updateRepoStatus(repoPath, RepoStatus.SYNC_IN_PROGRESS);
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Initializing repo"){
            public void run(@NotNull ProgressIndicator progressIndicator) {
                progressIndicator.setIndeterminate(false);

                CodeSyncProgressIndicator codeSyncProgressIndicator = new CodeSyncProgressIndicator(progressIndicator);

                // Set the progress bar percentage and text
                codeSyncProgressIndicator.setMileStone(InitRepoMilestones.START);
                setupCodeSyncRepo(project, repoPath, repoName, codeSyncProgressIndicator, skipSyncPrompt, isSyncingBranch);

                // Finished
                codeSyncProgressIndicator.setMileStone(InitRepoMilestones.END);
                
                // Reload state according to the current state.
                // We are reloading here instead of marking it as Done because we want to make sure the state is correct.
                StateUtils.reloadState(project);
            }
        });
    }

    public static void setupCodeSyncRepo(Project project, String repoPath, String repoName, CodeSyncProgressIndicator codeSyncProgressIndicator, boolean skipSyncPrompt, boolean isSyncingBranch) {
        try {
            Repo repo = Repo.getTable().find(repoPath);

            if (repo == null || !repo.isActive() || !repo.hasSyncedBranches() || isSyncingBranch) {
                String branchName = GitUtils.getBranchName(repoPath);
                codeSyncProgressIndicator.setMileStone(InitRepoMilestones.CHECK_USER_ACCESS);
                boolean hasAccessToken = checkUserAccess(project, repoPath, repoName, branchName, skipSyncPrompt, isSyncingBranch);
                if (!hasAccessToken) {
                    NotificationManager.getInstance().notifyInformation(Notification.LOGIN_REQUIRED_FOR_SYNC_MESSAGE, project);
                    return;
                }

                if (skipSyncPrompt) {
                    syncRepo(repoPath, repoName, branchName, project, codeSyncProgressIndicator, isSyncingBranch);
                    return;
                }

                // This is kind of a hack, but for some reason this method is called more than once during user is
                // shown a popup to confirm repo syncing. and during the popup is open we do not want to show
                // the message "Notification.REPO_SYNC_IN_PROGRESS_MESSAGE" (from the else statement.)
                // That is why this boolean is placed here.
                boolean shouldSyncRepo = false;
                // Do not ask user to sync repo, if it is already in progress.
                if (!reposBeingSynced.contains(repoPath)) {
                    reposBeingSynced.add(repoPath);
                    shouldSyncRepo = CodeSyncMessages.showYesNoMessage(
                            "Do you want to enable syncing of this repo?",
                            String.format("'%s' Is not being synced!", repoName),
                            project
                    );

                    if (shouldSyncRepo) {
                        syncRepo(repoPath, repoName, branchName, project, codeSyncProgressIndicator, false);
                        reposBeingSynced.remove(repoPath);
                    }
                }
            } else {
                NotificationManager.getInstance().notifyInformation(
                        String.format(Notification.REPO_IN_SYNC_MESSAGE, repoName),
                        project
                );
            }
        } catch (SQLException error) {
            CodeSyncLogger.critical(
                String.format(
                    "Error while getting repo from database. Error: %s",
                    CommonUtils.getStackTrace(error)
                )
            );
        }
    }

    public static void createSystemDirectories() {
        // Create system folders
        String[] systemFolders = {
            CODESYNC_ROOT, DIFFS_REPO, ORIGINALS_REPO, SHADOW_REPO, DELETED_REPO, LOCKS_FILE_DIR, S3_UPLOAD_QUEUE_DIR
        };
        for (String systemFolder : systemFolders) {
            File folder = new File(systemFolder);
            folder.mkdirs();
        }
        String[] systemFilePaths = {
                CONFIG_PATH, USER_FILE_PATH, SEQUENCE_TOKEN_FILE_PATH, PROJECT_LOCK_FILE, HANDLE_BUFFER_LOCK_FILE,
                POPULATE_BUFFER_LOCK_FILE, ALERTS_FILE_PATH
        };
        for (String systemFilePath: systemFilePaths) {
            File systemFile = new File(systemFilePath);

            if (!systemFile.exists()) {
                CodeSyncYmlFile.createFile(systemFilePath);
            }
        }
    }

    /*
    Validate given access token, return true if valid, false otherwise.
    The things this function will validate are listed below, if any of the following is not satisfied token is
    considered invalid.
        1. Validate that code sync server is up.
        2. Validate access token is not expired by sending a request to the server.
        3. Validate that user's plan has not been exhausted.
    */
    public static boolean validateAccessToken(String accessToken) throws InvalidAccessTokenError {
        CodeSyncClient codeSyncClient = new CodeSyncClient();
        if (!codeSyncClient.isServerUp()) {
            NotificationManager.getInstance().notifyError(Notification.SERVICE_NOT_AVAILABLE);
            return false;
        }

        try {
            Pair<Boolean, String> userPair = codeSyncClient.getUser(accessToken);
            Boolean isTokenValid = userPair.component1();

            // If token is not valid then need to authenticate again.
            if (!isTokenValid) {
                throw new InvalidAccessTokenError("Invalid access token, needs to be refreshed.");
            }

            // User has access to repo sync, and can proceed.
            return true;
        } catch (RequestError e) {
            // Unknown token status, need to authenticate again.
            return false;
        }
    }

    /*
    This will check if user is logged in or not and will display
    login prompt if user is not authenticated or has a missing access token.
     */
    public static void checkUserAuthStatus(Project project) {
        String accessToken = User.getTable().getAccessToken();
        if (accessToken != null) {
            // If user has an access token then return here.
            // Note: We do not check the validity of access token here,
            // that part is handled when user tries to sync a repo.
            return;
        }
        CodeSyncLogger.debug("[INTELLIJ_AUTH]: Login prompt displayed to the user.");
        boolean shouldSignup = CodeSyncMessages.showYesNoMessage(
            "Do you want to login or sign up to use CodeSync?",
            "To stream your code to the cloud, you'll need to authenticate.",
            project
        );

        // If user said no to signup request the return here.
        if (shouldSignup) {
            try {
                CodeSyncAuthServer server =  CodeSyncAuthServer.getInstance();
                CodeSyncAuthServer.registerPostAuthCommand(new ReloadStateCommand(project));
                BrowserUtil.browse(server.getLoginURL());
                CodeSyncLogger.debug("[INTELLIJ_AUTH]: User redirected to the login page.");
            } catch (Exception exc) {
                CodeSyncLogger.critical(String.format(
                    "[INTELLIJ_AUTH]: An error occurred during user authentication. Error: %s",
                    CommonUtils.getStackTrace(exc)
                ));
                NotificationManager.getInstance().notifyError("There was a problem with login, please try again later.", project);
            }
        }
    }

    /*
    Check if the user has proper access for repo initialization, or not.

    This will also trigger the authentication flow if user does not have access token setup.
     */
    public static boolean checkUserAccess(Project project, String repoPath, String repoName, String branchName, boolean skipSyncPrompt, boolean isSyncingBranch) {
        String accessToken = User.getTable().getAccessToken();

        // User already has access token, no need to proceed.
        if (accessToken != null) {
            try {
                return validateAccessToken(accessToken);
            } catch (InvalidAccessTokenError invalidAccessTokenError) {
                // no action needed, user will be prompted for authenticated later by default.
            }
        }

        CodeSyncLogger.debug("[INTELLIJ_AUTH]: Login prompt displayed to the user.");

        boolean shouldSignup = CodeSyncMessages.showYesNoMessage(
            "Do you want to login or sign up to use CodeSync?",
            "To stream your code to the cloud, you'll need to authenticate.",
            project
        );

        // If user said no to signup request the return here.
        if (!shouldSignup) {
            // TODO: We should add logic here to not ask the user again next time he opens the project.
            CodeSyncLogger.debug("[INTELLIJ_AUTH]: User declined login prompt.");
            return false;
        }

        CodeSyncAuthServer server;
        try {
            server =  CodeSyncAuthServer.getInstance();
            CodeSyncAuthServer.registerPostAuthCommand(new ResumeCodeSyncCommand(project, repoPath, repoName, skipSyncPrompt, isSyncingBranch));
            CodeSyncAuthServer.registerPostAuthCommand(new ReloadStateCommand(project));
            BrowserUtil.browse(server.getLoginURL());

            CodeSyncLogger.debug("[INTELLIJ_AUTH]: User redirected to the login page.");
        } catch (Exception exc) {
            CodeSyncLogger.critical(String.format(
                "[INTELLIJ_AUTH]: An error occurred during user authentication. Error: %s",
                CommonUtils.getStackTrace(exc)
            ));
            NotificationManager.getInstance().notifyError("There was a problem with login, please try again later.", project);
        }

        return false;
    }

    public static void syncRepo(String repoPath, String repoName, String branchName, Project project, CodeSyncProgressIndicator codeSyncProgressIndicator, boolean isSyncingBranch) {
        // create .syncignore file.
        String syncIgnoreFilePath = createSyncIgnore(repoPath);

        codeSyncProgressIndicator.setMileStone(InitRepoMilestones.FETCH_FILES);
        String[] filePaths = FileUtils.listFiles(repoPath);

        // For some reason, syncignore file is not added to original and shadow repo.
        // We are enforcing here that syncignore is always added.
        if (syncIgnoreFilePath != null && Arrays.stream(filePaths).noneMatch(syncIgnoreFilePath::equals)) {
            filePaths = Stream.concat(Arrays.stream(filePaths), Stream.of(syncIgnoreFilePath)).toArray(String[]::new);
        }

        codeSyncProgressIndicator.setMileStone(InitRepoMilestones.COPY_FILES);
        // Copy files to shadow repo.
        ShadowRepoManager shadowRepoManager = new ShadowRepoManager(repoPath, branchName);
        shadowRepoManager.copyFiles(filePaths);

        // Copy files to originals repo.
        OriginalsRepoManager originalsRepoManager = new OriginalsRepoManager(repoPath, branchName);
        originalsRepoManager.copyFiles(filePaths);

        // Upload the repo.
        codeSyncProgressIndicator.setMileStone(InitRepoMilestones.SENDING_REPO);
        boolean wasUploadSuccessful = uploadRepo(repoPath, repoName, filePaths, project, codeSyncProgressIndicator, isSyncingBranch);

        codeSyncProgressIndicator.setMileStone(InitRepoMilestones.CLEANUP);
        if (wasUploadSuccessful) {
            if (!Objects.equals(repoPath, project.getBasePath()) && isSyncingBranch) {
                // Skip messaging as we are syncing offline branch for non-opened project.
                return;
            }

            // Show success message and update state
            if (!isSyncingBranch){
                NotificationManager.getInstance().notifyInformation(Notification.INIT_SUCCESS_MESSAGE, project);
            } else {
                NotificationManager.getInstance().notifyInformation(
                    String.format(Notification.BRANCH_INIT_SUCCESS_MESSAGE, branchName), project);
            }
            StateUtils.reloadState(project);
        } else {
            // Show failure message.
            if (!isSyncingBranch){
                NotificationManager.getInstance().notifyError(Notification.INIT_FAILURE_MESSAGE, project);
            } else {
                NotificationManager.getInstance().notifyError(
                    String.format(Notification.BRANCH_INIT_FAILURE_MESSAGE, branchName), project
                );
            }
        }
    }

    public static void uploadRepoAsync(String repoPath, String repoName, String[] filePaths, Project project, boolean isSyncingBranch){
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Initializing repo"){
            public void run(@NotNull ProgressIndicator progressIndicator) {
                progressIndicator.setIndeterminate(false);

                CodeSyncProgressIndicator codeSyncProgressIndicator = new CodeSyncProgressIndicator(progressIndicator);

                // Set the progress bar percentage and text
                codeSyncProgressIndicator.setMileStone(InitRepoMilestones.START);
                uploadRepo(repoPath, repoName, filePaths, project, codeSyncProgressIndicator, isSyncingBranch);

                // Finished
                codeSyncProgressIndicator.setMileStone(InitRepoMilestones.END);
            }
        });
    }

    /*
    upload the repo and returns status as a boolean.

    @return: true if all repo upload operations (including repo upload and S3 upload) completed successfully,
        false otherwise.
    */
    public static boolean uploadRepo(String repoPath, String repoName, String[] filePaths, Project project, CodeSyncProgressIndicator codeSyncProgressIndicator, boolean isSyncingBranch) {
        // If plan limit is reached then do not process new repos.
        if (PricingAlerts.getPlanLimitReached()) {
            return false;
        }

        String branchName = GitUtils.getBranchName(repoPath);

        Map<String, Integer> branchFiles = new HashMap<>();
        JSONObject filesData = new JSONObject();

        String[] relativeFilePaths = Arrays.stream(filePaths)
                .map(filePath -> filePath.replace(repoPath, ""))
                .map(filePath -> filePath.replaceFirst(Pattern.quote(String.valueOf(File.separatorChar)), ""))
                .toArray(String[]::new);

        for (String relativeFilePath: relativeFilePaths) {
            branchFiles.put(relativeFilePath, null);
            Map<String, Object> fileInfo;
            try {
                fileInfo = FileUtils.getFileInfo(Paths.get(repoPath, relativeFilePath).toString());
            } catch (FileInfoError error) {
                CodeSyncLogger.warning(String.format("File Info could not be found for %s", relativeFilePath));

                // Skip this file.
                continue;
            }

            JSONObject item = new JSONObject();
            item.put("is_binary",  fileInfo.get("isBinary"));
            item.put("size", fileInfo.get("size"));
            item.put("created_at", CodeSyncDateUtils.getPosixTime((String) fileInfo.get("creationTime")));
            filesData.put(relativeFilePath, item);
        }

        String accessToken = User.getTable().getAccessToken();
        if (accessToken == null) {
            // Show error message.
            if (!isSyncingBranch) {
                NotificationManager.getInstance().notifyInformation(Notification.INIT_ERROR_MESSAGE, project);
            } else {
                NotificationManager.getInstance().notifyInformation(
                    String.format(Notification.BRANCH_INIT_ERROR_MESSAGE, branchName), project
                );
            }

            // Can not proceed. This should never happen as the same check is applied at the start.
            return false;
        }
        CodeSyncClient codeSyncClient = new CodeSyncClient();
        JSONObject payload = new JSONObject();
        if (!isSyncingBranch) {
            CommonUtils.invokeAndWait(
                () -> {
                    RepoPublicPrivateDialog repoPublicPrivateDialog = new RepoPublicPrivateDialog(project);
                    boolean isPublic = repoPublicPrivateDialog.showAndGet();
                    payload.put("is_public", isPublic);

                    return isPublic;
                },
                ModalityState.defaultModalityState()
            );
        } else {
            payload.put("is_public", false);
        }

        payload.put("name", repoName);
        payload.put("branch", branchName);
        payload.put("files_data", filesData.toJSONString());
        payload.put("source", DIFF_SOURCE);
        payload.put("platform", CommonUtils.getOS());
        payload.put("commit_hash", GitUtils.getCommitHash(repoPath));

        codeSyncProgressIndicator.setMileStone(InitRepoMilestones.PROCESS_RESPONSE);
        JSONObject response = codeSyncClient.uploadRepo(accessToken, payload);

        if (response == null || response.containsKey("error")) {
            // Show error message.
            if (!isSyncingBranch) {
                NotificationManager.getInstance().notifyInformation(Notification.INIT_ERROR_MESSAGE, project);
            } else {
                NotificationManager.getInstance().notifyInformation(
                        String.format(Notification.BRANCH_INIT_ERROR_MESSAGE, branchName), project
                );
            }
            return false;
        }
        String email;
        try {
            JSONObject userObject = (JSONObject) response.get("user");
            email = (String) userObject.get("email");
            saveIamUser(
                    email,
                    (String) userObject.get("iam_access_key"),
                    (String) userObject.get("iam_secret_key")
            );
        } catch (ClassCastException err) {
            CodeSyncLogger.critical(String.format(
                "Error parsing the response of /init endpoint. Error: %s", CommonUtils.getStackTrace(err)
            ));

            return false;
        }

        int repoId;
        try {
            Map<String, Integer> filePathAndIds = new HashMap<>();
            repoId = ((Long) response.get("repo_id")).intValue();
            JSONObject filePathAndIdsObject = (JSONObject) response.get("file_path_and_id");

            if (filePathAndIdsObject == null) {
                CodeSyncLogger.critical(
                    "Invalid response from /init endpoint. Missing `file_path_and_id` key.",
                    email
                );

                return false;
            }

            codeSyncProgressIndicator.setMileStone(InitRepoMilestones.CONFIG_UPDATE);
            filePathAndIds = new ObjectMapper().readValue(filePathAndIdsObject.toJSONString(), new TypeReference<Map<String, Integer>>(){});
            // Persists repo data on the database.
            saveToDatabase(email, repoId, repoName, repoPath, branchName, filePathAndIds);
        } catch (ClassCastException | JsonProcessingException err) {
            CodeSyncLogger.critical(
                String.format(
                    "Error parsing the response of /init endpoint. Error: %s",
                    CommonUtils.getStackTrace(err)
                ),
                email
            );

            return false;
        } catch (SQLException error) {
            CodeSyncLogger.error(String.format(
                "Error saving repo data on the database. Error: %s", CommonUtils.getStackTrace(error)
            ));

            if (!isSyncingBranch) {
                NotificationManager.getInstance().notifyInformation(Notification.INIT_ERROR_MESSAGE, project);
            } else {
                NotificationManager.getInstance().notifyInformation(
                    String.format(Notification.BRANCH_INIT_ERROR_MESSAGE, branchName), project
                );
            }

            return false;
        }

        try {
            Map<String, Object> fileUrls = new HashMap<>();
            JSONObject urls = (JSONObject) response.get("urls");
            if (urls == null) {
                CodeSyncLogger.critical("Invalid response from /init endpoint. Missing `urls` key.", email);

                return false;
            }
            fileUrls = new ObjectMapper().readValue(urls.toJSONString(), new TypeReference<Map<String, Object>>(){});

            S3FileUploader s3FileUploader = new S3FileUploader(repoPath, branchName, fileUrls);
            s3FileUploader.saveURLs();

            // Trigger the task to upload the file to S3.
            CodeSyncLogger.info(String.format(
                "[S3_FILE_UPLOAD]: Processing file: %s",
                s3FileUploader.getQueueFilePath()
            ));
            S3FilesUploader.registerFileBeingProcessed(s3FileUploader.getQueueFilePath());
            s3FileUploader.triggerAsyncTask(StateUtils.getGlobalState().project);
        } catch (ClassCastException | JsonProcessingException err) {
            CodeSyncLogger.critical(
                String.format("Error parsing the response of /init endpoint. Error: %s", CommonUtils.getStackTrace(err)),
                email
            );
            return false;
        } catch (InvalidYmlFileError | FileNotFoundException error) {
            CodeSyncLogger.critical(
                String.format("Error creating S3 upload queue file. Error: %s", CommonUtils.getStackTrace(error)),
                email
            );
            return false;
        }

        // Everything completed successfully.
        return true;
    }

    public static void saveIamUser(String email, String iamAccessKey, String iamSecretKey) {
        try {
            User user = User.getTable().find(email);
            if (user != null) {
                user.setAccessKey(iamAccessKey);
                user.setSecretKey(iamSecretKey);
            } else {
                user = new User(email, null, iamAccessKey, iamSecretKey, true);
            }
            user.save();
        } catch (SQLException e) {
            CodeSyncLogger.critical(
                String.format(
                    "[INTELLI_REPO_INIT_ERROR]: Auth SQLite Database error. Error: %s",
                    CommonUtils.getStackTrace(e)
                )
            );
        }
    }

    /*
    Save repo data to the database.
     */
    public static void saveToDatabase(
        String email, Integer serverRepoId, String repoName, String repoPath, String branchName, Map<String, Integer> filePathAndIds
    ) throws SQLException {
        User user;
        try {
            user = User.getTable().get(email);
        } catch (UserNotFound e) {
            CodeSyncLogger.error(
                "[INTELLI_REPO_INIT_ERROR]: Error while saving repo data. Could not find the user in the database."
            );

            return;
        }

        Repo repo = new Repo(serverRepoId, repoName, repoPath, user.getId(), RepoState.SYNCED);
        repo.save();
        RepoBranch repoBranch = new RepoBranch(branchName, repo.getId());
        repoBranch.save();
        ArrayList<RepoFile> repoFiles = new ArrayList<>();

        for (Map.Entry<String, Integer> fileEntry: filePathAndIds.entrySet()) {
            repoFiles.add(new RepoFile(fileEntry.getKey(), repoBranch.getId(), fileEntry.getValue()));
        }

        RepoFile.getTable().bulkInsert(repoFiles);

    }

    public static String createSyncIgnore(String repoPath) {
        File syncIgnoreFile = Paths.get(repoPath, ".syncignore").toFile();
        File gitIgnoreFile = Paths.get(repoPath, ".gitignore").toFile();;
        PluginState pluginState = StateUtils.getState(repoPath);
        Project project = null;
        if (pluginState != null) {
            project = pluginState.project;
        }

        // no need to create .syncignore if it already exists.
        if (syncIgnoreFile.exists()) {
            return syncIgnoreFile.getPath();
        }

        if (!gitIgnoreFile.exists()) {
            // create an empty .syncignore file and let the user populate it.
            try {
                if (syncIgnoreFile.createNewFile()){
                    NotificationManager.getInstance().notifyInformation(
                            ".syncignore file is created, you can now update that file according to your preferences.",
                            project
                    );
                }
            } catch (IOException e) {
                NotificationManager.getInstance().notifyError(
                        ".syncignore could not be created, you will have to create that file yourself.",
                        project
                );
                e.printStackTrace();
            }
            return null;
        }

        try {
            List<String> gitIgnoreLines = org.apache.commons.io.FileUtils.readLines(
                    gitIgnoreFile, StandardCharsets.UTF_8
            );
            gitIgnoreLines.add(0, SYNC_IGNORE_COMMENT);
            org.apache.commons.io.FileUtils.writeLines(syncIgnoreFile, gitIgnoreLines);
        } catch (IOException e) {
            // Ignore this error, user can create the file himself as well/
            NotificationManager.getInstance().notifyError(
                    ".syncignore could not be created, you will have to create that file yourself.",
                    project
            );
            e.printStackTrace();
        }

        return syncIgnoreFile.getPath();
    }
}
