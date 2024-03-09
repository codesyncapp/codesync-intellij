package org.intellij.sdk.codesync.database.queries;

import org.intellij.sdk.codesync.database.tables.UserTable;

public class RepoQueries extends CommonQueries {
    String tableName;

    public RepoQueries(String tableName) {
        this.tableName = tableName;
    }

    public String getCreateTableQuery() {
        UserTable userTable = UserTable.getInstance();

        return String.format("CREATE TABLE IF NOT EXISTS %s (", this.tableName) +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, path TEXT, user_id INTEGER, state TEXT, " +
            String.format("FOREIGN KEY (user_id) REFERENCES %s (id) ", userTable.getTableName()) +
            ")";

    }

    public String getInsertQuery(String name, String path, Integer userId, String state) {
        return String.format(
            "INSERT INTO %s (name, path, user_id, state) VALUES (%s, %s, %s, %s)",
                this.tableName,
                String.format("'%s'", name),
                String.format("'%s'", path),
                userId,
                String.format("'%s'", state)
        );
    }

    public String getSelectQuery(String name) {
        return String.format("SELECT * FROM %s WHERE name = '%s';", this.tableName, name);
    }

    public String getUpdateQuery(Integer id, String name, String path, Integer userId, String state) {
        return String.format(
            "UPDATE %s SET path = %s, user_id = %s, state = %s WHERE name = %s",
            this.tableName,
            String.format("'%s'", path),
            userId,
            String.format("'%s'", state),
            String.format("'%s'", name)
        );
    }
}
