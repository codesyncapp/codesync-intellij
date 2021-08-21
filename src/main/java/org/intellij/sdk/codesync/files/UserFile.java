package org.intellij.sdk.codesync.files;

import org.intellij.sdk.codesync.exceptions.FileNotCreatedError;
import org.intellij.sdk.codesync.exceptions.InvalidYmlFileError;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.intellij.sdk.codesync.Constants.USER_FILE_PATH;


public class UserFile extends CodeSyncYmlFile {
    File userFile;
    public Map<String, Object> contentsMap;
    public Map<String, User> users = new HashMap<>();

    public static class User {
        String userEmail, accessKey = null, secretKey = null, accessToken = null;

        public User(String userEmail, Map<String, String> userCredentials) {
            this.userEmail = userEmail;
            this.accessKey = userCredentials.getOrDefault("access_key", null);
            this.secretKey = userCredentials.getOrDefault("secret_key", null);
            this.accessToken = userCredentials.getOrDefault("access_token", null);
        }

        /*
        Construct a user instance with Auth0 access token.
         */
        public User(String userEmail, String accessToken) {
            this.userEmail = userEmail;
            this.accessToken = accessToken;
        }

        /*
        Construct a user instance with IAM access and secret.
         */
        public User(String userEmail, String iamAccessKey, String iamSecretKey) {
            this.userEmail = userEmail;
            this.accessKey = iamAccessKey;
            this.secretKey = iamSecretKey;
        }

        public String getUserEmail () {
            return this.userEmail;
        }
        public String getAccessKey () { return this.accessKey; }
        public String getAccessToken () { return this.accessToken; }
        public String getSecretKey () {
            return this.secretKey;
        }

        public void setAccessKey (String accessKey) { this.accessKey = accessKey; }
        public void setAccessToken (String accessToken) { this.accessToken = accessToken; }
        public void setSecretKey (String secretKey) { this.secretKey = secretKey; }


        public Map<String, String> getYMLAsHashMap() {
            Map<String, String> user = new HashMap<>();
            user.put("access_key", this.accessKey);
            user.put("secret_key", this.secretKey);
            user.put("access_token", this.accessToken);
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

    /*
    Instantiate a user file, create the file if it does not exist.
     */
    public UserFile (String filePath, boolean shouldCreateIfAbsent) throws FileNotFoundException, FileNotCreatedError, InvalidYmlFileError {
        File userFile = new File(filePath);

        if (!userFile.isFile() && shouldCreateIfAbsent) {
            boolean isFileReady = createFile(filePath);
            if (!isFileReady) {
                throw new FileNotCreatedError(String.format("User file \"%s\" could not be created.", filePath));
            }
        } else if(!userFile.isFile()) {
            throw new FileNotFoundException(String.format("User file \"%s\" does not exist.", filePath));
        }

        this.userFile = userFile;
        this.contentsMap = this.readYml();
        this.loadYmlContent();
    }

    /*
    Get access token for the user or null.

    This is a utility method and can be called in places where it has already been made sure that file with correct
    access token will be present.
     */
    public static String getAccessToken(String email) {
        UserFile userFile = null;
        try {
            userFile = new UserFile(USER_FILE_PATH);
        } catch (FileNotFoundException | InvalidYmlFileError e) {
            return null;
        }
        UserFile.User user = email == null ? userFile.getUser(): userFile.getUser(email);
        if (user != null) {
            return user.getAccessToken();
        }

        return null;
    }

    /*
    Get access token for the first user in the file or null.

    This is a utility method and can be called in places where it has already been made sure that file with correct
    access token will be present.
     */
    public static String getAccessToken() {
        return getAccessToken(null);
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

    /*
    Set user using Auth0 access token
     */
    public void setUser (String userEmail, String accessToken) {
        User user = getUser(userEmail);
        if (user == null) {
            user = new User(userEmail, accessToken);
        } else {
            user.setAccessToken(accessToken);
        }
        this.users.put(userEmail, user);
    }

    /*
    Set user using IAM access and secret.
     */
    public void setUser (String userEmail, String iamAccessKey, String iamSecretKey) {
        User user = getUser(userEmail);
        if (user == null) {
            user = new User(userEmail, iamAccessKey, iamSecretKey);
        } else {
            user.setAccessKey(iamAccessKey);
            user.setSecretKey(iamSecretKey);
        }
        this.users.put(userEmail, user);
    }

}
