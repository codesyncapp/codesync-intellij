package org.intellij.sdk.codesync.database;

import org.intellij.sdk.codesync.database.migrations.MigrateUser;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

import static org.intellij.sdk.codesync.Constants.*;

public class Database {

    private static Connection connection = null;

    public static void initiate() {

        try{
            File file = new File(DATABASE_PATH);
            if(!file.exists()){
                Class.forName("org.sqlite.JDBC");
                String connectionString = CONNECTION_STRING;
                connection = DriverManager.getConnection(connectionString);
                executeUpdate(CREATE_USER_TABLE_QUERY);
                MigrateUser migrateUser = new MigrateUser();
                migrateUser.migrateData();
            }else {
                Class.forName("org.sqlite.JDBC");
                String connectionString = CONNECTION_STRING;
                connection = DriverManager.getConnection(connectionString);
                executeUpdate(CREATE_USER_TABLE_QUERY);
            }

        }catch (Exception exception) {
            System.out.println("Database connection error: " + exception.getMessage());
        }
    }

    /*
    This method accepts a SELECT query and then using
    executeQuery method fetch rows from database.

    Every row is stored as a hashmap, where key is column name and value is column value.

    Then every row stored as a hashmap is added to arraylist which is returned as a list of rows.
     */
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
            System.out.println("Database error: " + exception.getMessage());
        }

        return null;
    }

    public static void executeUpdate(String query){
        try {
            Statement statement = connection.createStatement();
            statement.executeUpdate(query);
        } catch (Exception exception) {
            System.out.println("Database error: " + exception.getMessage());
        }
    }

    public static void disconnect(){
        try {
            connection.close();
        }catch (Exception exception){
            System.out.println("Error while disconnecting database: " + exception.getMessage());
        }
    }

}
