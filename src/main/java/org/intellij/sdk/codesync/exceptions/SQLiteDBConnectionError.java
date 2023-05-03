package org.intellij.sdk.codesync.exceptions;

import org.sqlite.SQLiteConnection;

import java.sql.SQLException;

public class SQLiteDBConnectionError extends SQLException {

    public SQLiteDBConnectionError(String message){
        super(message);
    }

}
