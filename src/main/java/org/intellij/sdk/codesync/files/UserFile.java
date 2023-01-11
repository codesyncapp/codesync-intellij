package org.intellij.sdk.codesync.files;

import org.intellij.sdk.codesync.exceptions.FileNotCreatedError;
import org.intellij.sdk.codesync.exceptions.InvalidYmlFileError;
import org.intellij.sdk.codesync.utils.CommonUtils;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

import static org.intellij.sdk.codesync.Constants.USER_FILE_PATH;


public class UserFile extends CodeSyncYmlFile {
    File userFile;
    public Map<String, Object> contentsMap;
    public Map<String, User> users = new HashMap<>();

    public static class User {
        String userEmail, accessKey = null, secretKey = null, accessToken = null;
        Boolean isActive = false;

        public User(String userEmail, Map<String, Object> userCredentials) {
            this.userEmail = userEmail;
            this.accessKey = (String) userCredentials.getOrDefault("access_key", null);
            this.secretKey = (String) userCredentials.getOrDefault("secret_key", null);
            this.accessToken = (String) userCredentials.getOrDefault("access_token", null);
            this.isActive = CommonUtils.getBoolValue(userCredentials, "is_active", false);
        }

        /*
        Construct a user instance with Auth0 access token.
         */
        public User(String userEmail, String accessToken) {
            this.userEmail = userEmail;
            this.accessToken = accessToken;
            this.isActive = true;
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
        public void makeActive () { this.isActive = true; }
        public void makeInActive () { this.isActive = false; }


        public Map<String, Object> getYMLAsHashMap() {
            Map<String, Object> user = new HashMap<>();
            user.put("access_key", this.accessKey);
            user.put("secret_key", this.secretKey);
            user.put("access_token", this.accessToken);
            user.put("is_active", this.isActive);
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
        UserFile userFile;
        try {
            userFile = new UserFile(USER_FILE_PATH);
        } catch (FileNotFoundException | InvalidYmlFileError e) {
            return null;
        }
        UserFile.User user = email == null ? userFile.getActiveUser(): userFile.getActiveUser(email);
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
        if (this.contentsMap == null) {
            return;
        }
        try {
            for (Map.Entry<String, Object> userEntry : this.contentsMap.entrySet()) {
                if (userEntry.getValue() != null) {
                    Map<String, Object> userCredentials = (Map<String, Object>) userEntry.getValue();
                    this.users.put(userEntry.getKey(), new User(userEntry.getKey(), userCredentials));
                }

            }
        } catch (ClassCastException e){
            throw new InvalidYmlFileError(
                String.format("User yml file \"%s\" is not valid. Error: %s", this.getYmlFile().getPath(), e.getMessage())
            );
        }
    }

    /*
    Get the user matching the given email.
     */
    public User getUser(String userEmail) {
        return this.users.getOrDefault(userEmail, null);
    }

    /*
     Get the access token of the active user from the map.
     */
    public String getActiveAccessToken() {
        User user = this.getActiveUser();
        if (user != null) {
            return user.accessToken;
        }
        return null;
    }

    /*
    Get the active user from the map.
     */
    public User getActiveUser(String email) {
        User user = getUser(email);
        return user.isActive ? user: null;
    }

    /*
    Get the active user from the map.
     */
    public User getActiveUser() {
        for (User user: this.users.values()) {
            if (user.isActive) {
                return user;
            }
        }
        return null;
    }

    /*
    Set user using Auth0 access token.

    This will also make sure that this user is set to active and all other users are
    set to in-active via is_active flag.
     */
    public void setActiveUser(String userEmail, String accessToken) {
        User user = getUser(userEmail);
        if (user == null) {
            user = new User(userEmail, accessToken);
        } else {
            user.setAccessToken(accessToken);
        }
        // First mark all users in-active.
        this.makeAllUsersInActive();

        // Now mark the new user active.
        user.makeActive();
        this.users.put(userEmail, user);
    }

    /*
    Make all user's in-active by setting `is_active` to false.
     */
    public void makeAllUsersInActive() {
        for (User user: this.users.values()) {
            user.makeInActive();
        }
    }

    /*
    Set user using IAM access and secret.
     */
    public void setActiveUser(String userEmail, String iamAccessKey, String iamSecretKey) {
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
