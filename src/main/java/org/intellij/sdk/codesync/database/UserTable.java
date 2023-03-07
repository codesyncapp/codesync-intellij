package org.intellij.sdk.codesync.database;

import org.intellij.sdk.codesync.Database;
import org.intellij.sdk.codesync.files.UserFile;

import java.util.ArrayList;
import java.util.HashMap;

public class UserTable {

    static String email;
    static String access_token;
    static String secret_key;
    static String access_key;
    static int is_active;

    public static void insertNewUser(UserFile.User user){
        setValues(user);
        String query = String.format("INSERT INTO user VALUES(%s, %s, %s, %s, %d)", email, access_token, secret_key, access_key, is_active);
        Database.executeUpdate(query);
    }

    public static void updateUser(UserFile.User user){
        setValues(user);
        String query = String.format("UPDATE user SET access_token = %s, secret_key = %s, access_key = %s, is_active = %d WHERE email = %s", access_token, secret_key, access_key, is_active, email);
        Database.executeUpdate(query);
    }

    public static void updateAllUsersInActive(){
        String query = "UPDATE user SET is_active = 0";
        Database.executeUpdate(query);
    }

    public static UserFile.User getActiveUser(){
        String query = "SELECT * FROM user WHERE is_active = 1";
        ArrayList<HashMap<String, Object>> usersArray = Database.runQuery(query);

        if(usersArray.size() > 0){
            UserFile.User user = new UserFile.User((String) usersArray.get(0).get("EMAIL"));
            user.setAccessKey((String) usersArray.get(0).getOrDefault("ACCESS_KEY", null));
            user.setAccessToken((String) usersArray.get(0).getOrDefault("ACCESS_TOKEN", null));
            user.setSecretKey((String) usersArray.get(0).getOrDefault("SECRET_KEY", null));
            if((String) usersArray.get(0).getOrDefault("IS_ACTIVE", null) == "1"){
                user.makeActive();
            }else {
                user.makeInActive();
            }
            return user;
        }

        return null;

    }

    public static UserFile.User getByEmail(String email){
        String query = String.format("SELECT * FROM user WHERE email = '%s'", email);
        ArrayList<HashMap<String, Object>> usersArray = Database.runQuery(query);

        if(usersArray.size() > 0){
            UserFile.User user = new UserFile.User((String) usersArray.get(0).get("EMAIL"));
            user.setAccessKey((String) usersArray.get(0).getOrDefault("ACCESS_KEY", null));
            user.setAccessToken((String) usersArray.get(0).getOrDefault("ACCESS_TOKEN", null));
            user.setSecretKey((String) usersArray.get(0).getOrDefault("SECRET_KEY", null));
            if((String) usersArray.get(0).getOrDefault("IS_ACTIVE", null) == "1"){
                user.makeActive();
            }else {
                user.makeInActive();
            }
            return user;
        }

        return null;
    }

    static void setValues(UserFile.User user){
        email = user.getUserEmail() != null? String.format("'%s'", user.getUserEmail()) : "NULL";
        access_token = user.getAccessToken() != null? String.format("'%s'", user.getAccessToken()) : "NULL";
        secret_key = user.getSecretKey() != null? String.format("'%s'", user.getSecretKey()) : "NULL";
        access_key = user.getAccessKey() != null? String.format("'%s'", user.getAccessKey()) : "NULL";
        if(user.getActive() != null){
            is_active = user.getActive()? 1 : 0;
        }else{
            is_active = 0;
        }
    }

}
