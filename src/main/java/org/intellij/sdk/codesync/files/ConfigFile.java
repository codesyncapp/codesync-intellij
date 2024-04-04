package org.intellij.sdk.codesync.files;

import java.io.*;
import java.util.Map;
import java.util.HashMap;

import org.intellij.sdk.codesync.CodeSyncLogger;
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
        } catch (InvalidYmlFileError e) {
            CodeSyncLogger.error("[CONFIG_FILE]: Config file facing invalid yaml error.");
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
    public Map<String, ConfigRepo> getRepos() {
        return this.repos;
    }

}
