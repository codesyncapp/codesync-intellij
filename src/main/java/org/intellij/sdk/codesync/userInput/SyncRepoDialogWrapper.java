package org.intellij.sdk.codesync.userInput;

import com.intellij.openapi.ui.DialogWrapper;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;

public class SyncRepoDialogWrapper extends DialogWrapper {
    String repoName;

    public SyncRepoDialogWrapper(String repoName){
        super(true); // use current window as parent
        this.repoName = repoName;
        setTitle(String.format("'%s' Is not Being Synced!", this.repoName));
        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel dialogPanel = new JPanel(new BorderLayout());

        JLabel label = new JLabel(String.format(
                "The '%s' is not being synced with CodeSync.",
                this.repoName
        ));

        JLabel promptMessage = new JLabel("Do you want to enable syncing of this repo?");
        dialogPanel.add(label, BorderLayout.PAGE_START);
        dialogPanel.add(promptMessage, BorderLayout.PAGE_END);

        return dialogPanel;
    }
}
