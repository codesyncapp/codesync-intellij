package org.intellij.sdk.codesync.database;

import org.intellij.sdk.codesync.models.UserAccount;

import java.util.ArrayList;
import java.util.HashMap;

public class UserTable {

    static String email;
    static String access_token;
    static String secret_key;
    static String access_key;
    static int is_active;

    public static void insertNewUser(UserAccount userAccount){
        setValues(userAccount);
        String query = String.format("INSERT INTO user VALUES(%s, %s, %s, %s, %d)", email, access_token, secret_key, access_key, is_active);
        Database.executeUpdate(query);
    }

    public static void updateUser(UserAccount userAccount){
        setValues(userAccount);
        String query = String.format("UPDATE user SET access_token = %s, secret_key = %s, access_key = %s, is_active = %d WHERE email = %s", access_token, secret_key, access_key, is_active, email);
        Database.executeUpdate(query);
    }

    public static void updateAllUsersInActive(){
        String query = "UPDATE user SET is_active = 0";
        Database.executeUpdate(query);
    }

    public static UserAccount getActiveUser(){
        String query = "SELECT * FROM user WHERE is_active = 1";
        ArrayList<HashMap<String, Object>> usersArray = Database.runQuery(query);

        if(usersArray.size() > 0){
            UserAccount userAccount = new UserAccount((String) usersArray.get(0).get("EMAIL"));
            userAccount.setAccessKey((String) usersArray.get(0).getOrDefault("ACCESS_KEY", null));
            userAccount.setAccessToken((String) usersArray.get(0).getOrDefault("ACCESS_TOKEN", null));
            userAccount.setSecretKey((String) usersArray.get(0).getOrDefault("SECRET_KEY", null));
            if(((String)usersArray.get(0).getOrDefault("IS_ACTIVE", null)).equals("1")){
                userAccount.makeActive();
            }else {
                userAccount.makeInActive();
            }
            return userAccount;
        }

        return null;

    }

    public static UserAccount getByEmail(String email){
        String query = String.format("SELECT * FROM user WHERE email = '%s'", email);
        ArrayList<HashMap<String, Object>> usersArray = Database.runQuery(query);

        if(usersArray.size() > 0){
            UserAccount userAccount = new UserAccount((String) usersArray.get(0).get("EMAIL"));
            userAccount.setAccessKey((String) usersArray.get(0).getOrDefault("ACCESS_KEY", null));
            userAccount.setAccessToken((String) usersArray.get(0).getOrDefault("ACCESS_TOKEN", null));
            userAccount.setSecretKey((String) usersArray.get(0).getOrDefault("SECRET_KEY", null));
            if(((String)usersArray.get(0).getOrDefault("IS_ACTIVE", null)).equals("1")){
                userAccount.makeActive();
            }else {
                userAccount.makeInActive();
            }
            return userAccount;
        }

        return null;
    }

    static void setValues(UserAccount userAccount){
        email = userAccount.getUserEmail() != null? String.format("'%s'", userAccount.getUserEmail()) : "NULL";
        access_token = userAccount.getAccessToken() != null? String.format("'%s'", userAccount.getAccessToken()) : "NULL";
        secret_key = userAccount.getSecretKey() != null? String.format("'%s'", userAccount.getSecretKey()) : "NULL";
        access_key = userAccount.getAccessKey() != null? String.format("'%s'", userAccount.getAccessKey()) : "NULL";
        if(userAccount.getActive() != null){
            is_active = userAccount.getActive()? 1 : 0;
        }else{
            is_active = 0;
        }
    }

}
