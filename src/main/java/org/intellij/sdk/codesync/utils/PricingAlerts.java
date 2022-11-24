package org.intellij.sdk.codesync.utils;

import org.intellij.sdk.codesync.locks.CodeSyncLock;

import static org.intellij.sdk.codesync.Constants.LockFileType;
import static org.intellij.sdk.codesync.Constants.PRICING_ALERT_LOCK_KEY;

public class PricingAlerts {
    public static void setPlanLimitReached() {
        CodeSyncLock pricingAlertLock = new CodeSyncLock(LockFileType.PROJECT_LOCK, PRICING_ALERT_LOCK_KEY);
        pricingAlertLock.acquireLock(PRICING_ALERT_LOCK_KEY);
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
