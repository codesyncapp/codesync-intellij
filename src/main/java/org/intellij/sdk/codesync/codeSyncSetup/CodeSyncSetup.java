package org.intellij.sdk.codesync.codeSyncSetup;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import kotlin.Pair;
import org.intellij.sdk.codesync.CodeSyncLogger;
import org.intellij.sdk.codesync.NotificationManager;
import org.intellij.sdk.codesync.Utils;
import org.intellij.sdk.codesync.auth.CodeSyncAuthServer;
import org.intellij.sdk.codesync.clients.CodeSyncClient;
import org.intellij.sdk.codesync.commands.Command;
import org.intellij.sdk.codesync.commands.ReloadStateCommand;
import org.intellij.sdk.codesync.commands.ResumeCodeSyncCommand;
import org.intellij.sdk.codesync.commands.ResumeRepoUploadCommand;
import org.intellij.sdk.codesync.exceptions.*;
import org.intellij.sdk.codesync.exceptions.file.UserFileError;
import org.intellij.sdk.codesync.exceptions.network.RepoUpdateError;
import org.intellij.sdk.codesync.exceptions.network.ServerConnectionError;
import org.intellij.sdk.codesync.exceptions.repo.RepoNotActive;
import org.intellij.sdk.codesync.files.*;
import org.intellij.sdk.codesync.state.PluginState;
import org.intellij.sdk.codesync.state.StateUtils;
import org.intellij.sdk.codesync.ui.messages.CodeSyncMessages;
import org.intellij.sdk.codesync.models.User;
import org.intellij.sdk.codesync.ui.progress.CodeSyncProgressIndicator;
import org.intellij.sdk.codesync.ui.progress.InitRepoMilestones;
import org.intellij.sdk.codesync.repoManagers.OriginalsRepoManager;
import org.intellij.sdk.codesync.repoManagers.ShadowRepoManager;
import org.intellij.sdk.codesync.utils.CommonUtils;
import org.intellij.sdk.codesync.utils.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

import static org.intellij.sdk.codesync.Constants.*;

public class CodeSyncSetup {
    public static final Set<String> reposBeingSynced = new HashSet<>();

    // TODO: need to figure out a way to improve this.
    private static Command resumeUploadCommand = null;

    public static void registerResumeUploadCommand(Project project, String repoPath, String repoName, String branchName) {
        resumeUploadCommand = new ResumeRepoUploadCommand(project, repoPath, repoName, branchName);
    }

    public static void executeResumeUploadCommand() {
        if (resumeUploadCommand != null){
            resumeUploadCommand.execute();

            // This is done to avoid multiple invocations.
            resumeUploadCommand = null;
        }
    }

    /*
    This can be called to disconnect an already connected repo.
     */
    public static void disconnectRepo(Project project, String repoPath, String repoName) throws InvalidConfigFileError, RepoNotActive, UserFileError, ServerConnectionError, RepoUpdateError {
        // Show confirmation message.
        boolean shouldDisconnect = CodeSyncMessages.showYesNoMessage(
                "Are you sure?",
                Notification.REPO_UNSYNC_CONFIRMATION,
                project
        );

        // User confirmed to disconnect the repo.
        if (shouldDisconnect) {
            ConfigFile configFile = new ConfigFile(CONFIG_PATH);

            if (!configFile.isRepoActive(repoPath)) {
               throw new RepoNotActive(String.format("Repo '%s' is not active and can not be disconnected.", repoName));
            };
            UserFile userFile;

            try {
                userFile = new UserFile(USER_FILE_PATH);
            } catch (FileNotFoundException | InvalidYmlFileError e) {
                throw new UserFileError(
                        String.format(
                                "Repo '%s' could not be disconnected because there was an error trying to read user file.",
                                repoName
                        )
                );
            }
            ConfigRepo configRepo = configFile.getRepo(repoPath);
            UserFile.User user = userFile.getUser(configRepo.email);
            if (user == null) {
                throw new UserFileError(
                        String.format(
                                "Repo '%s' could not be disconnected because user data is missing from user file.",
                                repoName
                        )
                );
            }
            String accessToken = user.getAccessToken();
            CodeSyncClient codeSyncClient = new CodeSyncClient();

            if (!codeSyncClient.isServerUp()) {
                throw new ServerConnectionError(
                        String.format(
                                "Repo '%s' could not be disconnected because we could not connect to the codesync servers.",
                                repoName
                        )
                );
            }
            JSONObject payload = new JSONObject();
            payload.put("is_in_sync", false);
            JSONObject response = codeSyncClient.updateRepo(accessToken, configRepo.id, payload);
            if (response.containsKey("error")) {
                throw new RepoUpdateError(
                        String.format(
                                "Repo '%s' could not be disconnected server returned an error response. Error: %s",
                                repoName,
                                response.get("error")
                        )
                );
            }

            configRepo.isDisconnected = true;
            configFile.publishRepoUpdate(configRepo);

            NotificationManager.notifyInformation(Notification.REPO_UNSYNCED, project);
        }
    }

