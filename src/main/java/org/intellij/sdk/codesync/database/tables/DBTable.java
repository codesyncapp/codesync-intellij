package org.intellij.sdk.codesync.database.tables;


import org.intellij.sdk.codesync.database.SQLiteConnection;
import org.intellij.sdk.codesync.database.queries.CommonQueries;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/*
    This is the base class for all tables in the database.
*/
public abstract class DBTable {
    public DBTable() {}

    public abstract String getTableName();
    protected abstract String getCreateTableQuery();

    /*
        This method checks if the table exists in the database.
    */
    public Boolean exists() throws SQLException {
        String query = new CommonQueries().getTableExistsQuery(getTableName());
        try (Statement statement = SQLiteConnection.getInstance().getConnection().createStatement()) {
            ResultSet resultSet  = statement.executeQuery(query);
            return resultSet.isBeforeFirst();
        }
    }

    public void createTable() throws SQLException {
        try (Statement statement = SQLiteConnection.getInstance().getConnection().createStatement()) {
            statement.executeUpdate(getCreateTableQuery());
        }
    }
}
