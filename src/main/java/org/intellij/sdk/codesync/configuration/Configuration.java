package org.intellij.sdk.codesync.configuration;

public interface Configuration {
    public abstract String getCodeSyncDirName();
    public abstract String getCodeSyncDomain();
    public abstract String getCodeSyncHost();
    public abstract String getCodeSyncAppURL();
    public abstract String getCodeSyncWebsocketURL();
    public abstract String getCodeSyncWebAppURL();
    public abstract String getLogGroupName();
    public abstract String getPluginUser();
    public abstract String getPluginUserLogStream();
    public abstract String getPluginUserAccessKey();
    public abstract String getPluginUserSecretKey();
    public abstract String getIDEName();
    public abstract String getPluginVersion();
}
