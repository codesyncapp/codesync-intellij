package org.intellij.sdk.codesync.ui.dialogs;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.intellij.sdk.codesync.Constants.Notification;
import org.intellij.sdk.codesync.alerts.ActivityAlerts;
import org.intellij.sdk.codesync.utils.CommonUtils;
import org.jdesktop.swingx.JXLabel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class RepoPublicPrivateDialog extends DialogWrapper {

    String message, title, makeItPublicButtonText = "Make it Public", keepItPrivateButtonText = "Keep it Private";



    public RepoPublicPrivateDialog(String title, String message, Project project) {
        super(project);

        this.title = title;
        this.message = message;

        setTitle(this.title);
        getWindow().addWindowListener(
                new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        System.out.println(e.toString());
                    }
                }
        );
    }

    public void show() {
        CommonUtils.invokeAndWait(
                () -> {
                    init();
                    super.show();
                    return CANCEL_EXIT_CODE;
                },
                ModalityState.defaultModalityState()
        );
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel dialogPanel = new JPanel(new BorderLayout());

        String htmlMessage = String.format(
                "<html><br/><p>%s</p><br/></html>", this.message
        );
        JXLabel label = new JXLabel(htmlMessage);
        label.setMaxLineSpan(200);
        label.setLineWrap(true);
        label.setPreferredSize(new Dimension(500, Integer.MAX_VALUE));
        dialogPanel.add(label, BorderLayout.LINE_START);

        return dialogPanel;
    }

    @Override
    protected Action @NotNull [] createActions() {
        return new Action[]{
                new AbstractAction(makeItPublicButtonText) {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if(isEnabled()){
                            System.out.println("Make public!");
                            close(OK_EXIT_CODE);
                        }
                    }
                },
                new AbstractAction(keepItPrivateButtonText) {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if(isEnabled()){
                            System.out.println("Keep private!");
                            close(CANCEL_EXIT_CODE);
                        }
                    }
                }
        };
    }

}
