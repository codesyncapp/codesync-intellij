package org.intellij.sdk.codesync.database.tables;

import org.intellij.sdk.codesync.database.Database;
import org.intellij.sdk.codesync.exceptions.SQLiteDBConnectionError;
import org.intellij.sdk.codesync.exceptions.SQLiteDataError;
import org.intellij.sdk.codesync.database.models.UserAccount;
import org.intellij.sdk.codesync.utils.Queries;

import java.util.ArrayList;
import java.util.HashMap;

public class UserTable {
    static String tableName = "user";

    public static void insertNewUser(UserAccount userAccount) throws SQLiteDBConnectionError, SQLiteDataError {
        Database.executeUpdate(Queries.User.insert(userAccount.getUserEmail(), userAccount.getAccessToken(), userAccount.getSecretKey(), userAccount.getAccessKey(), userAccount.getActive()));
    }

    public static void updateUser(UserAccount userAccount) throws SQLiteDBConnectionError, SQLiteDataError {
        Database.executeUpdate(Queries.User.update_by_email(userAccount.getAccessToken(), userAccount.getSecretKey(), userAccount.getAccessKey(), userAccount.getActive(), userAccount.getUserEmail()));
    }

    public static void updateAllUsersInActive() throws SQLiteDBConnectionError, SQLiteDataError {
        Database.executeUpdate(Queries.User.update_all_by_active_status(false));
    }

    public static UserAccount getActiveUser() throws SQLiteDBConnectionError, SQLiteDataError {

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

    public static UserAccount getByEmail(String email) throws SQLiteDBConnectionError, SQLiteDataError {

        ArrayList<HashMap<String, String>> usersArray = Database.runQuery(Queries.User.get_by_email(email));

        if(usersArray.size() > 0){
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
