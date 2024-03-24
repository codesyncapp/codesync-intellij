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
    }

    public static UserTable getTable() {
        return UserTable.getInstance();
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
    public boolean isActive() {
        return isActive;
    }

    public void setEmail(String email) {
        this.email = email;
    }
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }
    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public boolean hasValidEmail() {
        return this.email != null && !this.email.isEmpty();
    }
    private void getOrCreate() throws SQLException {
        User user = getTable().getOrCreate(this);
        if (user != null) {
            this.id = user.getId();
        } else {
            throw new SQLException("Error saving User");
        }
    }

    private void update() throws SQLException {
        getTable().update(this);
    }

    public void save() throws SQLException {
        if (this.id == null) {
            this.getOrCreate();
        } else {
            this.update();
        }
    }

    /*
    Make this user active, there can be only one active user at a time.
    So, this means all other users will be made inactive.
     */
    public void makeActive() throws SQLException {
        // If user is not saved or is not active then mark it active and save.
        if(this.id == null || !this.isActive) {
            this.isActive = true;
            this.save();
        }

        // Now mark all other users as inactive.
        getTable().markOthersInActive(this.id);
    }

    public void setId(Integer id) {
        this.id = id;
    }
}
