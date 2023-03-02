package org.intellij.sdk.codesync;

import com.intellij.vcs.log.Hash;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;


class db {
    private Connection connection = null;

    public void connect() {
        try{
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:C:/Users/Gul Ahmed/.codesync/localstorage.db");
        }catch (Exception exception) {
            System.out.println("Database connection error: " + exception.getMessage());
        } finally {
            // Code that will be executed regardless of whether an exception was thrown or not
        }

    }

    public void disconnect() {
        try {
            connection.close();
        }catch (Exception exception){

        }
    }

    public void createTable(){
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
        } finally {
            // Code that will be executed regardless of whether an exception was thrown or not
        }
    }

    public void executeUpdate(String text){
        try {
            Statement statement = connection.createStatement();
            statement.executeUpdate(text);
        } catch (Exception exception) {
            System.out.println("While inserting: " + exception.getMessage());
        } finally {
            // Code that will be executed regardless of whether an exception was thrown or not
        }
    }

    public ArrayList executeQuery (String text) {

        try {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(text);
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
        } finally {
            // Code that will be executed regardless of whether an exception was thrown or not
        }

        return null;
    }


}