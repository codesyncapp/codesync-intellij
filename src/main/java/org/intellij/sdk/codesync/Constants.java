package org.intellij.sdk.codesync;

import org.intellij.sdk.codesync.configuration.Configuration;
import org.intellij.sdk.codesync.configuration.ConfigurationFactory;

import java.nio.file.Paths;

public final class Constants {
    private static final Configuration configuration = ConfigurationFactory.getConfiguration();

    private Constants() {
        // restrict instantiation
    }
    // Values that can be overridden via environment based Configuration should go here.
    private static final String CODESYNC_DIR_NAME = configuration.getCodeSyncDirName();
    private static final String CODESYNC_DOMAIN = configuration.getCodeSyncDomain();
    private static final String CODESYNC_HOST = configuration.getCodeSyncHost();
    private static final String CODESYNC_APP = configuration.getCodeSyncAppURL();
    private static final String WEB_APP_URL = configuration.getCodeSyncWebAppURL();

    public static String USER_HOME = System.getProperty("user.home");
    public static String CODESYNC_ROOT =Paths.get(USER_HOME, CODESYNC_DIR_NAME).toString();
    public static String DIFFS_REPO = Paths.get(CODESYNC_ROOT, ".diffs", ".intellij").toString();
    public static String ORIGINALS_REPO = Paths.get(CODESYNC_ROOT, ".originals").toString();
    public static String DELETED_REPO = Paths.get(CODESYNC_ROOT, ".deleted").toString();
    public static String SHADOW_REPO = Paths.get(CODESYNC_ROOT, ".shadow").toString();
    public static String CONFIG_PATH = Paths.get(CODESYNC_ROOT, "config.yml").toString();
    public static String USER_FILE_PATH = Paths.get(CODESYNC_ROOT, "user.yml").toString();
    public static String SEQUENCE_TOKEN_FILE_PATH = Paths.get(CODESYNC_ROOT, "sequence_token.yml").toString();

    public static String DEFAULT_BRANCH = "default";
    public static String GIT_REPO = ".git";
    public static String SYNC_IGNORE = ".syncignore";
    public static String GIT_IGNORE = ".gitignore";
    public static String[] IGNORABLE_DIRECTORIES = new String[]{".git", "node_modules", ".DS_Store", ".idea"};
    public static String CURRENT_GIT_BRANCH_COMMAND = "git rev-parse --abbrev-ref HEAD";

    public static String MAGIC_STRING = "IntellijIdeaRulezzz";
    public static String DIR_CREATE_EVENT = "VfsEvent[create dir";
    public static String EMPTY_DIR_CREATE_EVENT = "VfsEvent[create (empty) dir";
    public static String FILE_COPY_EVENT = "VfsEvent[copy file";
    public static String FILE_CREATE_EVENT = "VfsEvent[create file";
    public static String FILE_DELETE_EVENT = "VfsEvent[deleted: file";
    public static String FILE_RENAME_EVENT = "VfsEvent[property(name) changed";

    public static String REGEX_REPLACE_LEADING_EXCAPED_EXCLAMATION = "/^\\!/";
    public static Integer DIFFS_PER_ITERATION = 50;

    public static String WEBSOCKET_ENDPOINT = String.format("ws://%s/v1/websocket", CODESYNC_DOMAIN);


    public static String API_ENDPOINT = String.format("%s/v1", CODESYNC_HOST);
    public static String API_INIT = String.format("%s/init", API_ENDPOINT);
    public static String API_USERS = String.format("%s/users", API_ENDPOINT);
    public static String FILES_API_ENDPOINT = String.format("%s/files", API_ENDPOINT);
    public static String API_HEALTHCHECK = String.format("%s/healthcheck", CODESYNC_HOST);
    public static String CODESYNC_AUTHORIZE_URL = String.format("%s/authorize", CODESYNC_HOST);
    public static String CODESYNC_LOGOUT_URL = String.format("%s/auth-logout", CODESYNC_HOST);
    public static String CODESYNC_UPDATE_REPO_URL = String.format("%s/repos", CODESYNC_HOST);

    public static String PLANS_URL = String.format("%s/plans", WEB_APP_URL);
    public static String REPO_PLAYBACK_LINK = String.format("%s/repos", WEB_APP_URL) + "/%s/playback";
    public static String FILE_PLAYBACK_LINK = String.format("%s/files", WEB_APP_URL) + "/%s/history";

    public static Integer DELAY_BETWEEN_BUFFER_TASKS = 5000;
    public static String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS z";
    public static String DIFF_SOURCE = "intellij";
    public static String CLIENT_LOGS_GROUP_NAME = "client-logs";

    public static String CONNECTION_ERROR_MESSAGE = "Error => Server is not available. Please try again in a moment";

    public static Integer FILE_SIZE_AS_COPY = 100;  // 100 bytes;
    public static double SEQUENCE_MATCHER_RATIO = 0.8; // 80% match ratio.

    public static Integer DIFF_SIZE_LIMIT = 16 * 1000 * 1000;

    public static String SYNC_IGNORE_COMMENT = "# CodeSync won't sync the files in the .syncignore. It follows same format as .gitignore.";

    public static final class Notification {
        private Notification() {
            // restrict instantiation
        }

        public static String YES = "Yes";
        public static String NO = "No";
        public static String SERVICE_NOT_AVAILABLE = "CodeSync Service is unavailable. Please try again in a moment.";
        public static String UPGRADE_PLAN = String.format("Upgrade your plan: %s", PLANS_URL);
        public static String PUBLIC_OR_PRIVATE = "Do you want to make the repo public? (You can change this later.)";

        public static String INIT_SUCCESS_MESSAGE = "Repo initialized successfully, your code will now be synced with CodeSync.";
        public static String INIT_ERROR_MESSAGE = "Repo initialization errored out, please try again later. If problem persists then contact support.";
        public static String INIT_FAILURE_MESSAGE = "Repo could not be initialized successfully, please try again later. If problem persists then contact support.";

        public static String REPO_SYNC_IN_PROGRESS_MESSAGE = "Repo '%s' is being synced with CodeSync.";
        public static String REPO_IN_SYNC_MESSAGE = "Repo '%s' is synced with CodeSync.";
        public static String REPO_ALREADY_IN_SYNC_MESSAGE = "Repo '%s' is already being synced with CodeSync.";

        public static String REPO_UNSYNC_CONFIRMATION = "Are you sure to continue? You won't be able to revert this!";
        public static String REPO_UNSYNCED = "Repo disconnected successfully";
        public static String REPO_UNSYNC_FAILED = "Could not unsync the repo";
    }
}
