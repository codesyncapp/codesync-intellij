package org.intellij.sdk.codesync.ui.dialogs;

import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.intellij.sdk.codesync.CodeSyncLogger;
import org.intellij.sdk.codesync.utils.CommonUtils;
import org.jdesktop.swingx.JXLabel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import static org.intellij.sdk.codesync.Constants.NotificationButton.SKIP_THIS_STEP;

public class RepoTeamSelectorDialog extends DialogWrapper {

    private final List<String> teamNames;
    private String selectedTeam = null;
    String message = "Which team should this repository be added to?";
    String title = "Add repository to your team";

    public RepoTeamSelectorDialog(Project project, List<String> teamNames) {
        super(project);
        setTitle(this.title);
        this.teamNames = teamNames;
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
                    return selectedTeam;
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
        for (String teamName : this.teamNames) {
            actions.add(new AbstractAction(teamName) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    selectedTeam = teamName;
                    close(OK_EXIT_CODE);
                }
            });
        }

        // Add a cancel button
        actions.add(new AbstractAction(SKIP_THIS_STEP) {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectedTeam = SKIP_THIS_STEP;
                close(CANCEL_EXIT_CODE);
            }
        });
        return actions.toArray(new Action[0]);
    };

    public String getSelectedTeam() {
        return selectedTeam;
    }
}
