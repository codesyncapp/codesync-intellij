package org.intellij.sdk.codesync.files;

import java.io.*;
import java.util.Map;
import java.util.HashMap;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import org.intellij.sdk.codesync.exceptions.InvalidConfigFileError;


public class ConfigFile {
    File configFile;
    Map<String, Object> contentsMap;
    public Map<String, ConfigRepo> repos = new HashMap<>();

    private void reloadFromFile() throws InvalidConfigFileError {
        this.contentsMap = this.readYml();
        this.loadYmlContent();
    }

    private void loadYmlContent () throws InvalidConfigFileError {
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

    public ConfigFile(String filePath) throws InvalidConfigFileError {
        File configFile = new File(filePath);

        if (!configFile.isFile()) {
            throw new InvalidConfigFileError("Config file path must be absolute path pointing to a file.");
        }

        this.configFile = configFile;
        this.contentsMap = this.readYml();

        try {
            this.loadYmlContent();
        } catch (InvalidConfigFileError e){
            throw new InvalidConfigFileError("Config file is not valid.");
        }
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

    private Map<String, Object> readYml() throws InvalidConfigFileError {
        Yaml yaml = new Yaml();
        InputStream inputStream;
        try {
            inputStream = new FileInputStream(this.configFile);
        } catch (FileNotFoundException e) {
            throw new InvalidConfigFileError("Config file must be present.");
        }
        return yaml.load(inputStream);
    }

    private void writeYml() throws InvalidConfigFileError {
        final DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);

        Yaml yaml = new Yaml(options);
        FileWriter writer;
        try {
            writer = new FileWriter(this.configFile);
        } catch (IOException e) {
            throw new InvalidConfigFileError("Error while writing to the config file.");
        }
        yaml.dump(this.getYMLAsHashMap(), writer);
    }

    public ConfigRepo getRepo(String repoPath) {
        return this.repos.get(repoPath);
    }

    public void updateRepo(String repoPath, ConfigRepo newRepo) {
        this.repos.put(repoPath, newRepo);
    }

    public void publishRepoUpdate (ConfigRepo updatedRepo) throws InvalidConfigFileError {
        this.reloadFromFile();
        this.updateRepo(updatedRepo.repoPath, updatedRepo);
        this.writeYml();
    }

    public void publishBranchUpdate (ConfigRepo updatedRepo, ConfigRepoBranch updatedBranch) throws InvalidConfigFileError {
        this.reloadFromFile();
        this.getRepo(updatedRepo.repoPath).updateRepoBranch(updatedBranch.branchName, updatedBranch);
        this.writeYml();
    }

    public void publishBranchRemoval (ConfigRepo updatedRepo, ConfigRepoBranch updatedBranch) throws InvalidConfigFileError {
        this.reloadFromFile();
        this.getRepo(updatedRepo.repoPath).deleteRepoBranch(updatedBranch.branchName);
        this.writeYml();
    }
}
