package org.intellij.sdk.codesync.models;

import org.intellij.sdk.codesync.database.Database;
import org.intellij.sdk.codesync.database.UserTable;
import org.intellij.sdk.codesync.exceptions.SQLiteDBConnectionError;
import org.intellij.sdk.codesync.exceptions.SQLiteDataError;
import org.intellij.sdk.codesync.utils.CommonUtils;

import java.util.Map;

public class UserAccount {

    public UserAccount() throws SQLiteDBConnectionError {
        if(!Database.isConnected()){
            throw new SQLiteDBConnectionError("SQLite database connection error!");
        }
    }

    String userEmail, accessKey = null, secretKey = null, accessToken = null;
    Boolean isActive = false;

    public UserAccount(String userEmail, Map<String, Object> userCredentials) {
        this.userEmail = userEmail;
        this.accessKey = (String) userCredentials.getOrDefault("access_key", null);
        this.secretKey = (String) userCredentials.getOrDefault("secret_key", null);
        this.accessToken = (String) userCredentials.getOrDefault("access_token", null);
        this.isActive = CommonUtils.getBoolValue(userCredentials, "is_active", false);
    }

    public UserAccount(String userEmail) {
        this.userEmail = userEmail;
    }

    /*
    Construct a user instance with Auth0 access token.
     */
    public UserAccount(String userEmail, String accessToken) {
        this.userEmail = userEmail;
        this.accessToken = accessToken;
        this.isActive = true;
    }

    /*
    Construct a user instance with IAM access and secret.
     */
    public UserAccount(String userEmail, String iamAccessKey, String iamSecretKey) {
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

    /*
    Get access token for the user or null.

    This is a utility method and can be called in places where it has already been made sure that file with correct
    access token will be present.
     */
    public static String getAccessToken(String email) {

        UserAccount userAccount;
        try {
            userAccount = new UserAccount();
        } catch (SQLiteDBConnectionError e) {
            return null;
        }

        userAccount = email == null ? userAccount.getActiveUser(): userAccount.getActiveUser(email);
        if (userAccount != null) {
            return userAccount.getAccessToken();
        }

        return null;
    }

   /*
    Get email of the default user or null.
     */
    public static String getEmail() {
        UserAccount userAccount;
        try {
            userAccount = new UserAccount();
        } catch (SQLiteDBConnectionError e) {
            return null;
        }
        return userAccount.getActiveUser() != null ? userAccount.getActiveUser().getUserEmail(): null;
    }

    /*
    Get access token for the first user in the file or null.

    This is a utility method and can be called in places where it has already been made sure that file with correct
    access token will be present.
     */
    public static String getAccessTokenByEmail() {
        return getAccessToken(null);
    }

    /*
    Get the user matching the given email.
     */
    public UserAccount getUser(String userEmail) {
        try {
            return UserTable.getByEmail(userEmail);
        } catch (SQLiteDataError | SQLiteDBConnectionError e) {
            return null;
        }
    }

    /*
     Get the access token of the active user from the map.
     */
    public String getActiveAccessToken() {
        UserAccount userAccount = this.getActiveUser();
        if (userAccount != null) {
            return userAccount.accessToken;
        }
        return null;
    }

    /*
    Get the active user from the map.
     */
    public UserAccount getActiveUser(String email) {
        UserAccount userAccount = getUser(email);
        if(userAccount != null){
            return userAccount.isActive ? userAccount : null;
        }
        return null;
    }

    /*
    Get the active user from the map.
     */
    public UserAccount getActiveUser() {
        try {
            return UserTable.getActiveUser();
        } catch (SQLiteDataError | SQLiteDBConnectionError e) {
            return null;
        }
    }

    /*
    Set user using Auth0 access token.

    This will also make sure that this user is set to active and all other users are
    set to in-active via is_active flag.
     */
    public void setActiveUser(String userEmail, String accessToken) throws SQLiteDataError, SQLiteDBConnectionError {
        UserAccount userAccount = getUser(userEmail);
        if (userAccount == null) {
            userAccount = new UserAccount(userEmail, accessToken);
            // First mark all users in-active.
            this.makeAllUsersInActive();
            // Now mark the new user active.
            userAccount.makeActive();
            UserTable.insertNewUser(userAccount);
        } else {
            userAccount.setAccessToken(accessToken);
            // First mark all users in-active.
            this.makeAllUsersInActive();
            // Now mark the new user active.
            userAccount.makeActive();
            UserTable.updateUser(userAccount);
        }
    }

    /*
    Make all user's in-active by setting `is_active` to false.
     */
    public void makeAllUsersInActive() throws SQLiteDataError, SQLiteDBConnectionError {
        UserTable.updateAllUsersInActive();
    }

    /*
    Set user using IAM access and secret.
     */
    public void setActiveUser(String userEmail, String iamAccessKey, String iamSecretKey) throws SQLiteDataError, SQLiteDBConnectionError {
        UserAccount userAccount = getUser(userEmail);
        if (userAccount == null) {
            userAccount = new UserAccount(userEmail, iamAccessKey, iamSecretKey);
            UserTable.insertNewUser(userAccount);
        } else {
            userAccount.setAccessKey(iamAccessKey);
            userAccount.setSecretKey(iamSecretKey);
            UserTable.updateUser(userAccount);
        }
    }

}
