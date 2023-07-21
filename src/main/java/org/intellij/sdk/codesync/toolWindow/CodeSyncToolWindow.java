package org.intellij.sdk.codesync.toolWindow;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class CodeSyncToolWindow extends JPanel {

    private final JPanel contentPanel = new JPanel();
    private final JPanel leftPanel = new JPanel();
    private final JPanel rightPanel = new JPanel();

    public CodeSyncToolWindow() {
        contentPanel.setLayout(new BorderLayout(0, 20));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        leftPanel.setLayout(new BorderLayout());
        rightPanel.setLayout(new BorderLayout());

        leftPanel.setPreferredSize(new Dimension((int) (Toolkit.getDefaultToolkit().getScreenSize().width * 0.8), 0));
        rightPanel.setPreferredSize(new Dimension((int) (Toolkit.getDefaultToolkit().getScreenSize().width * 0.2), 0));

        contentPanel.add(leftPanel, BorderLayout.CENTER);
        contentPanel.add(rightPanel, BorderLayout.WEST);

        rightPanel.add(new CodeSyncMenu(), BorderLayout.CENTER);
    }

    public JPanel getContentPanel() {
        return contentPanel;
    }
}
