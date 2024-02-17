package org.intellij.sdk.codesync.database.tables;

import org.intellij.sdk.codesync.database.queries.RepoQueries;

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


}
