package org.intellij.sdk.codesync.files;

import org.intellij.sdk.codesync.Utils;
import org.intellij.sdk.codesync.exceptions.InvalidConfigFileError;
import org.intellij.sdk.codesync.utils.CommonUtils;

import java.util.HashMap;
import java.util.Map;

public class ConfigRepo {
    public String repoPath;
    public String email;
    public Integer id;
    public String token;
    public Boolean isInSync;
    public Boolean isDeleted;
    public Boolean pauseNotification;
    public Boolean isDisconnected;

    public Map<String, ConfigRepoBranch> branches = new HashMap<>();

    public ConfigRepo (String repoPath) {
        this.repoPath = repoPath;
    }

    public ConfigRepo (String repoPath, Map<String, Object> configRepoMap) throws InvalidConfigFileError {
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
            throw new InvalidConfigFileError("Config file is not valid.");
        }

        this.token = (String) configRepoMap.get("token");

        this.isInSync = CommonUtils.getBoolValue(configRepoMap, "is_in_sync", true);
        this.isDeleted = CommonUtils.getBoolValue(configRepoMap, "is_deleted", false);
        this.isDisconnected = CommonUtils.getBoolValue(configRepoMap, "is_disconnected", false);
        this.pauseNotification = CommonUtils.getBoolValue(configRepoMap, "pause_notification", false);
    }

    public Map<String, Object> getYMLAsHashMap() {
        Map<String, Map<String, Integer>> branches = new HashMap<>();
        for (Map.Entry<String, ConfigRepoBranch> branch : this.branches.entrySet()) {
            branches.put(branch.getKey(), branch.getValue().getYMLAsHashMap());
        }
        Map<String, Object> repo = new HashMap<>();
        repo.put("branches", branches);
        repo.put("email", this.email);
        repo.put("id", this.id);
        repo.put("token", this.token);
        repo.put("is_deleted", this.isDeleted);
        repo.put("is_disconnected", this.isDisconnected);
        repo.put("pause_notification", this.pauseNotification);
        return repo;
    }

    public ConfigRepoBranch getRepoBranch(String branchName) {
        return this.branches.get(branchName);
    }
    public boolean containsBranch(String branchName) { return this.branches.containsKey(branchName); }
    public boolean hasValidEmail() {
        return this.email != null && !this.email.isEmpty();
    }

    public void updateRepoBranch(String branchName, ConfigRepoBranch newBranch) {
        this.branches.put(branchName, newBranch);
    }

    public void deleteRepoBranch(String branchName) {
        this.branches.remove(branchName);
    }

    public boolean isSuccessfullySynced() {
        String branchName = Utils.GetGitBranch(this.repoPath);
        if (!this.branches.containsKey(branchName)) {
            // If branch is not synced, daemon will take care of it.
            return true;
        }

        ConfigRepoBranch configRepoBranch = this.getRepoBranch(branchName);

        // If there is any invalid file then it mean repo was not synced successfully.
        return !configRepoBranch.hasInvalidFiles();
    }
}
