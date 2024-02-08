package org.intellij.sdk.codesync.database.queries;

import org.intellij.sdk.codesync.database.tables.RepoTable;
import org.intellij.sdk.codesync.database.tables.UserTable;

public class RepoQueryManager extends QueryManager {
    private static RepoQueryManager instance;

    private RepoQueryManager() {
        super();
    }

    @Override
    public String getCreateTableQuery() {
        private String createTableQuery = String.format("CREATE TABLE IF NOT EXISTS %s (", RepoTable.tableName) +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, path TEXT, user_id INTEGER, state TEXT" +
            String.format("FOREIGN KEY (user_id) REFERENCES %s (id) ", UserTable.tableName) +
            ")";

    }

    public static RepoQueryManager getInstance() {
        if (instance == null) {
            instance = new RepoQueryManager();
        }
        return instance;
    }
}
