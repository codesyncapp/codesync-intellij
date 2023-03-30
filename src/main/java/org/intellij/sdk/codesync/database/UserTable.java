package org.intellij.sdk.codesync.database;

import org.intellij.sdk.codesync.models.UserAccount;
import org.intellij.sdk.codesync.utils.Queries;

import java.util.ArrayList;
import java.util.HashMap;

public class UserTable {

    public static void insertNewUser(UserAccount userAccount){
        Database.executeUpdate(Queries.User.insert(userAccount.getUserEmail(), userAccount.getAccessToken(), userAccount.getSecretKey(), userAccount.getAccessKey(), userAccount.getActive()));
    }

    public static int updateUser(UserAccount userAccount){
        return Database.executeUpdate(Queries.User.update_by_email(userAccount.getAccessToken(), userAccount.getSecretKey(), userAccount.getAccessKey(), userAccount.getActive(), userAccount.getUserEmail()));
    }

    public static void updateAllUsersInActive(){
        Database.executeUpdate(Queries.User.update_all_by_active_status(false));
    }

    public static UserAccount getActiveUser(){
        ArrayList<HashMap<String, String>> usersArray = Database.runQuery(Queries.User.get_by_active_status(true));

        if(usersArray != null){
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

}
