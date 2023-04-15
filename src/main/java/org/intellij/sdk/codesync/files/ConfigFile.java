package org.intellij.sdk.codesync.files;

import java.io.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.HashMap;

import org.intellij.sdk.codesync.CodeSyncLogger;
import org.intellij.sdk.codesync.exceptions.FileLockedError;
import org.intellij.sdk.codesync.exceptions.InvalidConfigFileError;
import org.intellij.sdk.codesync.exceptions.InvalidYmlFileError;
import org.intellij.sdk.codesync.state.StateUtils;


public class ConfigFile extends CodeSyncYmlFile {
    File configFile;
    Map<String, Object> contentsMap;
    public Map<String, ConfigRepo> repos = new HashMap<>();

    private static Instant fileWriteLockExpiry = null;

    public ConfigFile(String filePath) throws InvalidConfigFileError {
        File configFile = new File(filePath);

        if (!configFile.isFile()) {
            throw new InvalidConfigFileError("Config file path must be absolute path pointing to a file.");
        }

        this.configFile = configFile;

        try {
            this.contentsMap = this.readYml();
        } catch (InvalidYmlFileError e) {
            CodeSyncYmlFile.removeFileContents(this.getYmlFile());
            StateUtils.reloadState(StateUtils.getGlobalState().project);
            CodeSyncLogger.error("Removed contents of the config file after facing invalid yaml error.");
        } catch (FileNotFoundException e) {
            throw new InvalidConfigFileError(e.getMessage());
        }

        try {
            this.loadYmlContent();
        } catch (InvalidConfigFileError e){
            throw new InvalidConfigFileError(String.format("Config file is not valid. Error: %s", e.getMessage()));
        }
    }

    public File getYmlFile() {
        return this.configFile;
    }

    public Map<String, Object> getYMLAsHashMap() {
        Map<String, Object> repos = new HashMap<>();
        for (Map.Entry<String, ConfigRepo> repo : this.repos.entrySet()) {
            repos.put(repo.getKey(), repo.getValue().getYMLAsHashMap());
        }
        Map<String, Object> configFile = new HashMap<>();
        configFile.put("repos", repos);

        return configFile;
    }

    private static void acquireLock() {
        // This is a temporary fix to wait till the file contents are flushed before writing content of the next call.
        if (fileWriteLockExpiry != null) {
            try {
                long waitInMillis = Instant.now().until(fileWriteLockExpiry, ChronoUnit.MILLIS);
                if (waitInMillis > 0) {
                    Thread.sleep(waitInMillis);
                }
            } catch (InterruptedException e) {
                // Ignore the exception.
                CodeSyncLogger.error("Error calling Thread.sleep to handle config file locking.");
                Thread.currentThread().interrupt();
            }
        }

        // Set the expiry to 5 seconds into the future.
        fileWriteLockExpiry = Instant.now().plus(5, ChronoUnit.SECONDS);
    }

    private static void releaseLock() {
        // Clear the lock
        fileWriteLockExpiry = null;
    }

    private void reloadFromFile() throws InvalidConfigFileError {
        try {
            this.contentsMap = this.readYml();
        } catch (FileNotFoundException | InvalidYmlFileError e) {
            throw new InvalidConfigFileError(e.getMessage());
        }
        this.loadYmlContent();
    }

    private void loadYmlContent () throws InvalidConfigFileError {
        if (this.contentsMap == null || !this.contentsMap.containsKey("repos")) {
            // Empty config file.
            return;
        }

        try {
            Map<String, Map<String, Object>> repos = (Map<String, Map<String, Object>>) this.contentsMap.get("repos");

            for (Map.Entry<String, Map<String, Object>> repo : repos.entrySet()) {
                if (repo.getValue() != null){
                    this.repos.put(repo.getKey(), new ConfigRepo(repo.getKey(), repo.getValue()));
                }
            }
        } catch (ClassCastException e){
            throw new InvalidConfigFileError(String.format("Config file is not valid. Error: %s", e.getMessage()));
        }
    }

    public ConfigRepo getRepo(String repoPath) {
        return this.repos.get(repoPath);
    }

    public boolean hasRepo(String repoPath) {
        return this.repos.containsKey(repoPath);
    }

    public Map<String, ConfigRepo> getRepos() {
        return this.repos;
    }

    public void updateRepo(String repoPath, ConfigRepo newRepo) {
        this.repos.put(repoPath, newRepo);
    }
    public void deleteRepo(String repoPath) {
        this.repos.remove(repoPath);
    }

    public void publishRepoUpdate (ConfigRepo updatedRepo) throws InvalidConfigFileError {
        acquireLock();
        this.reloadFromFile();
        this.updateRepo(updatedRepo.repoPath, updatedRepo);
        try {
            this.writeYml();
            releaseLock();
        } catch (FileNotFoundException | InvalidYmlFileError | FileLockedError e) {
            releaseLock();
            throw new InvalidConfigFileError(e.getMessage());
        }
    }

    public void publishRepoRemoval (String repoPath) throws InvalidConfigFileError {
        acquireLock();
        this.reloadFromFile();
        this.deleteRepo(repoPath);
        try {
            this.writeYml();
            releaseLock();
        } catch (FileNotFoundException | InvalidYmlFileError | FileLockedError e) {
            releaseLock();
            throw new InvalidConfigFileError(e.getMessage());
        }
    }

    public void publishBranchUpdate (ConfigRepo updatedRepo, ConfigRepoBranch updatedBranch) throws InvalidConfigFileError {
        acquireLock();
        this.reloadFromFile();
        this.getRepo(updatedRepo.repoPath).updateRepoBranch(updatedBranch.branchName, updatedBranch);
        try {
            this.writeYml();
            releaseLock();
        } catch (FileNotFoundException | InvalidYmlFileError | FileLockedError e) {
            releaseLock();
            throw new InvalidConfigFileError(e.getMessage());
        }
    }

    public void publishBranchRemoval (ConfigRepo configRepo, String branchName) throws InvalidConfigFileError {
        acquireLock();
        this.reloadFromFile();
        this.getRepo(configRepo.repoPath).deleteRepoBranch(branchName);
        try {
            this.writeYml();
            releaseLock();
        } catch (FileNotFoundException | InvalidYmlFileError | FileLockedError e) {
            releaseLock();
            throw new InvalidConfigFileError(e.getMessage());
        }
    }

    public boolean isRepoActive(String repoPath) {
        ConfigRepo repo = this.getRepo(repoPath);
        if (repo == null) {
            return false;
        }

        return repo.isActive();
    }
}
