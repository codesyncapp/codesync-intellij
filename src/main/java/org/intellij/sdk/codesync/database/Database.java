package org.intellij.sdk.codesync.database;

import org.intellij.sdk.codesync.exceptions.SQLiteDBConnectionError;
import org.intellij.sdk.codesync.exceptions.SQLiteDataError;
import org.intellij.sdk.codesync.utils.CommonUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

public class Database {

    private static Database instance;

    private Database() {}

    public static Database getInstance() {
        if (instance == null) {
            instance = new Database();
        }
        return instance;
    }

    /*
    This method accepts a SELECT query and then using
    executeQuery method fetch rows from database.

    Every row is stored as a hashmap, where key is column name and value is column value.

    Then every row stored as a hashmap is added to arraylist which is returned as a list of rows.
     */
    public static ArrayList<HashMap<String, String>> runQuery(String query) throws SQLiteDBConnectionError, SQLiteDataError{

        ArrayList<HashMap<String, String>> dataSet = new ArrayList<>();

        try(Statement statement = SQLiteConnection.getInstance().getConnection().createStatement()){
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
            throw new SQLiteDBConnectionError(
                String.format("SQLite Database Connection error: %s%n", CommonUtils.getStackTrace(e))
            );
        } catch (SQLException e){
            throw new SQLiteDataError(
                String.format("Error while read data from SQLite database: %s%n",  CommonUtils.getStackTrace(e))
            );
        }

        return dataSet;
    }

    public static void executeUpdate(String query) throws SQLiteDBConnectionError, SQLiteDataError{

        try(Statement statement = SQLiteConnection.getInstance().getConnection().createStatement()){
            statement.executeUpdate(query);
        } catch (NullPointerException e){
            throw new SQLiteDBConnectionError(
                String.format("SQLite Database Connection error: %s%n", CommonUtils.getStackTrace(e))
            );
        } catch (SQLException e){
            throw new SQLiteDataError(
                String.format("Error while inserting data in SQLite database: %s%n", CommonUtils.getStackTrace(e))
            );
        }
    }
}
