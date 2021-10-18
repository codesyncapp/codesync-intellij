package org.intellij.sdk.codesync.configuration;

public interface Configuration {
    public abstract String getCodeSyncDirName();
    public abstract String getCodeSyncDomain();
    public abstract String getCodeSyncHost();
    public abstract String getCodeSyncAppURL();
    public abstract String getCodeSyncWebAppURL();
}
