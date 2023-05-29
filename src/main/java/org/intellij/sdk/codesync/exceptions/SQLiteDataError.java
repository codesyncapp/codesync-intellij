package org.intellij.sdk.codesync.exceptions;

import java.sql.SQLException;

public class SQLiteDataError extends SQLException {
    public SQLiteDataError(String message){
        super(message);
    }
}
