
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
            "id INTEGER PRIMARY KEY AUTOINCREMENT, server_repo_id BIGINT, name TEXT, path TEXT, user_id INTEGER, state TEXT, " +
            String.format("FOREIGN KEY (user_id) REFERENCES %s (id) ", userTable.getTableName()) +
            ")";

    }

    public String getInsertQuery(Integer serverRepoId, String name, String path, Integer userId, String state) {
        return String.format(
            "INSERT INTO %s (server_repo_id, name, path, user_id, state) VALUES (%s, %s, %s, %s, %s)",
                this.tableName,
                serverRepoId,
                String.format("'%s'", name),
                String.format("'%s'", path),
                userId,
                String.format("'%s'", state)
        );
    }

    public String getSelectQuery(String repoPath) {
        return String.format("SELECT * FROM %s WHERE path = '%s';", this.tableName, repoPath);
    }

    public String getUpdateQuery(String path, Integer userId, String state) {
        return String.format(
            "UPDATE %s SET user_id = %s, state = %s WHERE path = %s",
            this.tableName,
            userId,
            String.format("'%s'", state),
            String.format("'%s'", path)
        );
    }
}
