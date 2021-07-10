package org.intellij.sdk.codesync.files;

import org.intellij.sdk.codesync.exceptions.InvalidYmlFileError;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


public class UserFile extends CodeSyncYmlFile {
    File userFile;
    public Map<String, Object> contentsMap;
    public Map<String, User> users;

    public static class User {
        String userEmail, accessKey, secretKey;

        public User(String userEmail, Map<String, String> userCredentials) {
            this.userEmail = userEmail;
            this.accessKey = userCredentials.getOrDefault("access_key", null);
            this.secretKey = userCredentials.getOrDefault("secret_key", null);
        }

        public String getUserEmail () {
            return this.userEmail;
        }

        public String getAccessKey () {
            return this.accessKey;
        }

        public String getSecretKey () {
            return this.secretKey;
        }

        public Map<String, String> getYMLAsHashMap() {
            Map<String, String> user = new HashMap<>();
            user.put("access_key", this.accessKey);
            user.put("secret_key", this.secretKey);
            return user;
        }
    }

    public UserFile (String filePath) throws FileNotFoundException, InvalidYmlFileError {
        File userFile = new File(filePath);

        if (!userFile.isFile()) {
            throw new FileNotFoundException(String.format("User file \"%s\" does not exist.", filePath));
        }
        this.userFile = userFile;
        this.contentsMap = this.readYml();
        this.loadYmlContent();
    }

    public File getYmlFile()  {
        return this.userFile;
    }

    public Map<String, Object> getYMLAsHashMap() {
        Map<String, Object> contents = new HashMap<>();
        for (User user: this.users.values()) {
            contents.put(user.getUserEmail(), user.getYMLAsHashMap());
        }
        return contents;
    }

    private void loadYmlContent () throws InvalidYmlFileError {
        try {
            for (Map.Entry<String, Object> userEntry : this.contentsMap.entrySet()) {
                if (userEntry.getValue() != null) {
                    Map<String, String> userCredentials = (Map<String, String>) userEntry.getValue();
                    this.users.put(userEntry.getKey(), new User(userEntry.getKey(), userCredentials));
                }

            }
        } catch (ClassCastException e){
            throw new InvalidYmlFileError(String.format("User yml file \"%s\" is not valid.", this.getYmlFile().getPath()));
        }
    }

    /*
    Get the user matching the given email.
     */
    public User getUser(String userEmail) {
        return this.users.getOrDefault(userEmail, null);
    }

    /*
    Get the first user from the map.
     */
    public User getUser() {
        Optional<String> firstKey = this.users.keySet().stream().findFirst();
        return firstKey.map(this::getUser).orElse(null);
    }
}
