package org.intellij.sdk.codesync.database.tables;

import org.intellij.sdk.codesync.CodeSyncLogger;
import org.intellij.sdk.codesync.database.SQLiteConnection;
import org.intellij.sdk.codesync.database.models.User;
import org.intellij.sdk.codesync.database.queries.UserQueries;
import org.intellij.sdk.codesync.exceptions.database.UserNotFound;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

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

    public User get(String email) throws SQLException, UserNotFound {
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
        throw new UserNotFound(String.format("User with email '%s' not found.", email));
    }

    public User get(Integer userId) throws SQLException, UserNotFound {
        try (Statement statement = SQLiteConnection.getInstance().getConnection().createStatement()) {
            ResultSet resultSet = statement.executeQuery(this.userQueries.getSelectQuery(userId));
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
        throw new UserNotFound(String.format("User with id '%s' not found.", userId));
    }

    public User find(String email) throws SQLException {
        try {
            return get(email);
        } catch (UserNotFound e) {
            return null;
        }
    }

    public User find(Integer userId) throws SQLException {
        try {
            return get(userId);
        } catch (UserNotFound e) {
            return null;
        }
    }

    public User getOrCreate(User user) throws SQLException {
        User existingUser = find(user.getEmail());
        if (existingUser == null) {
            return insert(user);
        } else {
            user.setId(existingUser.getId());
            update(user);
        }
        return existingUser;
    }

    public User insert(User user) throws SQLException {
        try (Statement statement = SQLiteConnection.getInstance().getConnection().createStatement()) {
            statement.executeUpdate(
                this.userQueries.getInsertQuery(user.getEmail(), user.getAccessToken(), user.getAccessKey(), user.getSecretKey(), user.isActive())
            );
        }
        // return the user object with the id
        return find(user.getEmail());
    }

    public void update(User user) throws SQLException {
        try (Statement statement = SQLiteConnection.getInstance().getConnection().createStatement()) {
            statement.executeUpdate(
                this.userQueries.getUpdateQuery(user.getId(), user.getEmail(), user.getAccessToken(), user.getAccessKey(), user.getSecretKey(), user.isActive())
            );
        }
    }

    /*
    Mark all users except the given id as in-active.
    This is needed because we want to make sure that only one user is active at a time.
    */
    public void markInActive(Integer id) throws SQLException {
        try (Statement statement = SQLiteConnection.getInstance().getConnection().createStatement()) {
            statement.executeUpdate(this.userQueries.getMarkInActiveQuery(id));
        }
    }

    /*
    Mark all users as in-active.
     */
    public void markAllInActive() throws SQLException {
        try (Statement statement = SQLiteConnection.getInstance().getConnection().createStatement()) {
            statement.executeUpdate(this.userQueries.getMarkAllInActiveQuery());
        }
    }

    public User getActive() throws SQLException {
        try (Statement statement = SQLiteConnection.getInstance().getConnection().createStatement()) {
            ResultSet resultSet = statement.executeQuery(this.userQueries.getSelectActiveQuery());
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

    /*
    Get the access token of the active user from the database.
     */
    public String getAccessToken(String email) {
        try {
            User user = email.isEmpty() ? getActive() : find(email);
            if (user != null) {
                return user.getAccessToken();
            }
        } catch (SQLException error) {
            CodeSyncLogger.error(String.format("Error while fetching user: %s", error.getMessage()));
        }
        return null;
    }

    /*
    Get the access token of the given user from the database.
     */
    public String getAccessToken(Integer userId) {
        // TODO: Should we default to default user?
        try {
            User user = find(userId);
            if (user != null) {
                return user.getAccessToken();
            } else {
                getAccessToken();
            }
        } catch (SQLException error) {
            CodeSyncLogger.error(String.format("Error while fetching user: %s", error.getMessage()));
        }
        return null;
    }

    public String getAccessToken() {
        return getAccessToken("");
    }
}
