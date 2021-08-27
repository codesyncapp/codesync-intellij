package org.intellij.sdk.codesync.codeSyncSetup;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.ide.BrowserUtil;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataConstants;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import kotlin.Pair;
import org.intellij.sdk.codesync.CodeSyncLogger;
import org.intellij.sdk.codesync.NotificationManager;
import org.intellij.sdk.codesync.Utils;
import org.intellij.sdk.codesync.auth.CodeSyncAuthServer;
import org.intellij.sdk.codesync.clients.CodeSyncClient;
import org.intellij.sdk.codesync.commands.ResumeCodeSyncCommand;
import org.intellij.sdk.codesync.exceptions.*;
import org.intellij.sdk.codesync.files.*;
import org.intellij.sdk.codesync.models.User;
import org.intellij.sdk.codesync.repoManagers.OriginalsRepoManager;
import org.intellij.sdk.codesync.repoManagers.ShadowRepoManager;
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

    public static void setupCodeSyncRepo(Project project, boolean skipSyncPrompt) {
        String repoPath = project.getBasePath();
        String repoName = project.getName();

        try {
            ConfigFile configFile = new ConfigFile(CONFIG_PATH);

            ConfigRepo repo = configFile.getRepo(repoPath);

            if (!configFile.isRepoSynced(repoPath) || !repo.isSuccessfullySynced()) {
                String branchName = Utils.GetGitBranch(repoPath);

                if (skipSyncPrompt) {
                    if (checkUserAccess(project, branchName)) {
                        syncRepoAsync(project, branchName);
                    }
                    return;
                }

                // Do not ask user to sync repo, if it is already in progress.
                if (!reposBeingSynced.contains(repoPath)) {
                    reposBeingSynced.add(repoPath);
                    boolean shouldSyncRepo = Messages.showYesNoDialog(
                            "Do you want to enable syncing of this Repo?",
                            String.format("'%s' Is not Being Synced!", repoName),
                            Notification.YES,
                            Notification.NO,
                            Messages.getQuestionIcon()
                    ) == Messages.YES;;

                    if (shouldSyncRepo) {
                        boolean hasAccessToken = checkUserAccess(project, branchName);
                        if (hasAccessToken) {
                            syncRepoAsync(project, branchName);
                        }
                    }
                }
            }

        } catch (InvalidConfigFileError error) {
            CodeSyncLogger.logEvent(String.format("Config file error, %s.\n", error.getMessage()));
        }

        // Create system directories required by the plugin.
        createSystemDirectories();
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
        String repoPath = project.getBasePath();
        String repoName = project.getName();

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

        boolean shouldSignup = Messages.showYesNoDialog(
                "Do you want to proceed with authentication?",
                "You Need to Authenticate!",
                Notification.YES,
                Notification.NO,
                Messages.getQuestionIcon()
        ) == Messages.YES;

        // If user said no to signup request the return here.
        if (!shouldSignup) {
            // TODO: We should add logic here to not ask the user again next time he opens the project.
            return false;
        }

        // We need to ask this first, otherwise we run into errors that it can not be done from outside of event thread.
        boolean isPublic = Messages.showYesNoDialog(
                project,
                Notification.PUBLIC_OR_PRIVATE,
                Notification.PUBLIC_OR_PRIVATE,
                Notification.YES,
                Notification.NO,
                Messages.getQuestionIcon()
        ) == Messages.YES;

        CodeSyncAuthServer server;
        try {
            server =  CodeSyncAuthServer.getInstance();
            CodeSyncAuthServer.registerPostAuthCommand(new ResumeCodeSyncCommand(project, branchName, isPublic));
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
    public static void syncRepoAsync(Project project, String branchName) {
        syncRepoAsync(project, branchName, null);
    }

    /*
    Start an async task to sync repo.
     */
    public static void syncRepoAsync(Project project, String branchName, Boolean isPublic) {
        String repoPath = project.getBasePath();
        String repoName = project.getName();


        if (isPublic == null) {
            isPublic = Messages.showYesNoDialog(
                    project,
                    Notification.PUBLIC_OR_PRIVATE,
                    Notification.PUBLIC_OR_PRIVATE,
                    Notification.YES,
                    Notification.NO,
                    Messages.getQuestionIcon()
            ) == Messages.YES;
        }

        Boolean finalIsPublic = isPublic;
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Initializing repo"){
            public void run(@NotNull ProgressIndicator progressIndicator) {

                // Set the progress bar percentage and text
                progressIndicator.setFraction(0.10);
                progressIndicator.setText("90% to finish");

                // start your process
                syncRepo(repoPath, repoName, branchName, finalIsPublic);

                // Finished
                progressIndicator.setFraction(1.0);
                progressIndicator.setText("Repo initialized");

            }});
    }

    public static void syncRepo(String repoPath, String repoName, String branchName, Boolean isPublic) {
        // create .syncignore file.
        createSyncIgnore(repoPath);

        String[] filePaths = listFiles(repoPath);
        System.out.println(Arrays.toString(filePaths));

        // Copy files to shadow repo.
        ShadowRepoManager shadowRepoManager = new ShadowRepoManager(repoPath, branchName);
        shadowRepoManager.copyFiles(filePaths);

        // Copy files to originals repo.
        OriginalsRepoManager originalsRepoManager = new OriginalsRepoManager(repoPath, branchName);
        originalsRepoManager.copyFiles(filePaths);

        // Upload the repo.
        uploadRepo(repoPath, repoName, filePaths, isPublic);

        // Remove originals repo.
        originalsRepoManager.delete();
    }

    public static void  uploadRepo(String repoPath, String repoName, String[] filePaths, Boolean isPublic) {
        ConfigFile configFile = null;
        try {
            configFile = new ConfigFile(CONFIG_PATH);
        } catch (InvalidConfigFileError e) {
            e.printStackTrace();

            // Can not proceed. This should never happen as the same check is applied at the start.
            return;
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
        if(!configFile.isRepoSynced(repoPath)) {
            configRepo.updateRepoBranch(branchName, new ConfigRepoBranch(branchName, branchFiles));
            configFile.updateRepo(repoPath, configRepo);
            try {
                configFile.publishRepoUpdate(configRepo);
            } catch (InvalidConfigFileError e) {
                e.printStackTrace();

                // Can not proceed. This should never happen as the same check is applied at the start.
                return;
            }
        } else if(!configRepo.containsBranch(branchName)) {
            ConfigRepoBranch configRepoBranch = new ConfigRepoBranch(branchName, branchFiles);
            try {
                configFile.publishBranchUpdate(configRepo, configRepoBranch);
            } catch (InvalidConfigFileError e) {
                e.printStackTrace();

                // Can not proceed. This should never happen as the same check is applied at the start.
                return;
            }
        }
        String accessToken = UserFile.getAccessToken();
        if (accessToken == null) {
            // Can not proceed. This should never happen as the same check is applied at the start.
            return;
        }
        CodeSyncClient codeSyncClient = new CodeSyncClient();
        JSONObject payload = new JSONObject();

        payload.put("name", repoName);
        payload.put("is_public", isPublic);
        payload.put("branch", branchName);
        payload.put("files_data", filesData.toJSONString());

        JSONObject response = codeSyncClient.uploadRepo(accessToken, payload);

        if (response.containsKey("error")) {
            NotificationManager.notifyError(Notification.ERROR_SYNCING_REPO);
            return;
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

            return;
        }

        // Save sequence token file.
        saveSequenceToken(email);

        int repoId;
        try {
            Map<String, Integer> filePathAndIds = new HashMap<>();
            repoId = ((Long) response.get("repo_id")).intValue();
            JSONObject filePathAndIdsObject = (JSONObject) response.get("file_path_and_id");

            if (filePathAndIdsObject == null) {
                CodeSyncLogger.logEvent("Invalid response from /init endpoint. Missing `file_path_and_id` key.");

                return;
            }

            filePathAndIds = new ObjectMapper().readValue(filePathAndIdsObject.toJSONString(), new TypeReference<Map<String, Integer>>(){});
            // Save File IDs
            saveFileIds(branchName, accessToken, email, repoId, filePathAndIds, configRepo, configFile);
        } catch (ClassCastException | JsonProcessingException err) {
            CodeSyncLogger.logEvent(String.format(
                    "Error parsing the response of /init endpoint. Error: %s", err.getMessage()
            ));

            return;
        }

        try {
            Map<String, Object> fileUrls = new HashMap<>();
            JSONObject urls = (JSONObject) response.get("urls");
            if (urls == null) {
                CodeSyncLogger.logEvent("Invalid response from /init endpoint. Missing `urls` key.");

                return;
            }
            fileUrls = new ObjectMapper().readValue(urls.toJSONString(), new TypeReference<Map<String, Object>>(){});

            // Upload file to S3.
            uploadToS3(repoPath, branchName, accessToken, email, repoId, fileUrls);
        } catch (ClassCastException | JsonProcessingException err) {
            CodeSyncLogger.logEvent(String.format(
                    "Error parsing the response of /init endpoint. Error: %s", err.getMessage()
            ));
            return;
        }
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

    public static void saveSequenceToken(String email) {
        try {
            SequenceTokenFile sequenceTokenFile = new SequenceTokenFile(SEQUENCE_TOKEN_FILE_PATH, true);
            if (sequenceTokenFile.getSequenceToken(email) == null) {
                sequenceTokenFile.updateSequenceToken(email, "");
            }
        } catch (InvalidYmlFileError error) {
            CodeSyncLogger.logEvent(
                    String.format("[INTELLI_REPO_INIT_ERROR]: Invalid sequence token file. Error: %s", error.getMessage())
            );
        } catch (FileNotFoundException error) {
            CodeSyncLogger.logEvent(
                    String.format("[INTELLI_REPO_INIT_ERROR]: sequence token file not found. Error: %s", error.getMessage())
            );
        } catch (FileNotCreatedError error) {
            CodeSyncLogger.logEvent(
                    String.format("[INTELLI_REPO_INIT_ERROR]: Could not create sequence token file. Error %s", error.getMessage())
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
            Stream<Path> filePathStream = filePathStream = Files.walk(Paths.get(directory))
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
