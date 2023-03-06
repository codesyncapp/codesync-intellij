package org.intellij.sdk.codesync;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

import static org.intellij.sdk.codesync.Constants.CODESYNC_ROOT;

public class Database {

    private static Connection connection = null;

    public static void initiate() {
        try{
            Class.forName("org.sqlite.JDBC");
            String connectionString = "jdbc:sqlite:" + CODESYNC_ROOT + "\\localstorage.db";
            connection = DriverManager.getConnection(connectionString);
            createTable();
        }catch (Exception exception) {
            System.out.println("Database connection error: " + exception.getMessage());
        }
    }

    public static void createTable(){
        try {
            Statement statement = connection.createStatement();
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS user " +
                            "(EMAIL TEXT PRIMARY KEY, " +
                            "ACCESS_TOKEN TEXT, " +
                            "SECRET_KEY TEXT, " +
                            "ACCESS_KEY TEXT, " +
                            "IS_ACTIVE INT)");
        } catch (Exception exception) {
            System.out.println("Table creation error!");
        }
    }

    public static ArrayList runQuery(String query){
        try {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(query);
            ResultSetMetaData md = rs.getMetaData();

            ArrayList<HashMap<String, String>> dataSet = new ArrayList<>();

            while (rs.next()){
                HashMap<String, String> row = new HashMap<>();
                for(int i = 1; i <= md.getColumnCount(); i++){
                    row.put(md.getColumnName(i), rs.getString(i));
                }
                dataSet.add(row);
            }

            return dataSet;

        } catch (Exception exception) {
            System.out.println("Error while getting records from database.");
        }

        return null;
    }

    public static void executeUpdate(String query){
        try {
            Statement statement = connection.createStatement();
            statement.executeUpdate(query);
        } catch (Exception exception) {
            System.out.println("While inserting: " + exception.getMessage());
        }
    }

    public static void queryTest(){
//        try {
//            Statement statement = connection.createStatement();
//            ResultSet rs = statement.executeQuery("SELECT * FROM user WHERE email = 'gulahmed@codesync.com'");
//
//            while (rs.next()){
//                System.out.println("Testing!!!! : " + rs.getString(3));
//            }
//        } catch (Exception exception) {
//            System.out.println("Error while getting records from database.");
//        }
    }

    public static void disconnect(){
        try {
            connection.close();
        }catch (Exception exception){
            System.out.println("Error while disconnecting database: " + exception.getMessage());
        }
    }

}
