package org.intellij.sdk.codesync.ui.toolWindows;

import com.intellij.openapi.wm.ToolWindow;
import org.intellij.sdk.codesync.codeSyncSetup.CodeSyncSetup;

import javax.swing.*;

public class CodeSyncToolWindow {
    private JPanel toolWindowContent;
    private JButton continueButton;
    private JTextPane youCanClickTheTextPane;


    public CodeSyncToolWindow(ToolWindow toolWindow) {

        continueButton.addActionListener(e -> {
            CodeSyncSetup.executeResumeUploadCommand();
            toolWindow.hide(null);
        });
    }


    public JPanel getContent() {
        return toolWindowContent;
    }
}
