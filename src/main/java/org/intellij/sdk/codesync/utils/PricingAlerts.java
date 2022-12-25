package org.intellij.sdk.codesync.utils;

import com.intellij.openapi.project.Project;
import org.intellij.sdk.codesync.clients.CodeSyncClient;
import org.intellij.sdk.codesync.locks.CodeSyncLock;
import org.intellij.sdk.codesync.ui.dialogs.PricingAlertDialog;
import org.json.simple.JSONObject;

import static org.intellij.sdk.codesync.Constants.LockFileType;
import static org.intellij.sdk.codesync.Constants.*;

public class PricingAlerts {
    private static void acquirePricingLock() {
        CodeSyncLock pricingAlertLock = new CodeSyncLock(LockFileType.PROJECT_LOCK, PRICING_ALERT_LOCK_KEY);
        pricingAlertLock.acquireLock(PRICING_ALERT_LOCK_KEY);
    }
    public static void setPlanLimitReached() {
        setPlanLimitReached((Project) null);
    }
    public static void setPlanLimitReached(Project project) {
        // We only want to show notification once every 5 minutes, I have implemented that using locks with an expiry of
        // 5 minutes. So, skip the notification if lock is not acquired.
        acquirePricingLock();

        PricingAlertDialog pricingAlertDialog = new PricingAlertDialog(false, false, CODESYNC_PRICING_URL, project);
        pricingAlertDialog.show();
    }

    public static void setPlanLimitReached(String accessToken, int repoId) {
        setPlanLimitReached(accessToken, repoId, (Project) null);
    }
    public static void setPlanLimitReached(String accessToken, int repoId, Project project) {
        CodeSyncClient codeSyncClient = new CodeSyncClient();
        JSONObject response = codeSyncClient.getRepoPlanInfo(accessToken, repoId);
        if (response != null) {
            acquirePricingLock();
            boolean isOrgRepo = (boolean) response.get("is_org_repo");
            boolean canAvailTrial = (boolean) response.get("can_avail_trial");
            String pricingUrl = (String) response.get("url");

            PricingAlertDialog pricingAlertDialog = new PricingAlertDialog(isOrgRepo, canAvailTrial, pricingUrl, project);
            pricingAlertDialog.show();
        } else {
            setPlanLimitReached(project);
        }
    }

    public static void resetPlanLimitReached() {
        CodeSyncLock pricingAlertLock = new CodeSyncLock(LockFileType.PROJECT_LOCK, PRICING_ALERT_LOCK_KEY);
        pricingAlertLock.releaseLock(PRICING_ALERT_LOCK_KEY);
    }

    /*
    Return `true` if plan limit is reached for the current user, `false` otherwise.
    */
    public static boolean getPlanLimitReached() {
        CodeSyncLock pricingAlertLock = new CodeSyncLock(LockFileType.PROJECT_LOCK, PRICING_ALERT_LOCK_KEY);

        // If we are able to acquire the lock then it means price limit is not reached.
        return !pricingAlertLock.acquireLock(PRICING_ALERT_LOCK_KEY);
    }
}
