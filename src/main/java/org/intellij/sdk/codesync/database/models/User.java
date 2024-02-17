package org.intellij.sdk.codesync.database.models;

import org.intellij.sdk.codesync.database.tables.UserTable;

import java.sql.SQLException;

/*
    This class is model for User table, and will contain all accessor and utility methods for managing User.
*/
public class User extends Model {
    private String email, accessToken, accessKey, secretKey;
    private Boolean isActive;
    private Integer id;
    private UserTable table;
    /*
    This constructor is used to create a User object with the given parameters.
    This will be useful when we are creating a new User object from the database.
    */
    public User(Integer id, String email, String accessToken, String accessKey, String secretKey, Boolean isActive) {
        this.email = email;
        this.accessToken = accessToken;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.isActive = isActive;
        this.id = id;
        this.table = UserTable.getInstance();
    }

    /*
    This constructor is used to create a User object with the given parameters.
    This will be useful when we are creating a new User object to insert into the database.
    */
    public User (String email, String accessToken, String accessKey, String secretKey, Boolean isActive) {
        this.id = null;
        this.email = email;
        this.accessToken = accessToken;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.isActive = isActive;

        this.table = UserTable.getInstance();
    }

    public UserTable getTable() {
        return table;
    }

    public Integer getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void save() throws SQLException {
        this.table.insert(this);
    }

    public boolean isActive() {
        return isActive;
    }
}
