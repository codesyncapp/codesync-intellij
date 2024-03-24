package org.intellij.sdk.codesync.configuration;

import com.intellij.openapi.application.ApplicationInfo;
import org.intellij.sdk.codesync.utils.FileUtils;
import org.intellij.sdk.codesync.utils.ProjectUtils;
import org.json.simple.JSONObject;

public class DevConfiguration implements Configuration {
    private static DevConfiguration configuration = null;

    public String CODESYNC_DIR_NAME = ".codesync-dev";
    public String CODESYNC_DOMAIN = "127.0.0.1:8000";
    public String CODESYNC_HOST = "http://127.0.0.1:8000";
    public String CODESYNC_WEBSOCKET = String.format("ws://%s/v2/websocket", CODESYNC_DOMAIN);
    public String CODESYNC_APP = "http://localhost:3000";
    public String WEB_APP_URL = "http://localhost:3000";

    public String LOG_GROUP_NAME = "client-logs-dev";
    public String PLUGIN_USER = "codesync-dev-plugin-user";
    public String PLUGIN_USER_LOG_STREAM = "codesync-dev-common-logs";
    public String CREDENTIALS_URL = "https://codesync-public.s3.amazonaws.com/plugin-dev-user.json";

    // These 2 values will be lazy-loaded.
    public String PLUGIN_USER_ACCESS_KEY = null;
    public String PLUGIN_USER_SECRET_KEY = null;

    public static DevConfiguration getInstance () {
        if (configuration == null) {
            configuration = new DevConfiguration();
        }
        return configuration;
    }

    /*
    Fetch credentials from the server and populate attributes, ignore the errors.
     */
    private void fetchCredentials() {
        JSONObject jsonObject = FileUtils.readURLToJson(this.CREDENTIALS_URL);
        if (jsonObject != null) {
            this.PLUGIN_USER_ACCESS_KEY = (String) jsonObject.getOrDefault("IAM_ACCESS_KEY", null);
            this.PLUGIN_USER_SECRET_KEY = (String) jsonObject.getOrDefault("IAM_SECRET_KEY", null);
        }
    }

    @Override
    public String getCodeSyncDirName() {
        return CODESYNC_DIR_NAME;
    }

    @Override
    public String getCodeSyncDomain() {
        return CODESYNC_DOMAIN;
    }

    @Override
    public String getCodeSyncHost() {
        return CODESYNC_HOST;
    }

    @Override
    public String getCodeSyncAppURL() {
        return CODESYNC_APP;
    }

    @Override
    public String getCodeSyncWebsocketURL() { return this.CODESYNC_WEBSOCKET; }

    @Override
    public String getCodeSyncWebAppURL() {
        return WEB_APP_URL;
    }

    @Override
    public String getPluginUser() {
        return this.PLUGIN_USER;
    }

    @Override
    public String getPluginUserLogStream() {
        return this.PLUGIN_USER_LOG_STREAM;
    }

    @Override
    public String getPluginUserAccessKey() {
        // Lazy load values, we are using lazy loading because these values are used only for tiny set of use cases
        if (this.PLUGIN_USER_ACCESS_KEY == null) {
            this.fetchCredentials();
        }
        return this.PLUGIN_USER_ACCESS_KEY;
    }

    @Override
    public String getPluginUserSecretKey() {
        // Lazy load values, we are using lazy loading because these values are used only for tiny set of use cases
        if (this.PLUGIN_USER_SECRET_KEY == null) {
            this.fetchCredentials();
        }
        return this.PLUGIN_USER_SECRET_KEY;
    }

    @Override
    public String getIDEName() {
        return ApplicationInfo.getInstance().getVersionName();
    }

    @Override
    public String getPluginVersion() {
        return ProjectUtils.getPluginVersion();
    }

    @Override
    public String getLogGroupName() {
        return LOG_GROUP_NAME;
    }
}
