package org.intellij.sdk.codesync.database;

import com.intellij.openapi.application.ApplicationManager;
import org.intellij.sdk.codesync.CodeSyncLogger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.intellij.sdk.codesync.Constants.CONNECTION_STRING;

public class SQLiteConnection {

    private static SQLiteConnection instance;
    private Connection connection;

    private SQLiteConnection(){

        boolean unitTestMode = ApplicationManager.getApplication() == null;

        try{
            Class.forName("org.sqlite.JDBC");
            if(unitTestMode){
                Path testDirPath = Paths.get(System.getProperty("user.dir"), "test_data").toAbsolutePath();
                Path userTestFile = Paths.get(testDirPath.toString(), "test.db").toAbsolutePath();
                connection = DriverManager.getConnection("jdbc:sqlite:" + userTestFile);
            }else{
                connection = DriverManager.getConnection(CONNECTION_STRING);
            }
        } catch (ClassNotFoundException e) {
            CodeSyncLogger.critical("[DATABASE] JDBC library error while initiating SQLite database connection. Error: " + e.getMessage());
        } catch (SQLException e) {
            CodeSyncLogger.error("[DATABASE] SQL error while re-initiating SQLite database connection. Error: " + e.getMessage());
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
            CodeSyncLogger.error("[DATABASE_DISCONNECTION] SQL error while disconnecting SQLite database connection. Error: " + e.getMessage());
        }
    }

}
