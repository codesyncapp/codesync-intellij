package org.intellij.sdk.codesync.database.models;

/*
    This class is model for User table, and will contain all accessor and utility methods for managing User.
*/
public class User {
    String email, accessToken, accessKey, secretKey;
    Boolean isActive;
    Integer id;
    public User(String email, String accessToken, String accessKey, String secretKey, Boolean isActive, Integer id) {
        this.email = email;
        this.accessToken = accessToken;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.isActive = isActive;
        this.id = id;
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
}
