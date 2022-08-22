package org.intellij.sdk.codesync.locks;

import org.intellij.sdk.codesync.CodeSyncLogger;
import org.intellij.sdk.codesync.Constants;
import org.intellij.sdk.codesync.exceptions.FileNotCreatedError;
import org.intellij.sdk.codesync.exceptions.InvalidYmlFileError;
import org.intellij.sdk.codesync.files.LockFile;

import java.io.FileNotFoundException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.intellij.sdk.codesync.Constants.LOCK_FILE;

public class CodeSyncLock {
    LockFile.Lock lock = null;
    LockFile lockFile;

    public CodeSyncLock (String category) {
        lockFile = loadLockFile();
        if (lockFile != null) {
            this.lock = lockFile.getLock(category);
        }
    }

    private static LockFile loadLockFile() {
        try {
            return new LockFile(LOCK_FILE, true);
        } catch (InvalidYmlFileError | FileNotFoundException | FileNotCreatedError error) {
            error.printStackTrace();
            CodeSyncLogger.logEvent(String.format(
                    "[LOCK_FILE]] Lock file error, %s.\n", error.getMessage()
            ));
            return null;
        }
    }

    public boolean acquireLock(String identifier) {
        if (this.lock == null || this.lock.isActive()) {
            return false;
        } else {
            Instant expiry = Instant.now().plus(5, ChronoUnit.MINUTES);
            return this.lockFile.publishNewLock(this.lock.getCategory(), Date.from(expiry), identifier);
        }
    }

    /*
    Release all the locks having the given identifier.
    */
    public static void releaseAllLocks(String identifier) {
       LockFile lockFile = loadLockFile();
       if (lockFile != null) {
           for (LockFile.Lock lock: lockFile.getLocks()) {
               if (lock.compareIdentifier(identifier)) {
                   Instant past = Instant.now().minus(1, ChronoUnit.MINUTES);

                   lockFile.publishNewLock(lock.getCategory(), Date.from(past), identifier);
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

            this.lockFile.publishNewLock(this.lock.getCategory(), Date.from(past), identifier);
        }
    }
}
