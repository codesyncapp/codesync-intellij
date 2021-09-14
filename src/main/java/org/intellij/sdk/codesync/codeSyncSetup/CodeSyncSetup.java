package org.intellij.sdk.codesync.codeSyncSetup;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import kotlin.Pair;
import org.intellij.sdk.codesync.CodeSyncLogger;
import org.intellij.sdk.codesync.NotificationManager;
import org.intellij.sdk.codesync.Utils;
import org.intellij.sdk.codesync.auth.CodeSyncAuthServer;
import org.intellij.sdk.codesync.clients.CodeSyncClient;
import org.intellij.sdk.codesync.commands.Command;
import org.intellij.sdk.codesync.commands.ResumeCodeSyncCommand;
import org.intellij.sdk.codesync.commands.ResumeRepoUploadCommand;
import org.intellij.sdk.codesync.exceptions.*;
import org.intellij.sdk.codesync.files.*;
import org.intellij.sdk.codesync.ui.messages.CodeSyncMessages;
import org.intellij.sdk.codesync.models.User;
import org.intellij.sdk.codesync.ui.progress.CodeSyncProgressIndicator;
import org.intellij.sdk.codesync.ui.progress.InitRepoMilestones;
import org.intellij.sdk.codesync.repoManagers.OriginalsRepoManager;
import org.intellij.sdk.codesync.repoManagers.ShadowRepoManager;
import org.intellij.sdk.codesync.ui.userInput.UserInputDialog;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

import static org.intellij.sdk.codesync.Constants.*;

public class CodeSyncSetup {
    public static final Set<String> reposBeingSynced = new HashSet<>();

    // TODO: need to figure out a way to improve this.
    private static Command resumeUploadCommand = null;

    public static void registerResumeUploadCommand(Project project, String branchName) {
        resumeUploadCommand = new ResumeRepoUploadCommand(project, branchName);
    }

    public static void executeResumeUploadCommand() {
        if (resumeUploadCommand != null){
            resumeUploadCommand.execute();

            // This is done to avoid multiple invocations.
            resumeUploadCommand = null;
        }
    }

