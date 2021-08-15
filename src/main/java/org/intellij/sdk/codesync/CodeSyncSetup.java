package org.intellij.sdk.codesync;

import com.intellij.ide.BrowserUtil;
import kotlin.Pair;
import org.intellij.sdk.codesync.auth.CodeSyncAuthServer;
import org.intellij.sdk.codesync.clients.CodeSyncClient;
import org.intellij.sdk.codesync.exceptions.InvalidConfigFileError;
import org.intellij.sdk.codesync.exceptions.InvalidYmlFileError;
import org.intellij.sdk.codesync.exceptions.RequestError;
import org.intellij.sdk.codesync.files.ConfigFile;
import org.intellij.sdk.codesync.files.ConfigRepo;
import org.intellij.sdk.codesync.files.UserFile;
import org.intellij.sdk.codesync.models.User;
import org.intellij.sdk.codesync.repoManagers.OriginalsRepoManager;
import org.intellij.sdk.codesync.repoManagers.ShadowRepoManager;
import org.intellij.sdk.codesync.userInput.SignupRequestDialogWrapper;
import org.intellij.sdk.codesync.userInput.SyncRepoDialogWrapper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import static org.intellij.sdk.codesync.Constants.*;

public class CodeSyncSetup {
    public static void setupCodeSyncRepo(String repoPath, String repoName) {
        try {
            ConfigFile configFile = new ConfigFile(CONFIG_PATH);

            ConfigRepo repo = configFile.getRepo(repoPath);

            if (!configFile.isRepoSynced(repoPath) || !repo.isSuccessfullySynced()) {
                boolean shouldSyncRepo = new SyncRepoDialogWrapper(repoName).showAndGet();

                if (shouldSyncRepo) {
                    boolean hasAccessToken = checkUserAccess();
                    if (hasAccessToken) {
                        syncRepo(repoPath);
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
    Check if the user has proper access for repo initialization, or not.

    This will also trigger the authentication flow if user does not have access token setup.
     */
    public static boolean checkUserAccess() {
        CodeSyncClient codeSyncClient = new CodeSyncClient();
        if (!codeSyncClient.isServerUp()) {
            NotificationManager.notifyError(Notification.SERVICE_NOT_AVAILABLE);
            return false;
        }

        String accessToken = null;
        try {
            UserFile userFile = new UserFile(USER_FILE_PATH);
            UserFile.User user = userFile.getUser();
            if (user != null) {
                accessToken = user.getAccessKey();
            }
        } catch (FileNotFoundException | InvalidYmlFileError error) {
            // Set access token to null to trigger user signup.
            accessToken = null;
        }

        // User already has access token, no need to proceed.
        if (accessToken != null) {
            try {
                Pair<Boolean, User> userPair = codeSyncClient.getUser(accessToken);
                Boolean isTokenValid = userPair.component1();

                // If token is not valid then need to authenticate again.
                if (!isTokenValid) {
                    return false;
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

        boolean shouldSignup = new SignupRequestDialogWrapper().showAndGet();

        // If user said no to signup request the return here.
        if (!shouldSignup) {
            // TODO: We should add logic here to not ask the user again next time he opens the project.
            return false;
        }

        CodeSyncAuthServer server;
        // TODO: Add code to resume repo-sync after successful login. This might be already happening, need to test.
        try {
            server =  CodeSyncAuthServer.getInstance();
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

    public static void syncRepo(String repoPath) {
        // create .syncignore file.
        createSyncIgnore(repoPath);

        String[] filePaths = listFiles(repoPath);
        System.out.println(Arrays.toString(filePaths));

        // Copy files to shadow repo.
        ShadowRepoManager shadowRepoManager = new ShadowRepoManager(repoPath);
        shadowRepoManager.copyFiles(filePaths);

        // Copy files to originals repo.
        OriginalsRepoManager originalsRepoManager = new OriginalsRepoManager(repoPath);
        originalsRepoManager.copyFiles(filePaths);
    }

    public static String[] listFiles(String directory) {
        String[] filePaths = {};
        try {
            filePaths = Files.walk(Paths.get(directory))
                    .filter(Files::isRegularFile)
                    .map(name -> name.toString().replace(directory, ""))
                    .map(name -> name.replaceFirst("/", ""))
                    .filter((name) -> !Utils.shouldIgnoreFile(name, directory))
                    .map((name) -> String.format("%s/%s", directory.replaceFirst("/$",""), name))
                    .toArray(String[]::new);
        } catch (IOException e) {
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
