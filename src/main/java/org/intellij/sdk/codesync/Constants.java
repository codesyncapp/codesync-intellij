package org.intellij.sdk.codesync;

import com.intellij.openapi.application.ApplicationInfo;
import org.intellij.sdk.codesync.configuration.Configuration;
import org.intellij.sdk.codesync.configuration.ConfigurationFactory;
import org.intellij.sdk.codesync.utils.ProjectUtils;

import java.nio.file.Paths;
import java.time.ZoneId;

public final class Constants {
    public static final Configuration configuration = ConfigurationFactory.getConfiguration();

    private Constants() {
        // restrict instantiation
    }
    // Values that can be overridden via environment based Configuration should go here.
    private static final String CODESYNC_DIR_NAME = configuration.getCodeSyncDirName();
    private static final String CODESYNC_DOMAIN = configuration.getCodeSyncDomain();
    private static final String CODESYNC_HOST = configuration.getCodeSyncHost();
    private static final String CODESYNC_APP = configuration.getCodeSyncAppURL();
    private static final String WEB_APP_URL = configuration.getCodeSyncWebAppURL();

    public static final String USER_HOME = System.getProperty("user.home");
    public static final String CODESYNC_ROOT = Paths.get(USER_HOME, CODESYNC_DIR_NAME).toString();
    public static final String DIFFS_REPO = Paths.get(CODESYNC_ROOT, ".diffs", ".intellij").toString();
    public static final String LOCKS_FILE_DIR = Paths.get(CODESYNC_ROOT, ".locks").toString();
    public static final String PROJECT_LOCK_FILE = Paths.get(LOCKS_FILE_DIR, "project_locks.yml").toString();
    public static final String POPULATE_BUFFER_LOCK_FILE = Paths.get(LOCKS_FILE_DIR, "populate_buffer_locks.yml").toString();
    public static final String HANDLE_BUFFER_LOCK_FILE = Paths.get(LOCKS_FILE_DIR, "handle_buffer_locks.yml").toString();
    public static final String ORIGINALS_REPO = Paths.get(CODESYNC_ROOT, ".originals").toString();
    public static final String DELETED_REPO = Paths.get(CODESYNC_ROOT, ".deleted").toString();
    public static final String SHADOW_REPO = Paths.get(CODESYNC_ROOT, ".shadow").toString();
    public static final String CONFIG_PATH = Paths.get(CODESYNC_ROOT, "config.yml").toString();
    public static final String S3_UPLOAD_QUEUE_DIR = Paths.get(CODESYNC_ROOT, ".s3_uploader").toString();

    //TODO: User file will be removed
    public static final String USER_FILE_PATH = Paths.get(CODESYNC_ROOT, "user.yml").toString();
    public static final String ALERTS_FILE_PATH = Paths.get(CODESYNC_ROOT, "alerts.yml").toString();
    public static final String SEQUENCE_TOKEN_FILE_PATH = Paths.get(CODESYNC_ROOT, "sequence_token.yml").toString();
    public static final String DEFAULT_BRANCH = "default";
    public static final String GIT_REPO = ".git";
    public static final String SYNC_IGNORE = ".syncignore";
    public static final String GIT_IGNORE = ".gitignore";
    public static final String[] IGNORABLE_DIRECTORIES = new String[]{".git", "node_modules", ".DS_Store", ".idea"};
    public static final String CURRENT_GIT_BRANCH_COMMAND = "git rev-parse --abbrev-ref HEAD";
    public static final Integer DELAY_BETWEEN_BUFFER_TASKS = 5000;
    public static final Integer DELAY_BETWEEN_ACTIVITY_ALERT_TASKS = 10 * 60 * 1000; // 10 minutes wait.
    public static final Integer DELAY_BETWEEN_BUFFER_TASKS_IN_SECONDS = 5;

    //Database queries and strings
    public static final String DATABASE_PATH = Paths.get(CODESYNC_ROOT, "codesyncdb.db").toString();
    public static final String CONNECTION_STRING = "jdbc:sqlite:" + DATABASE_PATH;

