package org.intellij.sdk.codesync.database.queries;

public class RepoQueries extends CommonQueries {
    String tableName;

    public RepoQueries(String tableName) {
        this.tableName = tableName;
    }

    public String getCreateTableQuery() {
        return String.format("CREATE TABLE IF NOT EXISTS %s (", this.tableName) +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, path TEXT, user_id INTEGER, state TEXT" +
            String.format("FOREIGN KEY (user_id) REFERENCES %s (id) ", this.tableName) +
            ")";

    }
}
