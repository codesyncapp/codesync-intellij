package org.intellij.sdk.codesync.database.queries;

public class MigrationsQueries extends CommonQueries {
    String tableName;

    public MigrationsQueries(String tableName) {
        this.tableName = tableName;
    }

    public String getCreateTableQuery() {
        return String.format("CREATE TABLE IF NOT EXISTS %s (", this.tableName) +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, table_name VARCHAR(64), state VARCHAR(32)" +
            ")";
    }

    public String getFetchMigrationQuery(String tableName) {
        // Get the most recently applied migration for a table.
        return String.format("SELECT * FROM %s  WHERE table_name=%s ORDER BY id DESC LIMIT 1;", this.tableName, tableName);
    }
}
