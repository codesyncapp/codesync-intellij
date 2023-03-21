package org.intellij.sdk.codesync.database;

import org.intellij.sdk.codesync.models.UserAccount;
import org.intellij.sdk.codesync.utils.Queries;

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

    public static int updateUser(UserAccount userAccount){
        setValues(userAccount);
        String query = String.format("UPDATE user SET access_token = %s, secret_key = %s, access_key = %s, is_active = %d WHERE email = %s", access_token, secret_key, access_key, is_active, email);
        return Database.executeUpdate(query);
    }

    public static void updateAllUsersInActive(){
        String query = "UPDATE user SET is_active = 0";
        Database.executeUpdate(query);
    }

    public static UserAccount getActiveUser(){
        ArrayList<HashMap<String, String>> usersArray = Database.runQuery(Queries.User.get_by_active_status(true));

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
        ArrayList<HashMap<String, String>> usersArray = Database.runQuery(Queries.User.get_by_email(email));

        if(usersArray != null){
            UserAccount userAccount = new UserAccount((String) usersArray.get(0).get("EMAIL"));
            userAccount.setAccessToken((String) usersArray.get(0).getOrDefault("ACCESS_TOKEN", null));
            userAccount.setSecretKey((String) usersArray.get(0).getOrDefault("SECRET_KEY", null));
            userAccount.setAccessKey((String) usersArray.get(0).getOrDefault("ACCESS_KEY", null));
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
