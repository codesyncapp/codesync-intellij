package org.intellij.sdk.codesync.userInput;

import com.intellij.openapi.ui.DialogWrapper;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;

public class SignupRequestDialogWrapper extends DialogWrapper {
    String repoName;

    public SignupRequestDialogWrapper(){
        super(true); // use current window as parent
        this.repoName = repoName;
        setTitle("You Need to Authenticate!");
        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel dialogPanel = new JPanel(new BorderLayout());

        JLabel label = new JLabel(String.format(
                "To sync repo '%s', is not being synced with CodeSync.",
                this.repoName
        ));

        JLabel promptMessage = new JLabel("Do you want to proceed with authentication?");
        dialogPanel.add(label, BorderLayout.PAGE_START);
        dialogPanel.add(promptMessage, BorderLayout.PAGE_END);

        return dialogPanel;
    }
}
