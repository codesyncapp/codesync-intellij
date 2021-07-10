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
    public static String FILE_CREATE_EVENT = "VfsEvent[create file";
    public static String FILE_DELETE_EVENT = "VfsEvent[deleted: file";
    public static String FILE_RENAME_EVENT = "VfsEvent[property(name) changed";

    public static String REGEX_REPLACE_LEADING_EXCAPED_EXCLAMATION = "/^\\!/";
    public static Integer DIFFS_PER_ITERATION = 50;

    public static String CODESYNC_DOMAIN = "codesync-server.herokuapp.com";
    // public static String CODESYNC_DOMAIN = "127.0.0.1:8000";
    public static String CODESYNC_HOST = "https://codesync-server.herokuapp.com";
    // public static String CODESYNC_HOST = "http://127.0.0.1:8000";
    public static String CODESYNC_APP = "https://codesync.com";
    public static String WEBSOCKET_ENDPOINT = String.format("ws://%s/v1/websocket", CODESYNC_DOMAIN);
    public static String API_ENDPOINT = String.format("%s/v1", CODESYNC_HOST);
    public static String API_INIT = String.format("%s/init", API_ENDPOINT);
    public static String GET_USER = String.format("%s/users", API_ENDPOINT);
    public static String FILES_API_ENDPOINT = String.format("%s/files", API_ENDPOINT);
    public static String API_HEALTHCHECK = String.format("%s/healthcheck", CODESYNC_HOST);

    public static Integer DELAY_BETWEEN_BUFFER_TASKS = 5000;
    public static String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
    public static String DIFF_SOURCE = "intellij";
    public static String AWS_REGION = "us-east-1";
    public static String CLIENT_LOGS_GROUP_NAME = "client-logs";
}
