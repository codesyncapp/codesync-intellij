package org.intellij.sdk.codesync.ui.dialogs;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.intellij.sdk.codesync.Constants.Notification;
import org.intellij.sdk.codesync.alerts.TeamActivityAlerts;
import org.intellij.sdk.codesync.utils.CommonUtils;
import org.jdesktop.swingx.JXLabel;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;


public class TeamActivityAlertDialog extends DialogWrapper {
    String primaryMessage = Notification.TEAM_ACTIVITY_ALERT_MESSAGE,
        secondaryMessage = Notification.TEAM_ACTIVITY_ALERT_SECONDARY_MESSAGE,
        title = Notification.TEAM_ACTIVITY_ALERT_HEADER_MESSAGE,
        reviewButtonText = "View team activity",
        reviewLaterButtonText = "Remind me later",
        cancelButtonText = "Skip for today"
    ;
    String teamActivityURL;

    public TeamActivityAlertDialog(String teamActivityURL, Project project){
        super(project, true);
        this.teamActivityURL = teamActivityURL;
        setTitle(this.title);
    }

    public TeamActivityAlertDialog(String teamActivityURL){
        this(teamActivityURL, null);
    }

    public void show() {
        CommonUtils.invokeAndWait(
            () -> {
                init();
                super.show();
                return OK_EXIT_CODE;
            },
            ModalityState.defaultModalityState()
        );
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel dialogPanel = new JPanel(new BorderLayout());

        String htmlMessage = String.format(
            "<html><p>%s</p><br/><p>%s</p><br/></html>", this.primaryMessage, this.secondaryMessage
        );
        JXLabel label = new JXLabel(htmlMessage);
        label.setMaxLineSpan(200);
        label.setLineWrap(true);
        label.setPreferredSize(new Dimension(500, Integer.MAX_VALUE));
        dialogPanel.add(label, BorderLayout.LINE_START);

        return dialogPanel;
    }

    protected void redirectToTeamActivity() {
        BrowserUtil.browse(this.teamActivityURL);
        // User has already been redirected to the activity page, we can now skip subsequent alerts.
        TeamActivityAlerts.skipToday();
    }
    protected void remindLaterAction() {
        TeamActivityAlerts.remindLater();
    }
    protected void skipForTodayAction() {
        TeamActivityAlerts.skipToday();
    }

    @Override
    protected Action @NotNull [] createActions() {
        return new Action[]{
            new AbstractAction(reviewButtonText) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (isEnabled()) {
                        redirectToTeamActivity();
                        close(OK_EXIT_CODE);
                    }
                }
            },
            new AbstractAction(reviewLaterButtonText) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (isEnabled()) {
                        remindLaterAction();
                        close(OK_EXIT_CODE);
                    }
                }
            },
            new AbstractAction(cancelButtonText) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (isEnabled()) {
                        skipForTodayAction();
                        close(CANCEL_EXIT_CODE);
                    }
                }
            },
        };
    }

}
