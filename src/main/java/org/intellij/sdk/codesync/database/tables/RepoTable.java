package org.intellij.sdk.codesync.database.tables;

/*
    This class is used to interact with the Repo table in the database.
*/
public class RepoTable extends DBTable {
    private String tableName = "repo";
    private String createTableQuery = String.format("CREATE TABLE IF NOT EXISTS %s (", tableName) +
        "id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, path TEXT, user_id INTEGER, state TEXT" +
        String.format("FOREIGN KEY (user_id) REFERENCES %s (id) ", UserTable.tableName) +
        ")";

    @Override
    public String getTableName() {
        return tableName;
    }

    @Override
    protected String getCreateTableQuery() {
        return createTableQuery;
    }

    public RepoTable() {
    }

}
