package org.intellij.sdk.codesync;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import icons.CodeSyncIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class CodeSyncButton implements ToolWindowFactory, DumbAware {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        CodeSyncButtons toolWindowContent = new CodeSyncButtons(toolWindow);
        Content content = ContentFactory.SERVICE.getInstance().createContent(toolWindowContent.getContentPanel(), "", false);
        toolWindow.getContentManager().addContent(content);
        toolWindow.setSplitMode(false, null);
    }

    private static class CodeSyncButtons {

        private final JPanel contentPanel = new JPanel();
        private final JPanel leftPanel = new JPanel();
        private final JPanel rightPanel = new JPanel();

        private static final String CONNECT_REPO = "Connect Repo";

        public CodeSyncButtons(ToolWindow toolWindow) {
            contentPanel.setLayout(new BorderLayout(0, 20));
            contentPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

            leftPanel.setLayout(new BorderLayout());
            rightPanel.setLayout(new BorderLayout());

            leftPanel.setBackground(Color.BLACK);

            leftPanel.setPreferredSize(new Dimension((int) (Toolkit.getDefaultToolkit().getScreenSize().width * 0.8), 0));
            rightPanel.setPreferredSize(new Dimension((int) (Toolkit.getDefaultToolkit().getScreenSize().width * 0.2), 0));

            contentPanel.add(leftPanel, BorderLayout.CENTER);
            contentPanel.add(rightPanel, BorderLayout.WEST);

            JPanel controlsPanel = createControlsPanel(toolWindow);

            rightPanel.add(controlsPanel, BorderLayout.CENTER);
        }

        @NotNull
        private JPanel createControlsPanel(ToolWindow toolWindow) {
            JPanel controlsPanel = new JPanel();
            controlsPanel.setLayout(new GridBagLayout());

            JPanel buttonListPanel = new JPanel();
            buttonListPanel.setLayout(new BoxLayout(buttonListPanel, BoxLayout.Y_AXIS));

            JButton viewFilePlaybackButton = new JButton("View File Playback");
            JButton viewRepoPlaybackButton = new JButton("View Repo Playback");
            JButton viewDashboardButton = new JButton("View Dashboard");
            JButton connectToolWindowButton = new JButton(CONNECT_REPO);
            connectToolWindowButton.addActionListener(
                    e -> {
                            if(connectToolWindowButton.getText().equals(CONNECT_REPO)){
                                connectToolWindowButton.setText("Disconnect Repo");
                                toolWindow.setIcon(CodeSyncIcons.getCodeSyncIcon());
                            }else{
                                connectToolWindowButton.setText(CONNECT_REPO);
                                toolWindow.setIcon(AllIcons.Toolwindows.Problems);
                            }
                        }
                    );
            JButton logoutToolWindowButton = new JButton("Logout");
            logoutToolWindowButton.addActionListener(e -> toolWindow.hide(null));

            viewFilePlaybackButton.setAlignmentX(Component.CENTER_ALIGNMENT);
            viewRepoPlaybackButton.setAlignmentX(Component.CENTER_ALIGNMENT);
            viewDashboardButton.setAlignmentX(Component.CENTER_ALIGNMENT);
            connectToolWindowButton.setAlignmentX(Component.CENTER_ALIGNMENT);
            logoutToolWindowButton.setAlignmentX(Component.CENTER_ALIGNMENT);

            Dimension buttonSize = new Dimension(200, 50);
            viewFilePlaybackButton.setMaximumSize(buttonSize);
            viewRepoPlaybackButton.setMaximumSize(buttonSize);
            viewDashboardButton.setMaximumSize(buttonSize);
            connectToolWindowButton.setMaximumSize(buttonSize);
            logoutToolWindowButton.setMaximumSize(buttonSize);

            buttonListPanel.add(viewFilePlaybackButton);
            buttonListPanel.add(viewRepoPlaybackButton);
            buttonListPanel.add(viewDashboardButton);
            buttonListPanel.add(connectToolWindowButton);
            buttonListPanel.add(logoutToolWindowButton);

            buttonListPanel.setAlignmentY(Component.CENTER_ALIGNMENT);

            controlsPanel.add(buttonListPanel);

            return controlsPanel;
        }

        public JPanel getContentPanel() {
            return contentPanel;
        }
    }
}
