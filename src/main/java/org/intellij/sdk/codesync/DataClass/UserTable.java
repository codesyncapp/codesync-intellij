package org.intellij.sdk.codesync.DataClass;

import org.intellij.sdk.codesync.Database;
import org.intellij.sdk.codesync.files.UserFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class UserTable {

    static String email;
    static String access_token;
    static String secret_key;
    static String access_key;
    static int is_active;

    public static void update(UserFile.User user){
        updateValues(user);
        String query = "SELECT * FROM user WHERE email = '" + email + "'";
        ArrayList<HashMap<String, String>> users = getRecords(query);
        if(users != null && users.size() > 0){
            query = "UPDATE user SET " +
                    "access_token = '" + access_token +
                    "', secret_key = '" + secret_key +
                    "', access_key = '" + access_key +
                    "', is_active = " + is_active +
                    " WHERE email = '"+ email +"'";
        }else{
            query = "INSERT INTO user VALUES('"
                    + email + "', '"
                    + access_token + "', '"
                    + secret_key + "', '"
                    + access_key +"', '"
                    + is_active + "')";
        }
        Database.executeUpdate(query);

    }

    static void updateValues(UserFile.User user){

        email = user.getUserEmail() != null? user.getUserEmail() : "";
        access_token = user.getAccessToken() != null? user.getAccessToken() : "";
        secret_key = user.getSecretKey() != null? user.getSecretKey() : "";
        access_key = user.getAccessKey() != null? user.getAccessKey() : "";
        if(user.getActive() != null){
            is_active = user.getActive()? 1 : 0;
        }else{
            is_active = 0;
        }
    }

    public static Map<String, UserFile.User> getUsers(){
        String query = "SELECT * FROM user";
        Map<String, UserFile.User> users = new HashMap<>();
        ArrayList<HashMap<String, Object>> usersArray = getRecords(query);
        String userEmail;
        Map<String, Object> userCredentials;
        for(int i = 0; i < usersArray.size(); i++){
            userCredentials = new HashMap<>();
            userEmail = (String) usersArray.get(i).get("EMAIL");
            userCredentials.put("access_key", usersArray.get(i).getOrDefault("ACCESS_KEY", null));
            userCredentials.put("access_token", usersArray.get(i).getOrDefault("ACCESS_TOKEN", null));
            userCredentials.put("secret_key", usersArray.get(i).getOrDefault("SECRET_KEY", null));
            userCredentials.put("is_active", usersArray.get(i).getOrDefault("IS_ACTIVE", null).equals("1")? true : false);
            UserFile.User user = new UserFile.User(userEmail, userCredentials);
            users.put(userEmail, user);
        }
        return users;
    }
    static ArrayList getRecords(String query){
        return Database.runQuery(query);
    }

}
