package org.intellij.sdk.codesync.utils;

public class Queries {

    public static class User{

        public static final String TABLE_EXIST = Queries.table_exist("user");

        //TODO Turn this into multiline string.
        public static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS user(EMAIL TEXT PRIMARY KEY, ACCESS_TOKEN TEXT, SECRET_KEY TEXT, ACCESS_KEY TEXT, IS_ACTIVE INT)";

        public static String get_by_email(String email){
            return String.format("SELECT * FROM user WHERE email='%s'", email);
        }

        public static String get_by_active_status(boolean isActive){
            int is_active = isActive? 1 : 0;
            return String.format("SELECT * FROM user WHERE is_active=%s", is_active);
        }

        public static String insert(String email, String access_token, String secret_key, String access_key, boolean is_active){
            email = email != null? String.format("'%s'", email) : null;
            access_token = access_token != null? String.format("'%s'", access_token) : null;
            secret_key = secret_key != null? String.format("'%s'", secret_key) : null;
            access_key = access_key != null? String.format("'%s'", access_key) : null;
            int status = is_active? 1 : 0;
            return String.format("INSERT INTO user VALUES(%s, %s, %s, %s, %s)", email, access_token, secret_key, access_key, status);
        }

        public static String update_by_email(String access_token, String secret_key, String access_key, boolean is_active, String email){
            email = email != null? String.format("'%s'", email) : null;
            access_token = access_token != null? String.format("'%s'", access_token) : null;
            secret_key = secret_key != null? String.format("'%s'", secret_key) : null;
            access_key = access_key != null? String.format("'%s'", access_key) : null;
            int status = is_active? 1 : 0;
            return String.format("UPDATE user SET access_token = %s, secret_key = %s, access_key = %s, is_active = %s WHERE email = %s", access_token, secret_key, access_key, status, email);
        }

        public static String update_all_by_active_status(boolean isActive){
            int is_active = isActive? 1 : 0;
            return String.format("UPDATE user SET is_active = %d", is_active);
        }

    }

    public static String table_exist(String table_name){
        return String.format("SELECT name FROM sqlite_master WHERE type='table' AND name='%s'", table_name);
    }

}
