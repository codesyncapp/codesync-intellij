package org.intellij.sdk.codesync.database.tables;


import org.intellij.sdk.codesync.database.Database;
import org.intellij.sdk.codesync.database.queries.CommonQueries;

import java.sql.ResultSet;
import java.sql.SQLException;

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
        ResultSet resultSet = Database.getInstance().query(query);
        return resultSet.next();
    }

    public void createTable() throws SQLException {
        Database.getInstance().update(getCreateTableQuery());
    }
}
