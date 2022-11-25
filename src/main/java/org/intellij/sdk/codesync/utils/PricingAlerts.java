package org.intellij.sdk.codesync.utils;

import com.intellij.ide.BrowserUtil;
import org.intellij.sdk.codesync.locks.CodeSyncLock;
import org.intellij.sdk.codesync.ui.messages.CodeSyncMessages;

import static org.intellij.sdk.codesync.Constants.LockFileType;
import static org.intellij.sdk.codesync.Constants.*;

public class PricingAlerts {
    private static void acquirePricingLock() {
        CodeSyncLock pricingAlertLock = new CodeSyncLock(LockFileType.PROJECT_LOCK, PRICING_ALERT_LOCK_KEY);
        pricingAlertLock.acquireLock(PRICING_ALERT_LOCK_KEY);
    }

    public static void setPlanLimitReached() {
        acquirePricingLock();
        boolean shouldUpgrade = CodeSyncMessages.showYesNoMessage(
                Notification.UPGRADE, Notification.UPGRADE_PRICING_PLAN, CommonUtils.getCurrentProject()
        );
        if (shouldUpgrade) {
            BrowserUtil.browse(CODESYNC_PRICING_URL);
        }
    }

    public static void setPlanLimitReached(int repoId) {
        acquirePricingLock();
    }

    public static void resetPlanLimitReached() {
        CodeSyncLock pricingAlertLock = new CodeSyncLock(LockFileType.PROJECT_LOCK, PRICING_ALERT_LOCK_KEY);
        pricingAlertLock.releaseLock(PRICING_ALERT_LOCK_KEY);
    }

    public static boolean getPlanLimitReached() {
        CodeSyncLock pricingAlertLock = new CodeSyncLock(LockFileType.PROJECT_LOCK, PRICING_ALERT_LOCK_KEY);

        // If we are able to acquire the lock then it means price limit is not reached.
        return !pricingAlertLock.acquireLock(PRICING_ALERT_LOCK_KEY);
    }
}
