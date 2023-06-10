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

public class Database {

    public static void setupDbFilesAndTables(String databasePath){

        /*
            This method will make sure to create db file and required tables,
            make migration from files to db if necessary.
        */

        File file = new File(databasePath);
        if(file.exists()){
           return;
        }

        try {
            executeUpdate(Queries.User.CREATE_TABLE);
            MigrateUser migrateUser = new MigrateUser();
            migrateUser.migrateData();
        } catch (SQLiteDBConnectionError e) {
            CodeSyncLogger.error("[DATABASE] SQLite db connection error while making migration/creating db file first time " + e.getMessage());
        } catch (SQLiteDataError e) {
            CodeSyncLogger.error("[DATABASE] SQLite db data error while making migration/creating db file first time " + e.getMessage());
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
            Statement statement = SQLiteConnection.getInstance().getConnection().createStatement();
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
            Statement statement = SQLiteConnection.getInstance().getConnection().createStatement();
            statement.executeUpdate(query);
        } catch (NullPointerException e){
            throw new SQLiteDBConnectionError("SQLite Database Connection error: " + e.getMessage());
        } catch (SQLException e){
            throw new SQLiteDataError("Error while inserting data in SQLite database: " + e.getMessage());
        }


    }

}
