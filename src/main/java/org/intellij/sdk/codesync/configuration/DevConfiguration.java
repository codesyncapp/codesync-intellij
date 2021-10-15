package org.intellij.sdk.codesync.configuration;

public class DevConfiguration implements Configuration {
    private static DevConfiguration configuration = null;

    public String CODESYNC_DIR_NAME = ".codesync-dev";
    public String CODESYNC_DOMAIN = "127.0.0.1:8000";
    public String CODESYNC_HOST = "http://127.0.0.1:8000";
    public String CODESYNC_APP = "http://localhost:3000";
    public String WEB_APP_URL = "http://localhost:3000";

    public static DevConfiguration getInstance () {
        if (configuration == null) {
            configuration = new DevConfiguration();
        }
        return configuration;
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
    public String getCodeSyncWebAppURL() {
        return WEB_APP_URL;
    }
}
