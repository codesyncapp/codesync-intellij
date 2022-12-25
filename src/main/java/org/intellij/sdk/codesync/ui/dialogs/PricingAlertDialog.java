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
    String primaryMessage = Notification.PRICING_LIMIT_REACHED_MESSAGE,
        title = Notification.UPGRADE,
        cancelButtonText = "Maybe later"
    ;
    String upgradeButtonText, secondaryMessage, pricingURL;

    public PricingAlertDialog(Boolean isOrgRepo, Boolean canAvailTrial, String pricingURL, Project project){
        super(project, true);
        this.pricingURL = pricingURL;

        if (canAvailTrial) {
            upgradeButtonText = "Try Pro for free";
            secondaryMessage = isOrgRepo ? Notification.TRY_ORG_PRO_FOR_FREE: Notification.TRY_PRO_FOR_FREE;
        } else {
            upgradeButtonText = "Upgrade to Pro";
            secondaryMessage = isOrgRepo ? Notification.UPGRADE_ORG_PRICING_PLAN: Notification.UPGRADE_PRICING_PLAN;
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
            "<html><p>%s<p><br/><p>%s</p><br/></html>", this.primaryMessage, this.secondaryMessage
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