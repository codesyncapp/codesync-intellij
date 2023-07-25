package org.intellij.sdk.codesync.toolWindow;

import com.intellij.util.messages.MessageBus;
import org.intellij.sdk.codesync.eventBus.CodeSyncEventBus;
import org.intellij.sdk.codesync.utils.CommonUtils;

import javax.swing.*;
import java.awt.*;

public class CodeSyncToolWindow extends JPanel {

    private static JPanel contentPanel = new JPanel();
    private static JPanel rightPanel = new JPanel();
    private static JPanel leftPanel = new JPanel();

    public CodeSyncToolWindow() {
        contentPanel.setLayout(new BorderLayout(0, 20));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        rightPanel.setLayout(new BorderLayout());
        leftPanel.setLayout(new BorderLayout());

        rightPanel.setPreferredSize(new Dimension((int) (Toolkit.getDefaultToolkit().getScreenSize().width * 0.8), 0));
        leftPanel.setPreferredSize(new Dimension((int) (Toolkit.getDefaultToolkit().getScreenSize().width * 0.2), 0));

        contentPanel.add(rightPanel, BorderLayout.CENTER);
        contentPanel.add(leftPanel, BorderLayout.WEST);

        leftPanel.add(new CodeSyncMenu(), BorderLayout.CENTER);
    }

    public static void updateMenu(){
        leftPanel.removeAll();
        leftPanel.add(new CodeSyncMenu(), BorderLayout.CENTER);
        MessageBus messageBus = CommonUtils.getCurrentProject().getMessageBus();
        CodeSyncEventBus eventBus = messageBus.syncPublisher(CodeSyncEventBus.TOPIC);
        eventBus.onEvent(); // Notify subscribers about the event
    }

    public JPanel getContentPanel() {
        return contentPanel;
    }
}
