package org.intellij.sdk.codesync.locks;

import org.intellij.sdk.codesync.CodeSyncLogger;
import org.intellij.sdk.codesync.exceptions.FileNotCreatedError;
import org.intellij.sdk.codesync.exceptions.InvalidYmlFileError;
import org.intellij.sdk.codesync.files.LockFile;
import org.intellij.sdk.codesync.utils.CommonUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.intellij.sdk.codesync.Constants.*;

/*
Note: This locking mechanism is very different from traditional locks as we are
ignoring the cases if lock was unable to be acquired. This is intentional as in our use case if lock is unable to
be acquired we would simply have duplicate processes running. If we implement the traditional locks that processes will
not start if lock is not acquired and user's code will not be synced.
 */
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
        } catch (FileNotFoundException | FileNotCreatedError error) {
            CodeSyncLogger.error(String.format(
                "[LOCK_FILE] Lock file error, %s.\n", CommonUtils.getStackTrace(error)
            ));
            return null;
        } catch (InvalidYmlFileError error) {
            CodeSyncLogger.error(String.format(
                "[LOCK_FILE] Lock file error Invalid Yaml error, removing file contents. Error: %s.\n",
                CommonUtils.getStackTrace(error)
            ));
            LockFile.removeFileContents(new File(this.getLockFilePath()));
            return null;
        }
    }

    /*
    Acquire lock and return `true` if lock was acquired with success or `false` of lock could not be acquired.
    */
    public boolean acquireLock() {
        // Default expiry is 5 minutes.
        Instant defaultExpiry = Instant.now().plus(5, ChronoUnit.MINUTES);
        return acquireLock(defaultExpiry);
    }

    /*
    Acquire lock for the given owner and return `true` if lock was acquired with success
    or `false` of lock could not be acquired.
    */
    public boolean acquireLock(String owner) {
        // Default expiry is 5 minutes.
        Instant defaultExpiry = Instant.now().plus(5, ChronoUnit.MINUTES);
        return acquireLock(owner, defaultExpiry);
    }

    /*
    Acquire lock with custom expiry.
     */
    public boolean acquireLock(Instant expiry) {
        return acquireLock(null, expiry);
    }

    /*
    Acquire lock with custom expiry.

    Return true if lock is acquired with success and false otherwise.
     */
    public boolean acquireLock(String owner, Instant expiry) {
        if (isLockAcquired() && !isLockOwner(owner)) {
            // Lock is acquired by some other process, it can not be acquired.
            return false;
        }
        // If lock is not acquired or if the lock belongs to the same owner then publish the new lock.
        return this.lockFile.publishNewLock(this.lock.getCategory(), expiry, owner);
    }

    /*
    Return `true` if lock is acquired for the given owner, `false` otherwise.

    Owner is useful when same is locked is shared by different processes with the same owner.
    e.g.
    */
    public boolean isLockAcquired(String owner) {
        // If lock is active and has the same owner as the argument then lock is acquired,
        // and we should return true.
        return this.lock != null && this.lock.isActive() && this.isLockOwner(owner);
    }

    /*
    Return `true` if lock is acquired, `false` otherwise.
    */
    public boolean isLockAcquired() {
        return this.lock != null && this.lock.isActive();
    }

    /*
    Check if given string is the owner of the lock, locker owner is the same as lock owner.
     */
    public boolean isLockOwner(String owner) {
        return this.lock.hasOwner(owner);
    }

    /*
    Release all the locks having the given owner.
    */
    public static void releaseAllLocks(String lockType, String owner) {
        CodeSyncLock codeSyncLock = new CodeSyncLock(lockType, owner);
        LockFile lockFile = codeSyncLock.loadLockFile();
        if (lockFile != null) {
           for (LockFile.Lock lock: lockFile.getLocks()) {
               if (lock.hasOwner(owner)) {
                   Instant past = Instant.now().minus(1, ChronoUnit.MINUTES);
                   lockFile.publishNewLock(lock.getCategory(), past, owner);
               }
           }
        }
    }

    /*
    Release a single lock, lock will only be released if it has the same owner as the one in the argument.
    */
    public void releaseLock(String owner) {
        if (this.lock.hasOwner(owner)) {
            Instant past = Instant.now().minus(1, ChronoUnit.MINUTES);
            this.lockFile.publishNewLock(this.lock.getCategory(), past, owner);
        }
    }

    /*
    Release a single lock.
    */
    public void releaseLock() {
        Instant past = Instant.now().minus(1, ChronoUnit.MINUTES);
        this.lockFile.publishNewLock(this.lock.getCategory(), past, null);
    }
}
