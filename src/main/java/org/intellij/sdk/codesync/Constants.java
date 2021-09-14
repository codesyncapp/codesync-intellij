package org.intellij.sdk.codesync;

public final class Constants {

    private Constants() {
        // restrict instantiation
    }

    public static String expanduser(String path) {
        String user = System.getProperty("user.home");
        return path.replaceFirst("~", user);
    }

    public static String CODESYNC_ROOT = Constants.expanduser("~/.codesync");
    public static String DIFFS_REPO = String.format("%s/.diffs/.intellij", CODESYNC_ROOT);
    public static String ORIGINALS_REPO = String.format("%s/.originals", CODESYNC_ROOT);
    public static String DELETED_REPO = String.format("%s/.deleted", CODESYNC_ROOT);
    public static String SHADOW_REPO = String.format("%s/.shadow", CODESYNC_ROOT);
    public static String CONFIG_PATH = String.format("%s/config.yml", CODESYNC_ROOT);
    public static String USER_FILE_PATH = String.format("%s/user.yml", CODESYNC_ROOT);
    public static String SEQUENCE_TOKEN_FILE_PATH = String.format("%s/sequence_token.yml", CODESYNC_ROOT);

    public static String DEFAULT_BRANCH = "default";
    public static String GIT_REPO = ".git";
    public static String SYNC_IGNORE = ".syncignore";
    public static String GIT_IGNORE = ".gitignore";
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

    public static String CODESYNC_DOMAIN = "codesync-server.herokuapp.com";
//     public static String CODESYNC_DOMAIN = "127.0.0.1:8000";
    public static String CODESYNC_HOST = "https://codesync-server.herokuapp.com";
//     public static String CODESYNC_HOST = "http://127.0.0.1:8000";
    public static String CODESYNC_APP = "https://codesync.com";
    public static String WEBSOCKET_ENDPOINT = String.format("ws://%s/v1/websocket", CODESYNC_DOMAIN);

//    public static String WEB_APP_URL = "http://localhost:3000";
    public static String WEB_APP_URL = "https://www.codesync.com";

    public static String API_ENDPOINT = String.format("%s/v1", CODESYNC_HOST);
    public static String API_INIT = String.format("%s/init", API_ENDPOINT);
    public static String API_USERS = String.format("%s/users", API_ENDPOINT);
    public static String FILES_API_ENDPOINT = String.format("%s/files", API_ENDPOINT);
    public static String API_HEALTHCHECK = String.format("%s/healthcheck", CODESYNC_HOST);
    public static String CODESYNC_AUTHORIZE_URL = String.format("%s/authorize", CODESYNC_HOST);
    public static String CODESYNC_LOGOUT_URL = String.format("%s/auth-logout", CODESYNC_HOST);

    public static String PLANS_URL = String.format("%s/plans", WEB_APP_URL);

    public static Integer DELAY_BETWEEN_BUFFER_TASKS = 5000;
    public static String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
    public static String DIFF_SOURCE = "intellij";
    public static String CLIENT_LOGS_GROUP_NAME = "client-logs";

    public static String CONNECTION_ERROR_MESSAGE = "Error => Server is not available. Please try again in a moment";

    public static Integer FILE_SIZE_AS_COPY = 100;  // 100 bytes;
    public static double SEQUENCE_MATCHER_RATIO = 0.8; // 80% match ratio.
    public static String DATETIME_FORMAT = "UTC:yyyy-mm-dd HH:MM:ss.l";

    public static final class Notification {
        private Notification() {
            // restrict instantiation
        }

        public static String YES = "Yes";
        public static String NO = "No";
        public static String SERVICE_NOT_AVAILABLE = "CodeSync Service is unavailable. Please try again in a moment.";
        public static String UPGRADE_PLAN = String.format("Upgrade your plan: %s", PLANS_URL);
        public static String PUBLIC_OR_PRIVATE = "Do you want to make the repo public?";
        public static String ERROR_SYNCING_REPO = "Error syncing repo.";
    }
}
