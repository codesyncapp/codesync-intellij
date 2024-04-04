package org.intellij.sdk.codesync.database;

import org.intellij.sdk.codesync.CodeSyncLogger;
import org.intellij.sdk.codesync.utils.CommonUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.intellij.sdk.codesync.Constants.CONNECTION_STRING;

public class SQLiteConnection {

    private static SQLiteConnection instance;
    private Connection connection;

    private SQLiteConnection(){
        try{
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(CONNECTION_STRING);
        } catch (ClassNotFoundException e) {
            CodeSyncLogger.critical(
                String.format(
                    "[DATABASE] JDBC library error while initiating SQLite database connection. Error: %s",
                    CommonUtils.getStackTrace(e)
                )
            );
        } catch (SQLException e) {
            CodeSyncLogger.error(
                String.format(
                    "[DATABASE] SQL error while re-initiating SQLite database connection. Error: %s",
                    CommonUtils.getStackTrace(e)
                )
            );
        }
    }

    public static synchronized SQLiteConnection getInstance() throws SQLException {

        if(instance == null || instance.connection.isClosed()){
            instance = new SQLiteConnection();
        }

        return instance;
    }

    public Connection getConnection(){
        return connection;
    }

    public void disconnect(){
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            CodeSyncLogger.error(
                String.format(
                    "[DATABASE_DISCONNECTION] SQL error while disconnecting SQLite database connection. Error: %s",
                    CommonUtils.getStackTrace(e)
                )
            );
        }
    }

}
