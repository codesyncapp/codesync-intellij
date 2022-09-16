package org.intellij.sdk.codesync.files;

import com.google.common.io.CharStreams;
import org.intellij.sdk.codesync.CodeSyncLogger;
import org.intellij.sdk.codesync.exceptions.FileLockedError;
import org.intellij.sdk.codesync.exceptions.InvalidYmlFileError;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
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
            return yaml.load(text);
        } catch (IOException | YAMLException e) {
            throw new InvalidYmlFileError(e.getMessage());
        }
    }

    public void writeYml() throws FileNotFoundException, InvalidYmlFileError, FileLockedError {
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
        try {
            FileWriter writer = new FileWriter(ymlFile);
            yaml.dump(yamlConfig, writer);
        } catch (IOException | YAMLException e) {
            throw new InvalidYmlFileError(e.getMessage());
        }
    }

    public static boolean createFile(String filePath) {
        return createFile(filePath, "{}");
    }

    /*
    Create an empty yml file at the specified path.

    @return true if file created successfully, false otherwise.
     */
    public static boolean createFile(String filePath, String fileContents) {
        try {
            File file = new File(filePath);
            if (file.createNewFile()) {
                FileWriter fileWriter = new FileWriter(file);

                // Write empty yml dict.
                fileWriter.write(fileContents);
                fileWriter.close();

                return true;
            }
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

        return false;
    }

    /*
    Remove the contents of the file and replace with empty dict, this is useful when invalid yaml error is raised.
    */
    public void removeFileContents() {
        try {
            File ymlFile = this.getYmlFile();
            if (!ymlFile.exists()) {
                if (!ymlFile.createNewFile()){
                    CodeSyncLogger.critical(String.format(
                            "Could not create a yml for while removing its contents with name '%s'.",
                            this.getYmlFile().getPath()
                    ));
                }
            }
            RandomAccessFile randomAccessFile = new RandomAccessFile(ymlFile, "rw");
            FileChannel fileChannel = randomAccessFile.getChannel();
            FileLock fileLock = fileChannel.tryLock();
            if (fileLock != null) {
                writeEmptyDictToFile(ymlFile, fileLock);
            }
        } catch (IOException | OverlappingFileLockException e) {
            // Ignore errors
            CodeSyncLogger.error(String.format(
                    "Error while removing the contents of the yml file with name '%s'. Error: %s",
                    this.getYmlFile().getPath(), e.getMessage()
            ));
        }
    }

    private void writeEmptyDictToFile(File ymlFile, FileLock fileLock) throws IOException {
        try {
            FileWriter writer = new FileWriter(ymlFile);
            writer.write("{}");
        } catch (IOException | YAMLException e) {
            // Ignore errors
            CodeSyncLogger.error(String.format(
                    "Error while writing empty dict to the yml file with name '%s'. Error: %s",
                    ymlFile.getPath(), e.getMessage()
            ));
        } finally {
            fileLock.release();
        }
    }
}