    public static void setupCodeSyncRepoAsync(Project project, String repoPath, String repoName, boolean skipSyncPrompt) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Initializing repo"){
            public void run(@NotNull ProgressIndicator progressIndicator) {
                CodeSyncProgressIndicator codeSyncProgressIndicator = new CodeSyncProgressIndicator(progressIndicator);

                // Set the progress bar percentage and text
                codeSyncProgressIndicator.setMileStone(InitRepoMilestones.START);
                setupCodeSyncRepo(project, repoPath, repoName, codeSyncProgressIndicator, skipSyncPrompt);

                // Finished
                codeSyncProgressIndicator.setMileStone(InitRepoMilestones.END);
            }});
    }

    public static void setupCodeSyncRepo(Project project, String repoPath, String repoName, CodeSyncProgressIndicator codeSyncProgressIndicator, boolean skipSyncPrompt) {
        try {
            ConfigFile configFile = new ConfigFile(CONFIG_PATH);
            ConfigRepo configRepo = configFile.getRepo(repoPath);

            if (configFile.isRepoDisconnected(repoPath) || !configRepo.isSuccessfullySyncedWithBranch()) {
                String branchName = Utils.GetGitBranch(repoPath);
                codeSyncProgressIndicator.setMileStone(InitRepoMilestones.CHECK_USER_ACCESS);

                if (skipSyncPrompt) {
                    if (checkUserAccess(project, repoPath, repoName, branchName)) {
                        syncRepo(repoPath, repoName, branchName, project, codeSyncProgressIndicator);
                    }
                    return;
                }

                // This is kind of a hack, but for some reason this method is called more than once during user is
                // shown a popup to confirm repo syncing. and during the popup is open we do not want to show
                // the message "Notification.REPO_SYNC_IN_PROGRESS_MESSAGE" (from the else statement.)
                // That is why why this boolean is placed here.
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
                        boolean hasAccessToken = checkUserAccess(project, repoPath, repoName, branchName);
                        if (hasAccessToken) {
                            syncRepo(repoPath, repoName, branchName, project, codeSyncProgressIndicator);
                        }
                    }
                } else if (shouldSyncRepo) {
                    NotificationManager.notifyInformation(
                            String.format(Notification.REPO_SYNC_IN_PROGRESS_MESSAGE, repoName),
                            project
                    );
                }
            } else if (!configFile.isRepoDisconnected(repoPath)) {
                NotificationManager.notifyInformation(
                        String.format(Notification.REPO_IN_SYNC_MESSAGE, repoName),
                        project
                );
            }
        } catch (InvalidConfigFileError error) {
            CodeSyncLogger.logEvent(String.format("Config file error, %s.\n", error.getMessage()));
        }
    }

    public static void createSystemDirectories() {
        // Create system folders
        String[] systemFolders = {CODESYNC_ROOT, DIFFS_REPO, ORIGINALS_REPO, SHADOW_REPO, DELETED_REPO};
        for (String systemFolder : systemFolders) {
            File folder = new File(systemFolder);
            folder.mkdirs();
        }
        String[] systemFilePaths = {CONFIG_PATH, USER_FILE_PATH, SEQUENCE_TOKEN_FILE_PATH, LOCK_FILE};
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
            NotificationManager.notifyError(Notification.SERVICE_NOT_AVAILABLE);
            return false;
        }

        try {
            Pair<Boolean, User> userPair = codeSyncClient.getUser(accessToken);
            Boolean isTokenValid = userPair.component1();

            // If token is not valid then need to authenticate again.
            if (!isTokenValid) {
                throw new InvalidAccessTokenError("Invalid access token, needs to be refreshed.");
            }

            User user = userPair.component2();
            if (user.repoCount >= user.plan.repoCount) {
                NotificationManager.notifyError(Notification.UPGRADE_PLAN);
                return false;
            }

            // User has access to repo sync, and can proceed.
            return true;
        } catch (RequestError e) {
            // Unknown token status, need to authenticate again.
            return false;
        }
    }

    /*
    Check if the user has proper access for repo initialization, or not.

    This will also trigger the authentication flow if user does not have access token setup.
     */
    public static boolean checkUserAccess(Project project, String repoPath, String repoName, String branchName) {
        String accessToken;
        try {
            UserFile userFile = new UserFile(USER_FILE_PATH);
            accessToken = userFile.getActiveAccessToken();
        } catch (FileNotFoundException | InvalidYmlFileError error) {
            // Set access token to null to trigger user signup.
            accessToken = null;
        }

        // User already has access token, no need to proceed.
        if (accessToken != null) {
            try {
                return validateAccessToken(accessToken);
            } catch (InvalidAccessTokenError invalidAccessTokenError) {
                // no action needed, user will be prompted for authenticated later by default.
            }
        }

        boolean shouldSignup = CodeSyncMessages.showYesNoMessage(
                "Do you want to login or sign up to use CodeSync?",
                "To stream your code to the cloud, you'll need to authenticate.",
                project
        );

        // If user said no to signup request the return here.
        if (!shouldSignup) {
            // TODO: We should add logic here to not ask the user again next time he opens the project.
            return false;
        }

        CodeSyncAuthServer server;
        try {
            server =  CodeSyncAuthServer.getInstance();
            CodeSyncAuthServer.registerPostAuthCommand(new ResumeCodeSyncCommand(project, repoPath, repoName, branchName));
            CodeSyncAuthServer.registerPostAuthCommand(new ReloadStateCommand(project));
            BrowserUtil.browse(server.getAuthorizationUrl());
        } catch (Exception exc) {
            exc.printStackTrace();
            CodeSyncLogger.logEvent(
                    "[INTELLIJ_AUTH_ERROR]: IntelliJ Login Error, an error occurred during user authentication."
            );
            NotificationManager.notifyError("There was a problem with login, please try again later.", project);
        }

        return false;
    }

    /*
    Start an async task to sync repo.
     */
    public static void syncRepoAsync(Project project, String repoPath, String repoName, String branchName) {
        if (CommonUtils.isWindows()){
            // For some reason people at intelli-j thought it would be a good idea to confuse users by using
            // forward slashes in paths instead of windows path separator.
            repoPath = repoPath.replaceAll("/", "\\\\");
        }

        String finalRepoPath = repoPath;
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Initializing repo"){
            public void run(@NotNull ProgressIndicator progressIndicator) {
                CodeSyncProgressIndicator codeSyncProgressIndicator = new CodeSyncProgressIndicator(progressIndicator);

                // Set the progress bar percentage and text
                codeSyncProgressIndicator.setMileStone(InitRepoMilestones.START);
                syncRepo(finalRepoPath, repoName, branchName, project, codeSyncProgressIndicator);

                // Finished
                codeSyncProgressIndicator.setMileStone(InitRepoMilestones.END);
            }});
    }

    /*
    This method is useful when repo upload needs to be resumed after user has updated syncignore file.
     */
    public static void resumeRepoUploadAsync(Project project, String repoPath, String repoName, String branchName, boolean ignoreSyncIgnoreUpdate) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Initializing repo") {
            public void run(@NotNull ProgressIndicator progressIndicator) {
                CodeSyncProgressIndicator codeSyncProgressIndicator = new CodeSyncProgressIndicator(progressIndicator);

                // Set the progress bar percentage and text
                codeSyncProgressIndicator.setMileStone(InitRepoMilestones.START);
                syncRepo(repoPath, repoName, branchName, project, codeSyncProgressIndicator, ignoreSyncIgnoreUpdate);

                // Finished
                codeSyncProgressIndicator.setMileStone(InitRepoMilestones.END);
            }
        });
    }

    public static void syncRepo(String repoPath, String repoName, String branchName, Project project, CodeSyncProgressIndicator codeSyncProgressIndicator) {
        syncRepo(repoPath, repoName, branchName, project, codeSyncProgressIndicator, false);
    }

    public static void syncRepo(String repoPath, String repoName, String branchName, Project project, CodeSyncProgressIndicator codeSyncProgressIndicator, boolean ignoreSyncIgnoreUpdate) {
        // create .syncignore file.
        createSyncIgnore(repoPath);

        /*
        TODO: If we decide to keep this disabled, then we need to remove this and related code.
        if (!ignoreSyncIgnoreUpdate) {
            // Ask user to modify the .syncignore file.
            askUserToUpdateSyncIgnore(project, branchName);

            return;
        }
        */
        codeSyncProgressIndicator.setMileStone(InitRepoMilestones.FETCH_FILES);
        String[] filePaths = FileUtils.listFiles(repoPath);

        codeSyncProgressIndicator.setMileStone(InitRepoMilestones.COPY_FILES);
        // Copy files to shadow repo.
        ShadowRepoManager shadowRepoManager = new ShadowRepoManager(repoPath, branchName);
        shadowRepoManager.copyFiles(filePaths);

        // Copy files to originals repo.
        OriginalsRepoManager originalsRepoManager = new OriginalsRepoManager(repoPath, branchName);
        originalsRepoManager.copyFiles(filePaths);

        // Upload the repo.
        codeSyncProgressIndicator.setMileStone(InitRepoMilestones.SENDING_REPO);
        boolean wasUploadSuccessful = uploadRepo(repoPath, repoName, filePaths, project, codeSyncProgressIndicator);

        codeSyncProgressIndicator.setMileStone(InitRepoMilestones.CLEANUP);
        if (wasUploadSuccessful) {
            // Remove originals repo if it was uploaded successfully.
            originalsRepoManager.delete();

            // Show success message and update state
            NotificationManager.notifyInformation(Notification.INIT_SUCCESS_MESSAGE, project);
            StateUtils.reloadState(project);
        } else {
            // Show failure message.
            NotificationManager.notifyError(Notification.INIT_FAILURE_MESSAGE, project);
        }
    }

