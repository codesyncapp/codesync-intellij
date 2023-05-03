package org.intellij.sdk.codesync.database;

import org.intellij.sdk.codesync.CodeSyncLogger;
import org.intellij.sdk.codesync.database.migrations.MigrateUser;
import org.intellij.sdk.codesync.exceptions.SQLiteDBConnectionError;
import org.intellij.sdk.codesync.exceptions.SQLiteDataError;
import org.intellij.sdk.codesync.utils.Queries;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

import static org.intellij.sdk.codesync.Constants.*;

public class Database {

    private static Connection connection = null;

    public static boolean isConnected(){
        return connection != null;
    }

    public static void initiate() {
        try{
            File file = new File(DATABASE_PATH);
            if(!file.exists()){
                Class.forName("org.sqlite.JDBC");
                connection = DriverManager.getConnection(CONNECTION_STRING);
                executeUpdate(Queries.User.CREATE_TABLE);
                MigrateUser migrateUser = new MigrateUser();
                migrateUser.migrateData();
            } else {
                Class.forName("org.sqlite.JDBC");
                connection = DriverManager.getConnection(CONNECTION_STRING);
                executeUpdate(Queries.User.CREATE_TABLE);
            }

        } catch (ClassNotFoundException e) {
            CodeSyncLogger.critical("[DATABASE] JDBC library error while initiating SQLite database connection. Error: " + e.getMessage());
            connection = null;
        } catch (SQLException e) {
            CodeSyncLogger.critical("[DATABASE] SQL error while initiating SQLite database connection. Error: " + e.getMessage());
            connection = null;
        }
    }

    public static void initiate(String connectionString) {
        try{
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(connectionString);
        } catch (ClassNotFoundException e) {
            System.out.println("[DATABASE] JDBC library error while initiating SQLite database connection. Error: " + e.getMessage());
            connection = null;
        } catch (SQLException e) {
            System.out.println("[DATABASE] SQL error while initiating SQLite database connection. Error: " + e.getMessage());
            connection = null;
        }
    }

    /*
    This method accepts a SELECT query and then using
    executeQuery method fetch rows from database.

    Every row is stored as a hashmap, where key is column name and value is column value.

    Then every row stored as a hashmap is added to arraylist which is returned as a list of rows.
     */
    public static ArrayList<HashMap<String, String>> runQuery(String query) throws SQLiteDBConnectionError, SQLiteDataError{

        ArrayList<HashMap<String, String>> dataSet = new ArrayList<>();

        try{
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(query);
            ResultSetMetaData md = rs.getMetaData();

            while (rs.next()){
                HashMap<String, String> row = new HashMap<>();
                for(int i = 1; i <= md.getColumnCount(); i++){
                    row.put(md.getColumnName(i), rs.getString(i));
                }
                dataSet.add(row);
            }
        } catch (NullPointerException e){
            throw new SQLiteDBConnectionError("SQLite Database Connection error: " + e.getMessage());
        } catch (SQLException e){
            throw new SQLiteDataError("Error while read data from SQLite database: " + e.getMessage());
        }

        return dataSet;
    }

    public static void executeUpdate(String query) throws SQLiteDBConnectionError, SQLiteDataError{

        try{
            Statement statement = connection.createStatement();
            statement.executeUpdate(query);
        } catch (NullPointerException e){
            throw new SQLiteDBConnectionError("SQLite Database Connection error: " + e.getMessage());
        } catch (SQLException e){
            throw new SQLiteDataError("Error while inserting data in SQLite database: " + e.getMessage());
        }


    }

    public static void disconnect(){
        try {
            connection.close();
        } catch (NullPointerException e) {
            CodeSyncLogger.critical("[DATABASE] Database connection error disconnecting SQLite database connection. Error: " + e.getMessage());
        } catch (SQLException e) {
            CodeSyncLogger.critical("[DATABASE] SQL error disconnecting SQLite database connection. Error: " + e.getMessage());
        }
    }

}
