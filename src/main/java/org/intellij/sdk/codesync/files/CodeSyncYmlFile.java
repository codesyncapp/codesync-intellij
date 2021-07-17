package org.intellij.sdk.codesync.files;

import com.google.common.io.CharStreams;
import org.intellij.sdk.codesync.exceptions.InvalidYmlFileError;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.Map;

/*
This is the base class that serves as the starting point for all yml files used in the code sync,
it provides common functionality like reading from and writing to yml files and an interface that
subclasses must implement.
 */
abstract public class CodeSyncYmlFile {
    // This method must return the yml file that is under observation.
    abstract public File getYmlFile();
    // This must return the contents of the file as a HashMap, this hashmap will be used to write to the yml file.
    abstract public Map<String, Object> getYMLAsHashMap();

    public Map<String, Object> readYml() throws FileNotFoundException, InvalidYmlFileError {
        Yaml yaml = new Yaml();
        InputStream inputStream;
        inputStream = new FileInputStream(this.getYmlFile());
        String text = null;

        try (Reader reader = new InputStreamReader(inputStream)) {
            text = CharStreams.toString(reader);
        } catch (IOException e) {
            throw new InvalidYmlFileError(e.getMessage());
        }

        return yaml.load(text);
    }

    public void writeYml() throws FileNotFoundException, InvalidYmlFileError {
        final DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);

        Yaml yaml = new Yaml(options);

        // This line of code is placed here so that in  case of any error we do not write to the config file.
        Map<String, Object> yamlConfig = this.getYMLAsHashMap();
        File ymlFile = this.getYmlFile();
        if (!ymlFile.exists()) {
            throw new FileNotFoundException(String.format("Yml file \"%s\" not found.", ymlFile.getPath()));
        }

        FileWriter writer;
        try {
            writer = new FileWriter(ymlFile);
        } catch (IOException e) {
            throw new InvalidYmlFileError(e.getMessage());
        }

        yaml.dump(yamlConfig, writer);
    }

}