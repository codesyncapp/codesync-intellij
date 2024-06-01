package org.intellij.sdk.codesync.ui.dialogs;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.intellij.sdk.codesync.Constants.Notification;
import org.intellij.sdk.codesync.Constants.NotificationButton;
import org.intellij.sdk.codesync.utils.CommonUtils;
import org.jdesktop.swingx.JXLabel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

import static org.intellij.sdk.codesync.Constants.CODESYNC_PRICING_URL;


public class PricingAlertDialog extends DialogWrapper {
    String primaryMessage = Notification.PRICING_LIMIT_REACHED_MESSAGE,
            title = Notification.UPGRADE,
            cancelButtonText = "Maybe later";
    String upgradeButtonText, secondaryMessage, pricingURL;

    public PricingAlertDialog(Boolean isOrgRepo, Boolean canAvailTrial, String pricingURL, Project project) {
        super(project, true);
        this.pricingURL = pricingURL;
        if (canAvailTrial) {
            upgradeButtonText = isOrgRepo ? NotificationButton.TRY_TEAM_FOR_FREE : NotificationButton.TRY_PRO_FOR_FREE;
            secondaryMessage = isOrgRepo ? Notification.TRY_TEAM_PLAN_FOR_FREE : Notification.TRY_PRO_FOR_FREE;
        } else {
            upgradeButtonText = isOrgRepo ? NotificationButton.UPGRADE_TO_TEAM : NotificationButton.UPGRADE_TO_PRO;
            secondaryMessage = isOrgRepo ? Notification.UPGRADE_ORG_PRICING_PLAN : Notification.UPGRADE_PRICING_PLAN;
        }

        setTitle(this.title);
    }

    public PricingAlertDialog(Boolean canAvailTrial) {
        this(false, canAvailTrial, CODESYNC_PRICING_URL, null);
        setTitle(Notification.PRIVATE_REPO_COUNT_REACHED);
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

    public PricingAlertDialog(Boolean isOrgRepo, Boolean canAvailTrial, String pricingURL) {
        this(isOrgRepo, canAvailTrial, pricingURL, null);
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel dialogPanel = new JPanel(new BorderLayout());

        String htmlMessage = String.format(
                "<html><p>%s</p><br/><p>%s</p><br/></html>", this.primaryMessage, this.secondaryMessage
        );
        JXLabel label = new JXLabel(htmlMessage);
        label.setLineWrap(true);
        dialogPanel.add(label, BorderLayout.LINE_START);

        return dialogPanel;
    }

    /*
    Redirect the user to upgrade page and clear the locks.
    User may upgrade his plan and come back to try again. in such case, we do not want to show the same dialog again.
     */
    protected void redirectToUpgrade() {
        BrowserUtil.browse(this.pricingURL);
    }

    @Override
    protected Action @NotNull [] createActions() {
        return new Action[]{
                new AbstractAction(upgradeButtonText) {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        redirectToUpgrade();
                        if (isEnabled()) {
                            close(OK_EXIT_CODE);
                        }
                    }
                },
                new DialogWrapperExitAction(cancelButtonText, CANCEL_EXIT_CODE)
        };
    }

}
