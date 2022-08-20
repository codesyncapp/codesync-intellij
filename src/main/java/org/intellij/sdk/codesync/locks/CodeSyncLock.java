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

    public CodeSyncLock () {
        this(Constants.GLOBAL_LOCK_KEY);
    }

    public CodeSyncLock (String category) {
        LockFile lockFile = this.loadLockFile();
        if (lockFile != null) {
            this.lock = lockFile.getLock(category);
        }
    }

    private LockFile loadLockFile() {

        try {
            lockFile = new LockFile(LOCK_FILE, true);
        } catch (InvalidYmlFileError | FileNotFoundException | FileNotCreatedError error) {
            error.printStackTrace();
            CodeSyncLogger.logEvent(String.format(
                    "[LOCK_FILE]] Lock file error, %s.\n", error.getMessage()
            ));
            return null;
        }

        return lockFile;
    }

    public boolean acquireLock(String projectName) {
        if (this.lock == null || this.lock.isActive()) {
            return false;
        } else {
            Instant expiry = Instant.now().plus(5, ChronoUnit.MINUTES);
            return this.lockFile.publishNewLock(this.lock.getCategory(), Date.from(expiry), projectName);
        }
    }

    //  TODO: Add releaseLocks instead here, that will release all the locks with matching identifiers.
    public void releaseLock(String projectName) {
        if (this.lock.compareIdentifier(projectName)) {
            Instant past = Instant.now().minus(1, ChronoUnit.MINUTES);

            this.lockFile.publishNewLock(this.lock.getCategory(), Date.from(past), projectName);
        }
    }
}
