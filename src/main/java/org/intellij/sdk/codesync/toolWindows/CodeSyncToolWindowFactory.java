package org.intellij.sdk.codesync.toolWindows;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

public class CodeSyncToolWindowFactory implements ToolWindowFactory {
    /**
     * Create the tool window content.
     *
     * @param project    current project
     * @param toolWindow current tool window
     */
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        CodeSyncToolWindow codeSyncToolWindow = new CodeSyncToolWindow(toolWindow);
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(codeSyncToolWindow.getContent(), "CodeSync", false);
        toolWindow.getContentManager().addContent(content);
    }
}