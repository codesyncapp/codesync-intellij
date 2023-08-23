package org.intellij.sdk.codesync.toolWindow;

import com.intellij.util.messages.MessageBus;
import org.intellij.sdk.codesync.eventBus.CodeSyncEventBus;
import org.intellij.sdk.codesync.utils.CommonUtils;

import javax.swing.*;
import java.awt.*;

public class CodeSyncToolWindow {

    private static JPanel contentPanel = new JPanel();
    private static JPanel rightPanel = new JPanel();
    private static JPanel leftPanel = new JPanel();

    public static void createToolWindow() {
        contentPanel.setLayout(new BorderLayout(0, 20));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        rightPanel.setLayout(new BorderLayout());
        leftPanel.setLayout(new BorderLayout());

        rightPanel.setPreferredSize(new Dimension((int) (Toolkit.getDefaultToolkit().getScreenSize().width * 0.7), 0));
        leftPanel.setPreferredSize(new Dimension((int) (Toolkit.getDefaultToolkit().getScreenSize().width * 0.3), 0));

        contentPanel.add(rightPanel, BorderLayout.CENTER);
        contentPanel.add(leftPanel, BorderLayout.WEST);

        rightPanel.add(new RightSide());
        leftPanel.add(new ButtonList(), BorderLayout.CENTER);
    }

    public static void updateMenu(){
        leftPanel.removeAll();
        leftPanel.add(new CodeSyncMenu(), BorderLayout.CENTER);
        MessageBus messageBus = CommonUtils.getCurrentProject().getMessageBus();
        CodeSyncEventBus eventBus = messageBus.syncPublisher(CodeSyncEventBus.TOPIC);
        eventBus.onEvent(); // Notify subscribers about the event
    }

    public static JPanel getContentPanel() {
        createToolWindow();
        return contentPanel;
    }
}