    public static final String DEFAULT_TIMEZONE = ZoneId.systemDefault().getId();
    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS z";
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String DATE_TIME_FORMAT_WITHOUT_TIMEZONE = "yyyy-MM-dd HH:mm:ss.SSS";
    public static final String IDE_NAME = ApplicationInfo.getInstance().getVersionName();
    public static final String DIFF_SOURCE = "intellij";
    public static final String PLUGIN_VERSION = ProjectUtils.getPluginVersion();
    public static final String GA4_PARAMS = String.format("utm_medium=plugin&utm_source=%s&utm_source_platform=%s", DIFF_SOURCE, IDE_NAME);

    public static final String MAGIC_STRING = "IntellijIdeaRulezzz";
    public static final String DIR_CREATE_EVENT = "VfsEvent[create dir";
    public static final String EMPTY_DIR_CREATE_EVENT = "VfsEvent[create (empty) dir";
    public static final String FILE_COPY_EVENT = "VfsEvent[copy file";
    public static final String FILE_CREATE_EVENT = "VfsEvent[create file";
    public static final String FILE_DELETE_EVENT = "VfsEvent[deleted: file";
    public static final String FILE_RENAME_EVENT = "VfsEvent[property(name) changed";

    public static final String REGEX_REPLACE_LEADING_ESCAPED_EXCLAMATION = "/^\\!/";
    public static final Integer DIFFS_PER_ITERATION = 50;

    public static final String WEBSOCKET_ENDPOINT = configuration.getCodeSyncWebsocketURL();

    public static final String API_ENDPOINT = String.format("%s/v1", CODESYNC_HOST);
    public static final String API_INIT = String.format("%s/init?source=%s&v=%s", API_ENDPOINT, DIFF_SOURCE, PLUGIN_VERSION);
    public static final String API_USERS = String.format("%s/users?&source=%s&v=%s", API_ENDPOINT, DIFF_SOURCE, PLUGIN_VERSION);
    public static final String CODESYNC_REPO_URL = String.format("%s/repos", API_ENDPOINT);
    public static final String FILES_API_ENDPOINT = String.format("%s/files?source=%s&v=%s", API_ENDPOINT, DIFF_SOURCE, PLUGIN_VERSION);
    public static final String TEAM_ACTIVITY_ENDPOINT = String.format("%s/team_activity?tz=%s&source=%s&v=%s", API_ENDPOINT, DEFAULT_TIMEZONE, DIFF_SOURCE, PLUGIN_VERSION);
    public static final String USER_SUBSCRIPTION_ENDPOINT = String.format("%s/pricing/subscription", API_ENDPOINT);

    public static final String API_HEALTHCHECK = String.format("%s/healthcheck?source=%s&v=%s", CODESYNC_HOST, DIFF_SOURCE, PLUGIN_VERSION);
    public static final String CODESYNC_AUTHORIZE_URL = String.format("%s/authorize", CODESYNC_HOST);
    public static final String CODESYNC_LOGOUT_URL = String.format("%s/auth-logout", CODESYNC_HOST);

    public static final String WEBAPP_DASHBOARD_URL = String.format("%s/?%s", WEB_APP_URL, GA4_PARAMS);
    public static final String CODESYNC_PRICING_URL = String.format("%s/pricing", WEB_APP_URL);
    public static final String PLANS_URL = String.format("%s/plans?%s", WEB_APP_URL, GA4_PARAMS);
    public static final String REPO_PLAYBACK_LINK = String.format("%s/repos", WEB_APP_URL) + "/%s/playback?" + GA4_PARAMS;
    public static final String FILE_PLAYBACK_LINK = String.format("%s/files", WEB_APP_URL) + "/%s/history?" + GA4_PARAMS;
    public static final String SETTINGS_PAGE_URL = String.format("%s/settings", WEB_APP_URL);


    public static final String CLIENT_LOGS_GROUP_NAME = configuration.getLogGroupName();
    public static final String PLUGIN_USER = configuration.getPluginUser();
    public static final String PLUGIN_USER_LOG_STREAM = configuration.getPluginUserLogStream();
    public static final String PLUGIN_USER_ACCESS_KEY = configuration.getPluginUserAccessKey();
    public static final String PLUGIN_USER_SECRET_KEY = configuration.getPluginUserSecretKey();

