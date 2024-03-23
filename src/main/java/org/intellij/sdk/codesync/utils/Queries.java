package org.intellij.sdk.codesync.utils;

public class Queries {

    public static class User{

        //TODO Turn this into multiline string.
        public static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS user(EMAIL TEXT PRIMARY KEY, ACCESS_TOKEN TEXT, SECRET_KEY TEXT, ACCESS_KEY TEXT, IS_ACTIVE INT)";

        public static String get_by_email(String email){
            return String.format("SELECT * FROM user WHERE email='%s'", email);
        }
        public static String insert(String email, String access_token, String secret_key, String access_key, boolean is_active){
            email = email != null? String.format("'%s'", email) : null;
            access_token = access_token != null? String.format("'%s'", access_token) : null;
            secret_key = secret_key != null? String.format("'%s'", secret_key) : null;
            access_key = access_key != null? String.format("'%s'", access_key) : null;
            int status = is_active? 1 : 0;
            return String.format("INSERT INTO user VALUES(%s, %s, %s, %s, %s)", email, access_token, secret_key, access_key, status);
        }
    }

}
