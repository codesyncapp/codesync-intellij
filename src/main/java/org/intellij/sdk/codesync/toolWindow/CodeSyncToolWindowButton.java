package org.intellij.sdk.codesync.toolWindow;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.intellij.sdk.codesync.eventBus.CodeSyncEventBus;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class CodeSyncToolWindowButton implements ToolWindowFactory, DumbAware {

    CodeSyncToolWindow toolWindowContent;

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        Content content = ContentFactory.SERVICE.getInstance().createContent(CodeSyncToolWindow.getContentPanel(), "", false);
        toolWindow.getContentManager().addContent(content);
        toolWindow.setSplitMode(false, null);

        // Register a listener to update the content whenever the event occurs
        project.getMessageBus().connect().subscribe(CodeSyncEventBus.TOPIC, () -> {
            SwingUtilities.invokeLater(() -> {
                // Perform any updates or changes to the CodeSyncToolWindow content here
                // Repaint the content panel to reflect the changes
                toolWindowContent.getContentPanel().revalidate();
                toolWindowContent.getContentPanel().repaint();
            });
        });
    }
}