    public static final String CONNECTION_ERROR_MESSAGE = "Error => Server is not available. Please try again in a moment";

    public static final Integer FILE_SIZE_AS_COPY = 100;  // 100 bytes;
    public static final double SEQUENCE_MATCHER_RATIO = 0.8; // 80% match ratio.

    public static final Integer DIFF_SIZE_LIMIT = 15 * 1000 * 1000;

    public static final String SYNC_IGNORE_COMMENT = "# CodeSync won't sync the files in the .syncignore. It follows same format as .gitignore.";

    // Locks used by other IDEs alongside intellij.
    public static final String DIFFS_DAEMON_LOCK_KEY = "send_diffs_intellij";
    public static final String POPULATE_BUFFER_DAEMON_LOCK_KEY = "populate_buffer";
    public static final String ACTIVITY_ALERT_DAEMON_LOCK_KEY = "activity_alerts_daemon";
    public static final String PRICING_ALERT_LOCK_KEY = "pricing_alert";
    public static final String ACTIVITY_ALERT_LOCK_KEY = "activity_alerts";
    public static final String VIEW_ACTIVITY = "View Activity";
    public static final String REMIND_LATER = "Remind Later";
    public static final String SKIP_TODAY = "Skip Today";

    public static final String S3_FILE_UPLOAD_LOCK_KEY = "s3_file_upload";

    public static final Integer S3_UPLOAD_TIMEOUT = 5 * 60 * 1000; // 1000 is for ms
    public static final Integer S3_UPLOAD_RETRY_AFTER = 5 * 60 * 1000; // 1000 is for ms

    public static final class PlatformIdentifier {
        private PlatformIdentifier() {
            // restrict instantiation
        }

        public static final String WINDOWS = "windows";
        public static final String MAC_OS = "mac";
        public static final String UNIX = "aix";
        public static final String SOLARIS = "sunos";
    }

    public static final class Notification {
        private Notification() {
            // restrict instantiation
        }

        public static final String CODESYNC_NOTIFICATION_GROUP = "CodeSync Notifications";
        public static final String DEFAULT_TITLE = "CodeSync";
        public static final String CODESYNC_ERROR_TITLE = "CodeSync Error";
        public static final String CODESYNC_WARNING_TITLE = "CodeSync Warning";
        public static final String CODESYNC_INFORMATION_TITLE = "CodeSync Information";

        public static final String YES = "Yes";
        public static final String NO = "No";
        public static final String SERVICE_NOT_AVAILABLE = "CodeSync Service is unavailable. Please try again in a moment.";
        public static final String UPGRADE_PLAN = String.format("Upgrade your plan: %s", PLANS_URL);

        public static final String INIT_SUCCESS_MESSAGE = "Repo initialized successfully, your code will now be synced with CodeSync.";
        public static final String INIT_ERROR_MESSAGE = "Repo initialization errored out, please try again later. If problem persists then contact support.";
        public static final String INIT_FAILURE_MESSAGE = "Repo could not be initialized successfully, please try again later. If problem persists then contact support.";

        public static final String BRANCH_INIT_SUCCESS_MESSAGE = "Branch '%s' initialized successfully, your new branch will now be synced with CodeSync.";
        public static final String BRANCH_INIT_ERROR_MESSAGE = "Branch '%s' initialization errored out, please try again later. If problem persists then contact support.";
        public static final String BRANCH_INIT_FAILURE_MESSAGE = "Branch '%s' could not be initialized successfully, please try again later. If problem persists then contact support.";

        public static final String REPO_SYNC_IN_PROGRESS_MESSAGE = "Repo '%s' is being synced with CodeSync.";
        public static final String REPO_IN_SYNC_MESSAGE = "Repo '%s' is in sync with CodeSync.";
        public static final String REPO_ALREADY_IN_SYNC_MESSAGE = "Repo '%s' is already being synced with CodeSync.";

        public static final String LOGIN_REQUIRED_FOR_SYNC_MESSAGE = "You need to login to sync repo. Please login and then try again.";

