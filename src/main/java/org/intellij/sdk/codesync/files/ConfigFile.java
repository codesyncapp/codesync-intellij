package org.intellij.sdk.codesync.files;

import java.io.*;
import java.util.Map;
import java.util.HashMap;

import org.intellij.sdk.codesync.exceptions.InvalidConfigFileError;
import org.intellij.sdk.codesync.exceptions.InvalidYmlFileError;


public class ConfigFile extends CodeSyncYmlFile {
    File configFile;
    Map<String, Object> contentsMap;
    public Map<String, ConfigRepo> repos = new HashMap<>();

    public ConfigFile(String filePath) throws InvalidConfigFileError {
        File configFile = new File(filePath);

        if (!configFile.isFile()) {
            throw new InvalidConfigFileError("Config file path must be absolute path pointing to a file.");
        }

        this.configFile = configFile;

        try {
            this.contentsMap = this.readYml();
        } catch (FileNotFoundException | InvalidYmlFileError e) {
            throw new InvalidConfigFileError(e.getMessage());
        }

        try {
            this.loadYmlContent();
        } catch (InvalidConfigFileError e){
            throw new InvalidConfigFileError("Config file is not valid.");
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

    private void reloadFromFile() throws InvalidConfigFileError {
        try {
            this.contentsMap = this.readYml();
        } catch (FileNotFoundException | InvalidYmlFileError e) {
            throw new InvalidConfigFileError(e.getMessage());
        }
        this.loadYmlContent();
    }

    private void loadYmlContent () throws InvalidConfigFileError {
        if (!this.contentsMap.containsKey("repos")) {
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
            throw new InvalidConfigFileError("Config file is not valid.");
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

    public void publishRepoUpdate (ConfigRepo updatedRepo) throws InvalidConfigFileError {
        this.reloadFromFile();
        this.updateRepo(updatedRepo.repoPath, updatedRepo);
        try {
            this.writeYml();
        } catch (FileNotFoundException | InvalidYmlFileError e) {
            throw new InvalidConfigFileError(e.getMessage());
        }
    }

    public void publishBranchUpdate (ConfigRepo updatedRepo, ConfigRepoBranch updatedBranch) throws InvalidConfigFileError {
        this.reloadFromFile();
        this.getRepo(updatedRepo.repoPath).updateRepoBranch(updatedBranch.branchName, updatedBranch);
        try {
            this.writeYml();
        } catch (FileNotFoundException | InvalidYmlFileError e) {
            throw new InvalidConfigFileError(e.getMessage());
        }
    }

    public void publishFileRemoval (ConfigRepo updatedRepo, ConfigRepoBranch updatedBranch) throws InvalidConfigFileError {
        this.reloadFromFile();
        this.getRepo(updatedRepo.repoPath).deleteRepoBranch(updatedBranch.branchName);
        try {
            this.writeYml();
        } catch (FileNotFoundException | InvalidYmlFileError e) {
            throw new InvalidConfigFileError(e.getMessage());
        }
    }

    public boolean isRepoDisconnected(String repoPath) {
        ConfigRepo repo = this.getRepo(repoPath);
        if (repo == null) {
            return true;
        }

        return repo.isDisconnected;
    }
}
