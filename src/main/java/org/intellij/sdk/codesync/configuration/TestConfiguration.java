package org.intellij.sdk.codesync.configuration;

public class TestConfiguration implements Configuration {
    private static TestConfiguration configuration = null;

    public String CODESYNC_DIR_NAME = ".codesync-test";
    public String CODESYNC_DOMAIN = "example.com";
    public String CODESYNC_HOST = "https://example.com";
    public String CODESYNC_APP = "https://web.example.com";
    public String WEB_APP_URL = "https://web.example.com";

    public static TestConfiguration getInstance () {
        if (configuration == null) {
            configuration = new TestConfiguration();
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
