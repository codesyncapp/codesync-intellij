package org.intellij.sdk.codesync.database.tables;

import org.intellij.sdk.codesync.database.Database;
import org.intellij.sdk.codesync.database.SQLiteConnection;
import org.intellij.sdk.codesync.database.models.User;
import org.intellij.sdk.codesync.database.queries.UserQueries;
import org.intellij.sdk.codesync.exceptions.SQLiteDBConnectionError;
import org.intellij.sdk.codesync.exceptions.SQLiteDataError;
import org.intellij.sdk.codesync.database.models.UserAccount;
import org.intellij.sdk.codesync.utils.Queries;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;

public class UserTable extends DBTable {
    static String tableName = "user";
    private final UserQueries userQueries;
    private static UserTable instance;

    private UserTable() {
        this.userQueries = new UserQueries(tableName);
    }

    public static UserTable getInstance() {
        if (instance == null) {
            instance = new UserTable();
        }
        return instance;
    }

    public String getTableName() {
        return tableName;
    }

    @Override
    protected String getCreateTableQuery() {
        return userQueries.getCreateTableQuery();
    }

    public User get(String email) throws SQLException {
        try (Statement statement = SQLiteConnection.getInstance().getConnection().createStatement()) {
            ResultSet resultSet = statement.executeQuery(this.userQueries.getSelectQuery(email));
            if (resultSet.isBeforeFirst()){
                return new User(
                        resultSet.getInt("id"),
                        resultSet.getString("email"),
                        resultSet.getString("access_token"),
                        resultSet.getString("access_key"),
                        resultSet.getString("secret_key"),
                        resultSet.getBoolean("is_active")
                );
            }

        }
        return null;
    }

    public User insert(User user) throws SQLException {
        try (Statement statement = SQLiteConnection.getInstance().getConnection().createStatement()) {
            statement.executeUpdate(
                this.userQueries.getInsertQuery(user.getEmail(), user.getAccessToken(), user.getAccessKey(), user.getSecretKey(), user.isActive())
            );
        }
        // return the user object with the id
        return get(user.getEmail());
    }

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