//    public static void askUserToUpdateSyncIgnore(Project project. String repoPath, String repoName, String branchName){
//        CommonUtils.invokeAndWait(
//                () -> {
//                    // Ask user to modify the syncignore file
//                    TODO: Cannot use project.getBasePath() we would probably need to use something else.
//                    VirtualFile syncIgnoreFile = CommonUtils.findSingleFile(".syncignore", project.getBasePath());
//                    if (syncIgnoreFile != null) {
//                        new OpenFileDescriptor(project, syncIgnoreFile, 0).navigate(true);
//
//                        ToolWindow codeSyncToolWindow = ToolWindowManager.getInstance(project).getToolWindow("CodeSyncToolWindow");
//                        if (codeSyncToolWindow != null) {
//                            codeSyncToolWindow.show();
//                        }
//
//                        registerResumeUploadCommand(project, repoPath, repoName, branchName);
//
//                        new UserInputDialog(
//                                "Please update .syncignore file.",
//                                "Once you have updated the file, you can resume repo initialization " +
//                                        "process, by click 'Continue with Initialization' button on the left CodeSync " +
//                                        "menu."
//                        ).show();
//
//                    } else {
//                        boolean shouldRetry = CodeSyncMessages.showYesNoMessage(
//                                "Something went wrong!",
//                                "Do you want to try again? If problem persists please contact support.",
//                                project
//                        );
//                        if (shouldRetry) {
//                            new ResumeRepoUploadCommand(project, branchName, false).execute();
//                        }
//                    }
//
//                    return null;
//                },
//                ModalityState.defaultModalityState()
//        );
//    }

    public static void uploadRepoAsync(String repoPath, String repoName, String[] filePaths, Project project){
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Initializing repo"){
            public void run(@NotNull ProgressIndicator progressIndicator) {
                CodeSyncProgressIndicator codeSyncProgressIndicator = new CodeSyncProgressIndicator(progressIndicator);

                // Set the progress bar percentage and text
                codeSyncProgressIndicator.setMileStone(InitRepoMilestones.START);
                uploadRepo(repoPath, repoName, filePaths, project, codeSyncProgressIndicator);

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
    public static boolean uploadRepo(String repoPath, String repoName, String[] filePaths, Project project, CodeSyncProgressIndicator codeSyncProgressIndicator) {
        ConfigFile configFile;
        try {
            configFile = new ConfigFile(CONFIG_PATH);
        } catch (InvalidConfigFileError e) {
            e.printStackTrace();

            // Show error message.
            NotificationManager.notifyInformation(Notification.INIT_ERROR_MESSAGE, project);

            // Can not proceed. This should never happen as the same check is applied at the start.
            return false;
        }
        String branchName = Utils.GetGitBranch(repoPath);

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
                CodeSyncLogger.logEvent(String.format("File Info could not be found for %s", relativeFilePath));

                // Skip this file.
                continue;
            }

            JSONObject item = new JSONObject();
            item.put("is_binary",  fileInfo.get("isBinary"));
            item.put("size", fileInfo.get("size"));
            item.put("created_at", CommonUtils.getPosixTime((String) fileInfo.get("creationTime")));
            filesData.put(relativeFilePath, item);
        }
        ConfigRepo configRepo = new ConfigRepo(repoPath);
        if(configFile.isRepoDisconnected(repoPath)) {
            configRepo.updateRepoBranch(branchName, new ConfigRepoBranch(branchName, branchFiles));
            configFile.updateRepo(repoPath, configRepo);
            try {
                configFile.publishRepoUpdate(configRepo);
            } catch (InvalidConfigFileError e) {
                e.printStackTrace();

                // Show error message.
                NotificationManager.notifyInformation(Notification.INIT_ERROR_MESSAGE, project);

                // Can not proceed. This should never happen as the same check is applied at the start.
                return false;
            }
        } else if(!configRepo.containsBranch(branchName)) {
            ConfigRepoBranch configRepoBranch = new ConfigRepoBranch(branchName, branchFiles);
            try {
                configFile.publishBranchUpdate(configRepo, configRepoBranch);
            } catch (InvalidConfigFileError e) {
                e.printStackTrace();

                // Show error message.
                NotificationManager.notifyInformation(Notification.INIT_ERROR_MESSAGE, project);

                // Can not proceed. This should never happen as the same check is applied at the start.
                return false;
            }
        }
        String accessToken = UserFile.getAccessToken();
        if (accessToken == null) {
            // Show error message.
            NotificationManager.notifyInformation(Notification.INIT_ERROR_MESSAGE, project);

            // Can not proceed. This should never happen as the same check is applied at the start.
            return false;
        }
        CodeSyncClient codeSyncClient = new CodeSyncClient();
        JSONObject payload = new JSONObject();

        boolean isPublic = CodeSyncMessages.showYesNoMessage(
                Notification.PUBLIC_OR_PRIVATE,
                Notification.PUBLIC_OR_PRIVATE,
                project
        );

        payload.put("name", repoName);
        payload.put("is_public", isPublic);
        payload.put("branch", branchName);
        payload.put("files_data", filesData.toJSONString());
        payload.put("source", DIFF_SOURCE);
        payload.put("platform", CommonUtils.getOS());

        codeSyncProgressIndicator.setMileStone(InitRepoMilestones.PROCESS_RESPONSE);
        JSONObject response = codeSyncClient.uploadRepo(accessToken, payload);

        if (response.containsKey("error")) {
            NotificationManager.notifyError(Notification.INIT_ERROR_MESSAGE, project);
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
            CodeSyncLogger.logEvent(String.format(
                    "Error parsing the response of /init endpoint. Error: %s", err.getMessage()
            ));

            return false;
        }

        int repoId;
        try {
            Map<String, Integer> filePathAndIds = new HashMap<>();
            repoId = ((Long) response.get("repo_id")).intValue();
            JSONObject filePathAndIdsObject = (JSONObject) response.get("file_path_and_id");

            if (filePathAndIdsObject == null) {
                CodeSyncLogger.logEvent("Invalid response from /init endpoint. Missing `file_path_and_id` key.");

                return false;
            }

            codeSyncProgressIndicator.setMileStone(InitRepoMilestones.CONFIG_UPDATE);
            filePathAndIds = new ObjectMapper().readValue(filePathAndIdsObject.toJSONString(), new TypeReference<Map<String, Integer>>(){});
            // Save File IDs
            saveFileIds(branchName, email, repoId, filePathAndIds, configRepo, configFile);
        } catch (ClassCastException | JsonProcessingException err) {
            CodeSyncLogger.logEvent(String.format(
                    "Error parsing the response of /init endpoint. Error: %s", err.getMessage()
            ));

            return false;
        }

        try {
            Map<String, Object> fileUrls = new HashMap<>();
            JSONObject urls = (JSONObject) response.get("urls");
            if (urls == null) {
                CodeSyncLogger.logEvent("Invalid response from /init endpoint. Missing `urls` key.");

                return false;
            }
            fileUrls = new ObjectMapper().readValue(urls.toJSONString(), new TypeReference<Map<String, Object>>(){});

            codeSyncProgressIndicator.setMileStone(InitRepoMilestones.UPLOAD_FILES);
            // Upload file to S3.
            uploadToS3(repoPath, branchName, accessToken, email, repoId, fileUrls);
            ReloadStateCommand reloadStateCommand = new ReloadStateCommand(project);
            reloadStateCommand.execute();
        } catch (ClassCastException | JsonProcessingException err) {
            CodeSyncLogger.logEvent(String.format(
                    "Error parsing the response of /init endpoint. Error: %s", err.getMessage()
            ));
            return false;
        }

        // Everything completed successfully.
        return true;
    }

    public static void saveIamUser(String email, String iamAccessKey, String iamSecretKey) {
        UserFile userFile;
        try {
            userFile = new UserFile(USER_FILE_PATH, true);
        } catch (FileNotFoundException e) {
            CodeSyncLogger.logEvent(
                    String.format("[INTELLI_REPO_INIT_ERROR]: auth file not found. Error: %s", e.getMessage())
            );
            return;
        } catch (InvalidYmlFileError error) {
            error.printStackTrace();
            CodeSyncLogger.logEvent(
                    String.format("[INTELLI_REPO_INIT_ERROR]: Invalid auth file. Error: %s", error.getMessage())
            );
            // Could not read user file.
            return;
        } catch (FileNotCreatedError error) {
            CodeSyncLogger.logEvent(
                    String.format("[INTELLI_REPO_INIT_ERROR]: Could not create user auth file. Error %s", error.getMessage())
            );
            return;
        }

        userFile.setActiveUser(email, iamAccessKey, iamSecretKey);
        try {
            userFile.writeYml();
        } catch (FileNotFoundException | InvalidYmlFileError | FileLockedError error) {
            CodeSyncLogger.logEvent(
                    String.format("[INTELLI_REPO_INIT_ERROR]: Could not write to user auth file. Error %s", error.getMessage())
            );
        }
    }

    public static void saveFileIds(
            String branchName, String userEmail, Integer repoId, Map<String, Integer> filePathAndIds,
            ConfigRepo configRepo, ConfigFile configFile
    ) {
        ConfigRepoBranch configRepoBranch = new ConfigRepoBranch(branchName, filePathAndIds);
        configRepo.id = repoId;
        configRepo.email = userEmail;

        try {
            configRepo.updateRepoBranch(branchName, configRepoBranch);
            configFile.publishRepoUpdate(configRepo);
        } catch (InvalidConfigFileError error) {
            CodeSyncLogger.logEvent(
                    String.format("[INTELLI_REPO_INIT_ERROR]: Could not update config file. Error %s", error.getMessage())
            );
        }
    }

    public static void uploadToS3(
            String repoPath, String branchName, String accessToken, String userEmail, Integer repoId,
            Map<String, Object> fileUrls
    ) {
        OriginalsRepoManager originalsRepoManager = new OriginalsRepoManager(repoPath, branchName);
        CodeSyncClient codeSyncClient = new CodeSyncClient();

        for (Map.Entry<String, Object> fileUrl : fileUrls.entrySet()) {
            File originalsFile = originalsRepoManager.getFilePath(fileUrl.getKey()).toFile();
            if (fileUrl.getValue() == "") {
                // Skip if file is empty.
                 continue;
            }
            try {
                codeSyncClient.uploadToS3(
                        originalsFile,
                        (Map<String, Object>) fileUrl.getValue()
                );
            } catch (RequestError | ClassCastException error) {
                CodeSyncLogger.logEvent(
                        String.format("[INTELLI_REPO_INIT_ERROR]: Could not upload file to S3. Error %s", error.getMessage())
                );
            }
        }
    }

    public static void createSyncIgnore(String repoPath) {
        File syncIgnoreFile = Paths.get(repoPath, ".syncignore").toFile();
        File gitIgnoreFile = Paths.get(repoPath, ".gitignore").toFile();;
        PluginState pluginState = StateUtils.getState(repoPath);
        Project project = null;
        if (pluginState != null) {
            project = pluginState.project;
        }

        // no need to create .syncignore if it already exists.
        if (syncIgnoreFile.exists()) {
            return ;
        }

        if (!gitIgnoreFile.exists()) {
            // create an empty .syncignore file and let the user populate it.
            try {
                if (syncIgnoreFile.createNewFile()){
                    NotificationManager.notifyInformation(
                            ".syncignore file is created, you can now update that file according to your preferences.",
                            project
                    );
                }
            } catch (IOException e) {
                NotificationManager.notifyError(
                        ".syncignore could not be created, you will have to create that file yourself.",
                        project
                );
                e.printStackTrace();
            }
            return;
        }

        try {
            List<String> gitIgnoreLines = org.apache.commons.io.FileUtils.readLines(
                    gitIgnoreFile, StandardCharsets.UTF_8
            );
            gitIgnoreLines.add(0, SYNC_IGNORE_COMMENT);
            org.apache.commons.io.FileUtils.writeLines(syncIgnoreFile, gitIgnoreLines);
        } catch (IOException e) {
            // Ignore this error, user can create the file himself as well/
            NotificationManager.notifyError(
                    ".syncignore could not be created, you will have to create that file yourself.",
                    project
            );
            e.printStackTrace();
        }

    }
}
