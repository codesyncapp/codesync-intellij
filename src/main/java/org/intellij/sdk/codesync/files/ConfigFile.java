package org.intellij.sdk.codesync.files;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Map;
import java.util.HashMap;

import org.yaml.snakeyaml.Yaml;

import org.intellij.sdk.codesync.exceptions.InvalidConfigFile;

import static org.intellij.sdk.codesync.Constants.CONFIG_PATH;


public class ConfigFile {
    File configFile;
    Map<String, Object> contentsMap;
    public Map<String, ConfigRepo> repos = new HashMap<>();

    public ConfigFile(String filePath) throws InvalidConfigFile {
        File configFile = new File(filePath);

        if (!configFile.isFile()) {
            throw new InvalidConfigFile("Config file path must be absolute path pointing to a file.");
        }

        this.configFile = configFile;
        this.contentsMap = this.readYml();

        try {
            Map<String, Map<String, Object>> repos = (Map<String, Map<String, Object>>) this.contentsMap.get("repos");

            for (Map.Entry<String, Map<String, Object>> repo : repos.entrySet()) {
                if (repo.getValue() != null){
                    this.repos.put(repo.getKey(), new ConfigRepo(repo.getKey(), repo.getValue()));
                }

            }
        } catch (ClassCastException e){
            throw new InvalidConfigFile("Config file is not valid.");
        }
    }

    public Map<String, Object> readYml() throws InvalidConfigFile {
        Yaml yaml = new Yaml();
        InputStream inputStream;
        try {
            inputStream = new FileInputStream(CONFIG_PATH);
        } catch (FileNotFoundException e) {
            throw new InvalidConfigFile("Config file must be valid yml.");
        }
        return yaml.load(inputStream);
    }

    public ConfigRepo getRepo(String repoPath) {
        return this.repos.get(repoPath);
    }
}
