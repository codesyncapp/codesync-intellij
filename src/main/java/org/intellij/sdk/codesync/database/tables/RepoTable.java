package org.intellij.sdk.codesync.database.tables;

import org.intellij.sdk.codesync.database.Database;
import org.intellij.sdk.codesync.database.queries.RepoQueries;
import org.intellij.sdk.codesync.exceptions.SQLiteDBConnectionError;
import org.intellij.sdk.codesync.exceptions.SQLiteDataError;

/*
    This class is used to interact with the Repo table in the database.
*/
public class RepoTable extends DBTable {
    private final String tableName = "repo";
    private final RepoQueries repoQueries;

    private static RepoTable instance;

    private RepoTable() {
        this.repoQueries = new RepoQueries(tableName);
    }

    public static RepoTable getInstance() {
        if (instance == null) {
            instance = new RepoTable();
        }
        return instance;
    }

    @Override
    public String getTableName() {
        return tableName;
    }

    @Override
    protected String getCreateTableQuery() {
        return this.repoQueries.getCreateTableQuery();
    }

    public void insert(String name, String path, Integer userId, String state) throws SQLiteDataError, SQLiteDBConnectionError {
        String query = this.repoQueries.getInsertQuery(name, path, userId, state);
        Database.executeUpdate(query);
    }
}