    public static void setupCodeSyncRepoAsync(Project project, boolean skipSyncPrompt) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Initializing repo"){
            public void run(@NotNull ProgressIndicator progressIndicator) {
                CodeSyncProgressIndicator codeSyncProgressIndicator = new CodeSyncProgressIndicator(progressIndicator);

                // Set the progress bar percentage and text
                codeSyncProgressIndicator.setMileStone(InitRepoMilestones.START);
                setupCodeSyncRepo(project, codeSyncProgressIndicator, skipSyncPrompt);

                // Finished
                codeSyncProgressIndicator.setMileStone(InitRepoMilestones.END);

            }});
    }

    public static void setupCodeSyncRepo(Project project, CodeSyncProgressIndicator codeSyncProgressIndicator, boolean skipSyncPrompt) {
        // Create system directories required by the plugin.
        createSystemDirectories();

        String repoPath = project.getBasePath();
        String repoName = project.getName();

        try {
            ConfigFile configFile = new ConfigFile(CONFIG_PATH);
            ConfigRepo configRepo = configFile.getRepo(repoPath);

            if (configFile.isRepoDisconnected(repoPath) || !configRepo.isSuccessfullySynced()) {
                String branchName = Utils.GetGitBranch(repoPath);
                codeSyncProgressIndicator.setMileStone(InitRepoMilestones.CHECK_USER_ACCESS);

                if (skipSyncPrompt) {
                    if (checkUserAccess(project, branchName)) {
                        syncRepo(repoPath, repoName, branchName, project, codeSyncProgressIndicator);
                    }
                    return;
                }

                // Do not ask user to sync repo, if it is already in progress.
                if (!reposBeingSynced.contains(repoPath)) {
                    reposBeingSynced.add(repoPath);
                    boolean shouldSyncRepo = CodeSyncMessages.showYesNoMessage(
                            "Do you want to enable syncing of this Repo?",
                            String.format("'%s' Is not Being Synced!", repoName),
                            project
                    );

                    if (shouldSyncRepo) {
                        boolean hasAccessToken = checkUserAccess(project, branchName);
                        if (hasAccessToken) {
                            syncRepo(repoPath, repoName, branchName, project, codeSyncProgressIndicator);
                        }
                    }
                }
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
    public static boolean checkUserAccess(Project project, String branchName) {
        String accessToken = null;
        try {
            UserFile userFile = new UserFile(USER_FILE_PATH);
            UserFile.User user = userFile.getUser();
            if (user != null) {
                accessToken = user.getAccessToken();
            }
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
                "Do you want to proceed with authentication?",
                "You Need to Authenticate!",
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
            CodeSyncAuthServer.registerPostAuthCommand(new ResumeCodeSyncCommand(project, branchName));
            BrowserUtil.browse(server.getAuthorizationUrl());
        } catch (Exception exc) {
            exc.printStackTrace();
            CodeSyncLogger.logEvent(
                    "[INTELLIJ_AUTH_ERROR]: IntelliJ Login Error, an error occurred during user authentication."
            );
            NotificationManager.notifyError("There was a problem with login, please try again later.");
        }

        return false;
    }

    /*
    Start an async task to sync repo.
     */
    public static void syncRepoAsync(Project project, String branchName) {
        String repoPath = project.getBasePath();
        String repoName = project.getName();

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Initializing repo"){
            public void run(@NotNull ProgressIndicator progressIndicator) {
                CodeSyncProgressIndicator codeSyncProgressIndicator = new CodeSyncProgressIndicator(progressIndicator);

                // Set the progress bar percentage and text
                codeSyncProgressIndicator.setMileStone(InitRepoMilestones.START);
                syncRepo(repoPath, repoName, branchName, project, codeSyncProgressIndicator);

                // Finished
                codeSyncProgressIndicator.setMileStone(InitRepoMilestones.END);
            }});
    }

    /*
    This method is useful when repo upload needs to be resumed after user has updated syncignore file.
     */
    public static void resumeRepoUploadAsync(Project project, String branchName, boolean ignoreSyncIgnoreUpdate) {
        String repoPath = project.getBasePath();
        String repoName = project.getName();

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

        if (!ignoreSyncIgnoreUpdate) {
            // Ask user to modify the .syncignore file.
            askUserToUpdateSyncIgnore(project, branchName);

            return;
        }

        codeSyncProgressIndicator.setMileStone(InitRepoMilestones.FETCH_FILES);
        String[] filePaths = listFiles(repoPath);

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
        }
    }

    public static void askUserToUpdateSyncIgnore(Project project, String branchName){
        CodeSyncMessages.invokeAndWait(
                () -> {
                    // Ask user to modify the syncignore file
                    VirtualFile syncIgnoreFile = Utils.findSingleFile(".syncignore", project);
                    if (syncIgnoreFile != null) {
                        new OpenFileDescriptor(project, syncIgnoreFile, 0).navigate(true);

                        ToolWindow codeSyncToolWindow = ToolWindowManager.getInstance(project).getToolWindow("CodeSyncToolWindow");
                        if (codeSyncToolWindow != null) {
                            codeSyncToolWindow.show();
                        }

                        registerResumeUploadCommand(project, branchName);

                        new UserInputDialog(
                                "Please update .syncignore file.",
                                "Once you have updated the file, you can resume repo initialization " +
                                        "process, by click 'Continue with Initialization' button on the left CodeSync " +
                                        "menu."
                        ).show();

                    } else {
                        boolean shouldRetry = CodeSyncMessages.showYesNoMessage(
                                "Something went wrong!",
                                "Do you want to try again? If problem persists please contact support.",
                                project
                        );
                        if (shouldRetry) {
                            new ResumeRepoUploadCommand(project, branchName, false).execute();
                        }
                    }

                    return null;
                },
                ModalityState.defaultModalityState()
        );
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

            // Can not proceed. This should never happen as the same check is applied at the start.
            return false;
        }
        String branchName = Utils.GetGitBranch(repoPath);

        Map<String, Integer> branchFiles = new HashMap<>();
        JSONObject filesData = new JSONObject();

        String[] relativeFilePaths = Arrays.stream(filePaths)
                .map(filePath -> filePath.replace(repoPath, ""))
                .map(filePath -> filePath.replaceFirst("/", ""))
                .toArray(String[]::new);

        for (String relativeFilePath: relativeFilePaths) {
            branchFiles.put(relativeFilePath, null);
            Map<String, Object> fileInfo;
            try {
                fileInfo = Utils.getFileInfo(
                        String.format("%s/%s", repoPath.replaceFirst("/$",""), relativeFilePath)
                );
            } catch (FileInfoError error) {
                CodeSyncLogger.logEvent(String.format("File Info could not be found for %s", relativeFilePath));

                // Skip this file.
                continue;
            }

            JSONObject item = new JSONObject();
            item.put("is_binary",  fileInfo.get("isBinary"));
            item.put("size", fileInfo.get("size"));
            item.put("created_at", Utils.getPosixTime((String) fileInfo.get("creationTime")));
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

                // Can not proceed. This should never happen as the same check is applied at the start.
                return false;
            }
        } else if(!configRepo.containsBranch(branchName)) {
            ConfigRepoBranch configRepoBranch = new ConfigRepoBranch(branchName, branchFiles);
            try {
                configFile.publishBranchUpdate(configRepo, configRepoBranch);
            } catch (InvalidConfigFileError e) {
                e.printStackTrace();

                // Can not proceed. This should never happen as the same check is applied at the start.
                return false;
            }
        }
        String accessToken = UserFile.getAccessToken();
        if (accessToken == null) {
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

        codeSyncProgressIndicator.setMileStone(InitRepoMilestones.PROCESS_RESPONSE);
        JSONObject response = codeSyncClient.uploadRepo(accessToken, payload);

        if (response.containsKey("error")) {
            NotificationManager.notifyError(Notification.ERROR_SYNCING_REPO);
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
            saveFileIds(branchName, accessToken, email, repoId, filePathAndIds, configRepo, configFile);
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

        userFile.setUser(email, iamAccessKey, iamSecretKey);
        try {
            userFile.writeYml();
        } catch (FileNotFoundException | InvalidYmlFileError error) {
            CodeSyncLogger.logEvent(
                    String.format("[INTELLI_REPO_INIT_ERROR]: Could not write to user auth file. Error %s", error.getMessage())
            );
        }
    }

    public static void saveFileIds(
            String branchName, String accessToken, String userEmail, Integer repoId, Map<String, Integer> filePathAndIds,
            ConfigRepo configRepo, ConfigFile configFile
    ) {
        ConfigRepoBranch configRepoBranch = new ConfigRepoBranch(branchName, filePathAndIds);
        configRepo.id = repoId;
        configRepo.token = accessToken;
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
        String originalsRepoBranchPath = Paths.get(ORIGINALS_REPO, repoPath, branchName).toString();

        CodeSyncClient codeSyncClient = new CodeSyncClient();

        for (Map.Entry<String, Object> fileUrl : fileUrls.entrySet()) {
            File originalsFile = new File(Paths.get(originalsRepoBranchPath, fileUrl.getKey()).toString());
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

    /*
    List absolute file paths of all the files in a directory.

    This is recursively list all the files containing in the given directory and all its sub-directories.
     */
    public static String[] listFiles(String directory) {
        String[] filePaths = {};
        IgnoreFile ignoreFile;

        try {
            Stream<Path> filePathStream = Files.walk(Paths.get(directory))
                    .filter(Files::isRegularFile);

            try {
                ignoreFile = new IgnoreFile(Paths.get(directory).toString());
                filePathStream = filePathStream.filter(path -> !ignoreFile.shouldIgnore(path.toFile()));
            } catch (FileNotFoundError error) {
                // Do not filter anything if ignore file is not present.
            }

            filePaths = filePathStream.map(Path::toString).toArray(String[]::new);
        }
        catch (IOException e) {
            e.printStackTrace();
        }

       return filePaths;
    }

    public static void createSyncIgnore(String repoPath) {
        String syncIgnorePath = String.format("%s/.syncignore", repoPath);
        String gitIgnorePath = String.format("%s/.gitignore", repoPath);
        File syncIgnoreFile = new File(syncIgnorePath);
        File gitIgnoreFile = new File(gitIgnorePath);

        // no need to create .syncignore if it already exists.
        if (syncIgnoreFile.exists()) {
            return ;
        }

        if (!gitIgnoreFile.exists()) {
            // create an empty .syncignore file and let the user populate it.
            try {
                if (syncIgnoreFile.createNewFile()){
                    NotificationManager.notifyInformation(
                            ".syncignore file is created, you can now update that file according to your preferences."
                    );
                }
            } catch (IOException e) {
                NotificationManager.notifyError(
                        ".syncignore could not be created, you will have to create that file yourself."
                );
                e.printStackTrace();
            }
            return;
        }

        try {
            Files.copy(gitIgnoreFile.toPath(), syncIgnoreFile.toPath());
        } catch (IOException e) {
            // Ignore this error, user can create the file himself as well/
            NotificationManager.notifyError(
                    ".syncignore could not be created, you will have to create that file yourself."
            );
            e.printStackTrace();
        }

    }
}
