package org.intellij.sdk.codesync.files;

import org.intellij.sdk.codesync.database.UserTable;
import org.intellij.sdk.codesync.exceptions.FileNotCreatedError;
import org.intellij.sdk.codesync.exceptions.InvalidYmlFileError;
import org.intellij.sdk.codesync.utils.CommonUtils;

import java.io.*;
import java.util.Map;

public class UserFile{


    public static class User {
        //New chagnes
        String userEmail, accessKey = null, secretKey = null, accessToken = null;
        Boolean isActive = false;

        public User(String userEmail, Map<String, Object> userCredentials) {
            this.userEmail = userEmail;
            this.accessKey = (String) userCredentials.getOrDefault("access_key", null);
            this.secretKey = (String) userCredentials.getOrDefault("secret_key", null);
            this.accessToken = (String) userCredentials.getOrDefault("access_token", null);
            this.isActive = CommonUtils.getBoolValue(userCredentials, "is_active", false);
        }

        public User(String userEmail) {
            this.userEmail = userEmail;
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

        public Boolean getActive() {
            return isActive;
        }
    }

    public UserFile (String filePath) throws FileNotFoundException, InvalidYmlFileError{
        //UserFile constructor 1
        System.out.println("UserFile constructor 1: " + filePath);
    }

    /*
    Instantiate a user file, create the file if it does not exist.
     */
    public UserFile (String filePath, boolean shouldCreateIfAbsent)throws FileNotFoundException, FileNotCreatedError, InvalidYmlFileError{
        //UserFile constructor 2
        System.out.println("UserFile constructor 2: " + filePath + " " + shouldCreateIfAbsent);
    }

    /*
    Get access token for the user or null.

    This is a utility method and can be called in places where it has already been made sure that file with correct
    access token will be present.
     */
    public static String getAccessToken(String email) {

        UserFile userFile;

        try{
            userFile = new UserFile("FakePath");
        } catch (InvalidYmlFileError e) {
            throw new RuntimeException(e);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        UserFile.User user = email == null ? userFile.getActiveUser(): userFile.getActiveUser(email);
        if (user != null) {
            return user.getAccessToken();
        }

        return null;
    }

   /*
    Get email of the default user or null.
     */
    public static String getEmail() {
        UserFile userFile;

        try{
            userFile = new UserFile("FakePath");
        } catch (InvalidYmlFileError e) {
            throw new RuntimeException(e);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        UserFile.User user =  userFile.getActiveUser();
        return user != null ? user.getUserEmail(): null;
    }

    /*
    Get access token for the first user in the file or null.

    This is a utility method and can be called in places where it has already been made sure that file with correct
    access token will be present.
     */
    public static String getAccessToken() {
        return getAccessToken(null);
    }

    /*
    Get the user matching the given email.
     */
    public User getUser(String userEmail) {
        return UserTable.getByEmail(userEmail);
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
        if(user != null){
            return user.isActive ? user: null;
        }
        return null;
    }

    /*
    Get the active user from the map.
     */
    public User getActiveUser() {
        return UserTable.getActiveUser();
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
            // First mark all users in-active.
            this.makeAllUsersInActive();
            // Now mark the new user active.
            user.makeActive();
            UserTable.insertNewUser(user);
        } else {
            user.setAccessToken(accessToken);
            // First mark all users in-active.
            this.makeAllUsersInActive();
            // Now mark the new user active.
            user.makeActive();
            UserTable.updateUser(user);
        }
    }

    /*
    Make all user's in-active by setting `is_active` to false.
     */
    public void makeAllUsersInActive() {
        UserTable.updateAllUsersInActive();
    }

    /*
    Set user using IAM access and secret.
     */
    public void setActiveUser(String userEmail, String iamAccessKey, String iamSecretKey) {
        User user = getUser(userEmail);
        if (user == null) {
            user = new User(userEmail, iamAccessKey, iamSecretKey);
            UserTable.insertNewUser(user);
        } else {
            user.setAccessKey(iamAccessKey);
            user.setSecretKey(iamSecretKey);
            UserTable.updateUser(user);
        }
    }

}
