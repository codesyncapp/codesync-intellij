package org.intellij.sdk.codesync.locks;

import org.intellij.sdk.codesync.CodeSyncLogger;
import org.intellij.sdk.codesync.exceptions.FileNotCreatedError;
import org.intellij.sdk.codesync.exceptions.InvalidYmlFileError;
import org.intellij.sdk.codesync.files.LockFile;

import java.io.FileNotFoundException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.intellij.sdk.codesync.Constants.*;

public class CodeSyncLock {
    LockFile.Lock lock = null;
    LockFile lockFile;
    String lockType;

    public CodeSyncLock (String lockType, String category) {
        this.lockType = lockType;
        lockFile = loadLockFile();
        if (lockFile != null) {
            this.lock = lockFile.getLock(category);
        }
    }

    private String getLockFilePath() {
        if (this.lockType.toLowerCase().contains(LockFileType.HANDLE_BUFFER_LOCK)) {
            return HANDLE_BUFFER_LOCK_FILE;
        }
        if (this.lockType.toLowerCase().contains(LockFileType.POPULATE_BUFFER_LOCK)) {
            return POPULATE_BUFFER_LOCK_FILE;
        }
        return PROJECT_LOCK_FILE;
    }

    private LockFile loadLockFile() {
        try {
            return new LockFile(this.getLockFilePath(), true);
        } catch (InvalidYmlFileError | FileNotFoundException | FileNotCreatedError error) {
            error.printStackTrace();
            CodeSyncLogger.error(String.format(
                    "[LOCK_FILE]] Lock file error, %s.\n", error.getMessage()
            ));
            return null;
        }
    }

    /*
    Acquire lock and return `true` if lock was acquired with success or `false` of lock could not be acquired.
    */
    public boolean acquireLock(String identifier) {
        // Default expiry is 5 minutes.
        Instant defaultExpiry = Instant.now().plus(5, ChronoUnit.MINUTES);
        return acquireLock(identifier, defaultExpiry);
    }

    /*
    Acquire lock with custom expiry.
     */
    public boolean acquireLock(String identifier, Instant expiry) {
        if (this.lock == null) {
            return false;
        } else if (this.lock.isActive() && this.lock.compareIdentifier(identifier)) {
            return false;
        } else {
            return this.lockFile.publishNewLock(this.lock.getCategory(), expiry, identifier);
        }
    }

    /*
    Return `true` if lock was acquired with success or `false` of lock could not be acquired.
    */
    public boolean isLockAcquired(String identifier) {
        if (this.lock == null) {
            return false;
        } else {
            // If lock is active and has the same identifier as the argument then lock is acquired,
            // and we should return true.
            return this.lock.isActive() && this.lock.compareIdentifier(identifier);
        }
    }

    /*
    Release all the locks having the given identifier.
    */
    public static void releaseAllLocks(String lockType, String identifier) {
        CodeSyncLock codeSyncLock = new CodeSyncLock(lockType, identifier);
        LockFile lockFile = codeSyncLock.loadLockFile();
        if (lockFile != null) {
           for (LockFile.Lock lock: lockFile.getLocks()) {
               if (lock.compareIdentifier(identifier)) {
                   Instant past = Instant.now().minus(1, ChronoUnit.MINUTES);

                   lockFile.publishNewLock(lock.getCategory(), past, identifier);
               }
           }
        }
    }

    /*
    Release a single lock, lock will only be released if it has the same identifier as the one in the argument.
    */
    public void releaseLock(String identifier) {
        if (this.lock.compareIdentifier(identifier)) {
            Instant past = Instant.now().minus(1, ChronoUnit.MINUTES);

            this.lockFile.publishNewLock(this.lock.getCategory(), past, identifier);
        }
    }
}
