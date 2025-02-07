package org.intellij.sdk.codesync.ui.dialogs;

import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.intellij.sdk.codesync.CodeSyncLogger;
import org.intellij.sdk.codesync.utils.CommonUtils;
import org.jdesktop.swingx.JXLabel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sun.tools.jconsole.Messages;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import static org.intellij.sdk.codesync.Constants.REPO_IS_PERSONAL;

public class RepoOrgSelectorDialog extends DialogWrapper {

    private final List<String> orgNames;
    private String selectedOrg = null;
    String message = "Would you like to add this repository to an organization?";
    String title = "Add repository to an organization or keep it personal?";

    public RepoOrgSelectorDialog(Project project, List<String> orgNames) {
        super(project);
        setTitle(this.title);
        this.orgNames = orgNames;
        getWindow().addWindowListener(
                new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        CodeSyncLogger.logConsoleMessage(e.toString());
                    }
                }
        );
    }

    public void show() {
        CommonUtils.invokeAndWait(
                () -> {
                    init();
                    super.show();
                    return selectedOrg;
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
        List<Action> actions = new ArrayList<>();
        // Generate an action for each org name
        for (String orgName : this.orgNames) {
            actions.add(new AbstractAction(orgName) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    selectedOrg = orgName;
                    close(OK_EXIT_CODE);
                }
            });
        }

        // Add a cancel button
        actions.add(new AbstractAction(REPO_IS_PERSONAL) {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectedOrg = REPO_IS_PERSONAL;
                if (isEnabled()) {
                    close(CANCEL_EXIT_CODE);
                }
            }
        });
        // Add a cancel button
        actions.add(new AbstractAction(Messages.CANCEL) {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectedOrg = Messages.CANCEL;
                close(CANCEL_EXIT_CODE);
            }
        });
        return actions.toArray(new Action[0]);
    };

    public String getSelectedOrg() {
        return selectedOrg;
    }
}
