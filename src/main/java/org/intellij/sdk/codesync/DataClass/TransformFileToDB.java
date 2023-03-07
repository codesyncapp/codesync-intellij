package org.intellij.sdk.codesync.DataClass;

import com.google.common.io.CharStreams;
import org.intellij.sdk.codesync.Database;
import org.intellij.sdk.codesync.exceptions.InvalidYmlFileError;
import org.intellij.sdk.codesync.files.UserFile;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

import static org.intellij.sdk.codesync.Constants.USER_FILE_PATH;
public class TransformFileToDB {

    File userFile = new File(USER_FILE_PATH);
    public Map<String, Object> contentsMap;
    public Map<String, UserFile.User> users = new HashMap<>();

    private void readYml() throws FileNotFoundException, InvalidYmlFileError {
        Yaml yaml = new Yaml();
        InputStream inputStream;
        inputStream = new FileInputStream(userFile);
        String text = null;

        try (Reader reader = new InputStreamReader(inputStream)) {
            text = CharStreams.toString(reader);
            this.contentsMap = yaml.load(text);
        } catch (IOException | YAMLException e) {
            throw new InvalidYmlFileError(e.getMessage());
        }
    }

    private void loadYmlContent () throws InvalidYmlFileError {
        if (this.contentsMap == null) {
            return;
        }
        try {
            for (Map.Entry<String, Object> userEntry : this.contentsMap.entrySet()) {
                if (userEntry.getValue() != null) {
                    Map<String, Object> userCredentials = (Map<String, Object>) userEntry.getValue();
                    this.users.put(userEntry.getKey(), new UserFile.User(userEntry.getKey(), userCredentials));
                }
            }
        } catch (ClassCastException e){
            throw new InvalidYmlFileError(
                    String.format("User yml file \"%s\" is not valid. Error: %s", this.userFile.getPath(), e.getMessage())
            );
        }
    }

    public void readUsersInFile(){
        try{
            readYml();
            loadYmlContent();
            for(String key : users.keySet()){
                UserTable.insertNewUser(users.get(key));
            }
        } catch (InvalidYmlFileError e) {
            throw new RuntimeException(e);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

    }

}
