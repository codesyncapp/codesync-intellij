package org.intellij.sdk.codesync.ui.dialogs;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.intellij.sdk.codesync.Constants.Notification;
import org.intellij.sdk.codesync.utils.CommonUtils;
import org.jdesktop.swingx.JXLabel;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;


public class PricingAlertDialog extends DialogWrapper {
    String pricingURL, primaryMessage, secondaryMessage = "";
    Boolean isOrgRepo, canAvailTrial;
    String title = Notification.UPGRADE;

    public PricingAlertDialog(Boolean isOrgRepo, Boolean canAvailTrial, String pricingURL, Project project){
        super(project, true); // use current window as parent
        this.isOrgRepo = isOrgRepo;
        this.canAvailTrial = canAvailTrial;
        this.pricingURL = pricingURL;

        this.primaryMessage = isOrgRepo ? Notification.UPGRADE_ORG_PLAN : Notification.UPGRADE_PRICING_PLAN;
        if (canAvailTrial) {
            this.secondaryMessage = Notification.TRIAL_PROMPT_MESSAGE;
        }

        setTitle(this.title);
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

    public PricingAlertDialog(Boolean isOrgRepo, Boolean canAvailTrial, String pricingURL){
        this(isOrgRepo, canAvailTrial, pricingURL, null);
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel dialogPanel = new JPanel(new BorderLayout());

        String htmlMessage = String.format(
            "<html><p>%s<p><br/><p>%s</p></html>", this.primaryMessage, this.secondaryMessage
        );
        JXLabel label = new JXLabel(htmlMessage);
        label.setLineWrap(true);
        dialogPanel.add(label, BorderLayout.LINE_START);

        return dialogPanel;
    }

    protected void redirectToUpgrade() {
        BrowserUtil.browse(this.pricingURL);
    }

    @Override
    protected Action @NotNull [] createActions() {
        Action tryForFreeAction = new AbstractAction(
            isOrgRepo ? Notification.TRY_TEAM_FOR_FREE : Notification.TRY_PRO_FOR_FREE
        ) {
            @Override
            public void actionPerformed(ActionEvent e) {
                redirectToUpgrade();
                if (isEnabled()) {
                    close(OK_EXIT_CODE);
                }
            }
        };

        // We should disable this button if user is not eligible for a free trial.
        if (!this.canAvailTrial) {
            tryForFreeAction.setEnabled(false);
        }

        return new Action[]{
            new DialogWrapperAction(Notification.UPGRADE) {
                @Override
                protected void doAction(ActionEvent e) {
                    // handle button 1 click here
                    if (isEnabled()) {
                        redirectToUpgrade();
                        close(OK_EXIT_CODE);
                    }
                }
            },
            tryForFreeAction,
            new DialogWrapperExitAction("Cancel", CANCEL_EXIT_CODE)
        };
    }

}
