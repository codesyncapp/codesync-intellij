package org.intellij.sdk.codesync.actions;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

import static org.intellij.sdk.codesync.Constants.WEBAPP_DASHBOARD_URL;

public class ViewDashboardAction extends BaseModuleAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
        if (this.isAccountDeactivated() || !this.isAuthenticated()) {
            e.getPresentation().setEnabled(false);
        }
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        BrowserUtil.browse(WEBAPP_DASHBOARD_URL);
    }
}
