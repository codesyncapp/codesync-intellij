package org.intellij.sdk.codesync.files;

import org.intellij.sdk.codesync.Utils;
import org.intellij.sdk.codesync.exceptions.InvalidConfigFile;

import java.util.HashMap;
import java.util.Map;
import java.util.Date;


public class ConfigRepo {
    public String repoPath;
    public String email;
    public Integer id;
    public Date lastSyncedAt;
    public String token;
    public Boolean isInSync;
    public Boolean isDeleted;
    public Boolean pauseNotification;

    public Map<String, ConfigRepoBranch> branches = new HashMap<>();

    public ConfigRepo (String repoPath, Map<String, Object> configRepoMap) throws InvalidConfigFile {
        this.repoPath = repoPath;

        try {
            Map<String, Map<String, Integer>> branches = (Map<String, Map<String, Integer>>) configRepoMap.get("branches");

            if (branches != null) {
                for (Map.Entry<String, Map<String, Integer>> branch : branches.entrySet()) {
                    this.branches.put(branch.getKey(), new ConfigRepoBranch(branch.getKey(), branch.getValue()));
                }
            }
            this.email = (String) configRepoMap.get("email");
            this.id = (Integer) configRepoMap.get("id");
        } catch (ClassCastException e){
            throw new InvalidConfigFile("Config file is not valid.");
        }

        try {
            this.lastSyncedAt = (Date) configRepoMap.get("last_synced_at");
        } catch (ClassCastException e) {
            this.lastSyncedAt = Utils.parseDate((String) configRepoMap.get("last_synced_at"));
        }

        this.token = (String) configRepoMap.get("token");

        isInSync = (Boolean) configRepoMap.get("is_in_sync");
        this.isInSync = isInSync != null ? isInSync: true;

        isDeleted = (Boolean) configRepoMap.get("is_deleted");
        this.isDeleted = isDeleted != null ? isDeleted: false;

        pauseNotification = (Boolean) configRepoMap.get("pause_notification");
        this.pauseNotification = pauseNotification != null ? pauseNotification: false;
    }

    public Map<String, Map<String, Map<String, Integer>>> getYMLAsHashMap() {
        Map<String, Map<String, Integer>> branches = new HashMap<>();
        for (Map.Entry<String, ConfigRepoBranch> branch : this.branches.entrySet()) {
            branches.put(branch.getKey(), branch.getValue().getYMLAsHashMap());
        }
        Map<String, Map<String, Map<String, Integer>>> repo = new HashMap<>();
        repo.put("branches", branches);
        return repo;
    }

    public ConfigRepoBranch getRepoBranch(String branchName) {
        return this.branches.get(branchName);
    }

    public void updateRepoBranch(String branchName, ConfigRepoBranch newBranch) {
        this.branches.put(branchName, newBranch);
    }

    public void deleteRepoBranch(String branchName) {
        this.branches.remove(branchName);
    }
}
