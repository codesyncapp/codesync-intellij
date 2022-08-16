package org.intellij.sdk.codesync.configuration;

public class ProdConfiguration implements Configuration {
    private static ProdConfiguration configuration = null;

    public String CODESYNC_DIR_NAME = ".codesync";
    public String CODESYNC_DOMAIN = "api.codesync.com";
    public String CODESYNC_HOST = "https://api.codesync.com";
    public String CODESYNC_APP = "https://www.codesync.com";
    public String WEB_APP_URL = "https://www.codesync.com";

    public static ProdConfiguration getInstance () {
        if (configuration == null) {
            configuration = new ProdConfiguration();
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
