package org.intellij.sdk.codesync.userInput;

import com.intellij.openapi.ui.DialogWrapper;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;

public class UserInputDialog extends DialogWrapper {
    String title, primaryMessage, promptMessage;

    public UserInputDialog(String title, String primaryMessage, String promptMessage){
        super(true); // use current window as parent
        this.title = title;
        this.primaryMessage = primaryMessage;
        this.promptMessage = promptMessage;

        setTitle(this.title);
        init();
    }

    public UserInputDialog(String title, String primaryMessage){
        super(true); // use current window as parent
        this.title = title;
        this.primaryMessage = primaryMessage;
        this.promptMessage = "";

        setTitle(this.title);
        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel dialogPanel = new JPanel(new BorderLayout());

        JLabel label = new JLabel(this.primaryMessage);
        JLabel promptMessage = new JLabel(this.promptMessage);
        dialogPanel.add(label, BorderLayout.PAGE_START);
        dialogPanel.add(promptMessage, BorderLayout.PAGE_END);

        return dialogPanel;
    }
}
