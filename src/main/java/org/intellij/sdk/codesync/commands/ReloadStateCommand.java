package org.intellij.sdk.codesync.commands;


import com.intellij.openapi.project.Project;
import org.intellij.sdk.codesync.state.StateUtils;

public class ReloadStateCommand implements Command {
    Project project;

    public ReloadStateCommand(Project project) {
        this.project = project;
    }

    public void execute() {
        // Execute reload state function.s
        StateUtils.reloadState(project);
    }
}
