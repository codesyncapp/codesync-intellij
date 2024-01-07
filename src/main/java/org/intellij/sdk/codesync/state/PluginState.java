package org.intellij.sdk.codesync.state;

import com.intellij.openapi.project.Project;

public class PluginState {
    public Project project;
    public boolean isAuthenticated;
    public boolean isRepoInSync;
    public String repoPath;
    public String userEmail = null;
    public boolean syncInProcess = false;
    public boolean isAccountDeactivated = false;
}
