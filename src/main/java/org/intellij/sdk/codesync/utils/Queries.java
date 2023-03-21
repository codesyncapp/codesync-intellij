package org.intellij.sdk.codesync.utils;

import javax.annotation.Nullable;

public class Queries {

    public static final String CREATE_USER_TABLE = "CREATE TABLE IF NOT EXISTS user (EMAIL TEXT PRIMARY KEY, ACCESS_TOKEN TEXT, SECRET_KEY TEXT, ACCESS_KEY TEXT, IS_ACTIVE INT)";
    public static class User{

        public static final String TABLE_EXIST = Queries.table_exist("user");
        public static String get_all(){
            return "SELECT * FROM user";
        }

        public static String get_by_email(String email){
            return String.format("SELECT * FROM user WHERE email='%s'", email);
        }

        public static String get_by_active_status(boolean isActive){
            int is_active = isActive? 1 : 0;
            return String.format("SELECT * FROM user WHERE is_active=%s", is_active);
        }

        public static String insert(String email, String access_token, String secret_key, String access_key, boolean status){
            int is_active;
            email = email == null? "NULL" : String.format("'%s'", email);
            access_token = access_token == null? "NULL" : String.format("'%s'", access_token);
            secret_key = secret_key == null? "NULL" : String.format("'%s'", secret_key);
            access_key = access_key == null? "NULL" : String.format("'%s'", access_key);

            if(status){
                is_active = status? 1 : 0;
            }else{
                is_active = 0;
            }

            return String.format("INSERT INTO user VALUES(%s, %s, %s, %s, %d)", email, access_token, secret_key, access_key, is_active);

        }

    }

    public static String table_exist(String table_name){
        return String.format("SELECT name FROM sqlite_master WHERE type='table' AND name='%s'", table_name);
    }

}