        public static final String REPO_UNSYNC_CONFIRMATION = "Are you sure to continue? Your changes won't be synced if you disconnect!";
        public static final String REPO_UNSYNCED = "Repo disconnected successfully";
        public static final String REPO_UNSYNC_FAILED = "Could not unsync the repo";
        public static final String REPO_SYNC_ACTION_FAILED = "Could not perform the action, please try again.";

        public static final String REPO_RECONNECTED = "Repo reconnected successfully";
        public static final String REPO_RECONNECT_FAILED = "Could not reconnect the repo";

        // Pricing plan related notification messages
        public static final String UPGRADE = "CodeSync | Free tier limit reached";
        public static final String PRICING_LIMIT_REACHED_MESSAGE = "We hope you've found CodeSync useful! You've hit the limit of the Free tier.";
        public static final String UPGRADE_PRICING_PLAN = "To continue, please upgrade your plan.";
        public static final String UPGRADE_ORG_PRICING_PLAN = "To continue, please sign your organization up for the Team plan.";
        public static final String TRY_PRO_FOR_FREE = "To continue, please sign up for a 30-day free trial of Pro.";
        public static final String TRY_TEAM_PLAN_FOR_FREE = "To continue, please sign your organization up for a 30-day free trial of the Team plan.";

        // Free plan related notification messages
        public static final String PRIVATE_REPO_COUNT_REACHED = "In the Free Plan, you can have just one private repository.";

        // Team alert related notification messages
        public static final String ACTIVITY_ALERT_HEADER_MESSAGE = "CodeSync | Check your activity!";
        public static final String ACTIVITY_ALERT_MESSAGE = "Let’s take a minute to review today’s coding!";
        public static final String ACTIVITY_ALERT_SECONDARY_MESSAGE =
            "If you are not available right now then you can either skip for today or review later by clicking the correct button below.";
        public static final String TEAM_ACTIVITY_ALERT_HEADER_MESSAGE = "CodeSync | Check your team's activity!";
        public static final String TEAM_ACTIVITY_ALERT_MESSAGE = "Hope you had a great day! It's time to get in sync with your team's code.";
        public static final String TEAM_ACTIVITY_ALERT_SECONDARY_MESSAGE =
            "If you are not available right now then you can either skip for today or review later by clicking the correct button below.";

        public static final String ACCOUNT_DEACTIVATED = "Your account has been deactivated. Please click 'Reactivate Account' below to resume syncing.";
        public static final String ACCOUNT_REACTIVATE_BUTTON = "Reactivate Account";
        public static final String REACTIVATED_SUCCESS = "Successfully reactivated your account";
    }


    public static class NotificationButton {
        private NotificationButton() {
            // restrict instantiation
        }

        public static final String TRY_PRO_FOR_FREE = "Try Pro for Free";
        public static final String TRY_TEAM_FOR_FREE = "Try Team for Free";
        public static final String UPGRADE_PLAN = "Upgrade Plan";
        public static final String UPGRADE_TO_TEAM = "Upgrade to Team Plan";
    }
    public static final class LogMessageType {
        private LogMessageType() {
            // restrict instantiation
        }
        public static final String CRITICAL = "CRITICAL";
        public static final String ERROR = "ERROR";
        public static final String WARNING = "WARNING";
        public static final String INFO = "INFO";
        public static final String DEBUG = "DEBUG";
    }

    public static final class LockFileType {
        private LockFileType() {
            // restrict instantiation
        }

        public static final String PROJECT_LOCK = "project";
        public static final String POPULATE_BUFFER_LOCK = "populate_buffer";
        public static final String HANDLE_BUFFER_LOCK = "handle_buffer";
    }

    public static final class ErrorCodes {
        private ErrorCodes() {
            // restrict instantiation
        }

        public static final int INVALID_USAGE = 400;
        public static final int REPO_SIZE_LIMIT_REACHED = 402;
        public static final int DIFFS_LIMIT_REACHED = 402;
        public static final int ACCOUNT_DEACTIVATED = 403;
    }

    public static final class CustomErrorCodes {
        /*
            Custom error codes for the case: Free Plan restrictions
        */
        private CustomErrorCodes(){
            // restrict instantiation
        }

        public static final int IS_FROZEN_REPO = 4000;
        public static final int PRIVATE_REPO_COUNT_LIMIT_REACHED = 4006;
    }
}


