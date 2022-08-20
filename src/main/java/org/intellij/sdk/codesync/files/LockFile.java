package org.intellij.sdk.codesync.files;

import org.intellij.sdk.codesync.Constants;
import org.intellij.sdk.codesync.exceptions.FileNotCreatedError;
import org.intellij.sdk.codesync.exceptions.InvalidYmlFileError;
import org.intellij.sdk.codesync.utils.CommonUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/*
A file that will contain information about different kinds of inter-process and inter-thread synchronization.

The file will contain a dictionary at the root and can have the following structure. The key of the root map
will be the category of the lock, virtually any kind of lock is possible e.g. project specific (project name as key),
auth (for authentication related locks), daemon (for daemon related locks). Any lock that does not have a category
will have the key global.

Each lock entry will be required to have an expiry (timestamp), this expiry should not be very large and
ideally should not be any more than 5 minutes in the future.

Presence of a key with some expiry in the future will indicate that the lock is active for that category. A missing
entry or entry with expiry in the past would mean lock is not active.

```
    global: # global lock does not have any category.
        expiry: <time-stamp-indicating-lock-expiry>
        identifier: <name-of-the-project-holding-the-lock>
    auth:
        expiry: <time-stamp-indicating-lock-expiry>

```
 */
public class LockFile extends CodeSyncYmlFile {

    File lockFile;
    Map<String, Object> contentsMap;
    Map<String, Lock> locks = new HashMap<>();

    public static class Lock {
        Date expiry;
        String category, identifier;

        public Lock(String category, Map<String, Object> lockContents) {
            this.category = category;
            this.expiry = CommonUtils.parseDate((String) lockContents.get("expiry"));
            this.identifier = (String) lockContents.get("identifier");
        }

        public Lock(String category, Date expiry, String identifier) {
            this.category = category;
            this.expiry = expiry;
            this.identifier = identifier;
        }

        public Date getExpiry () {
            return this.expiry;
        }

        public String getCategory () {
            return this.category;
        }

        public boolean isActive () {
            return this.expiry.getTime()/1000 >  Instant.now().getEpochSecond();
        }

        /*
        Compares the lock identifier with the one in the arguments, returns true if both are same and false otherwise.
         */
        public boolean compareIdentifier(String identifier) {
            return identifier.compareTo(this.identifier) == 0;
        }

        public Map<String, Object> getYMLAsHashMap() {
            Map<String, Object> lock = new HashMap<>();
            lock.put("expiry", CommonUtils.formatDate(this.expiry));
            lock.put("identifier", this.identifier);

            return lock;
        }
    }

    public LockFile(String filePath) throws FileNotFoundException, InvalidYmlFileError {
        File lockFile = new File(filePath);

        if (!lockFile.isFile()) {
            throw new FileNotFoundException(String.format("Lock file \"%s\" does not exist.", filePath));
        }
        this.lockFile = lockFile;
        this.contentsMap = this.readYml();
        this.loadYmlContent();
    }

    public LockFile(String filePath, boolean shouldCreateIfAbsent) throws FileNotFoundException, InvalidYmlFileError, FileNotCreatedError {
        File lockFile = new File(filePath);

        if (!lockFile.isFile() && shouldCreateIfAbsent) {
            boolean isFileReady = createFile(filePath);
            if (!isFileReady) {
                throw new FileNotCreatedError(String.format("Lock file \"%s\" could not be created.", filePath));
            }
        } else if (!lockFile.isFile()) {
            throw new FileNotFoundException(String.format("Lock file \"%s\" does not exist.", filePath));
        }
        this.lockFile = lockFile;
        this.contentsMap = this.readYml();
        this.loadYmlContent();
    }

    public File getYmlFile() {
        return this.lockFile;
    }

    public Map<String, Object> getYMLAsHashMap() {
        Map<String, Object> contents = new HashMap<>();
        for (Lock lock: this.locks.values()) {
            contents.put(lock.getCategory(), lock.getYMLAsHashMap());
        }
        return contents;
    }

    private void loadYmlContent () throws InvalidYmlFileError {
        try {
            for (Map.Entry<String, Object> lockEntry : this.contentsMap.entrySet()) {
                if (lockEntry.getValue() != null) {
                    Map<String, Object> lockContents = (Map<String, Object>) lockEntry.getValue();
                    this.locks.put(lockEntry.getKey(), new Lock(lockEntry.getKey(), lockContents));
                }
            }
        } catch (ClassCastException e){
            throw new InvalidYmlFileError(String.format("Lock yml file \"%s\" is not valid.", this.getYmlFile().getPath()));
        }
    }

    /*
    Return the lock with category if present, otherwise return a lock from the past.
    */
    public Lock getLock (String category) {
        Instant past = Instant.now().minus(1, ChronoUnit.MINUTES);
        return this.locks.getOrDefault(category, new Lock(Constants.GLOBAL_LOCK_KEY, Date.from(past), null));
    }

    public Lock getLock() {
        return getLock(Constants.GLOBAL_LOCK_KEY);
    }

    public void updateLock (String category, Date expiry, String identifier) {
        this.locks.put(category, new Lock(category, expiry, identifier));
    }

    public boolean publishNewLock (String category, Date expiry, String identifier) {
        this.updateLock(category, expiry, identifier);
        try {
            this.writeYml();
            return true;
        } catch (FileNotFoundException | InvalidYmlFileError e) {
            return false;
        }
    }
}
