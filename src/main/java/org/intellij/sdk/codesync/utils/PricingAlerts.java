package org.intellij.sdk.codesync.utils;

import com.intellij.ide.BrowserUtil;
import org.intellij.sdk.codesync.clients.CodeSyncClient;
import org.intellij.sdk.codesync.locks.CodeSyncLock;
import org.intellij.sdk.codesync.ui.messages.CodeSyncMessages;
import org.json.simple.JSONObject;

import static org.intellij.sdk.codesync.Constants.LockFileType;
import static org.intellij.sdk.codesync.Constants.*;

public class PricingAlerts {
    private static void acquirePricingLock() {
        CodeSyncLock pricingAlertLock = new CodeSyncLock(LockFileType.PROJECT_LOCK, PRICING_ALERT_LOCK_KEY);
        pricingAlertLock.acquireLock(PRICING_ALERT_LOCK_KEY);
    }

    public static void setPlanLimitReached() {
        // We only want to show notification once every 5 minutes, I have implemented that using locks with an expiry of
        // 5 minutes. So, skip the notification if lock is not acquired.
        acquirePricingLock();
        boolean shouldUpgrade = CodeSyncMessages.showYesNoMessage(
                Notification.UPGRADE, Notification.UPGRADE_PRICING_PLAN, CommonUtils.getCurrentProject()
        );
        if (shouldUpgrade) {
            BrowserUtil.browse(CODESYNC_PRICING_URL);
        }
    }

    public static void setPlanLimitReached(String accessToken, int repoId) {
        CodeSyncClient codeSyncClient = new CodeSyncClient();
        JSONObject response = codeSyncClient.getRepoPlanInfo(accessToken, repoId);
        if (response != null) {
            acquirePricingLock();
            boolean isOrgRepo = (boolean) response.get("is_org_repo");
            String pricingUrl = (String) response.get("url");
            String message = isOrgRepo ? Notification.UPGRADE_ORG_PLAN : Notification.UPGRADE_PRICING_PLAN;
            boolean shouldUpgrade = CodeSyncMessages.showYesNoMessage(
                    Notification.UPGRADE, message, CommonUtils.getCurrentProject()
            );
            if (shouldUpgrade) {
                BrowserUtil.browse(pricingUrl);
            }
        } else {
            setPlanLimitReached();
        }
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
