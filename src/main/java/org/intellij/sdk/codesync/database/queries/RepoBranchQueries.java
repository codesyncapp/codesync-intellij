package org.intellij.sdk.codesync.database.queries;

import org.intellij.sdk.codesync.database.tables.RepoTable;

public class RepoBranchQueries extends CommonQueries {
    String tableName;

    public RepoBranchQueries(String tableName) {
        this.tableName = tableName;
    }

    public String getCreateTableQuery() {
        RepoTable repoTable = RepoTable.getInstance();

        return String.format("CREATE TABLE IF NOT EXISTS %s (", this.tableName) +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, repo_id INTEGER, " +
                String.format("FOREIGN KEY(repo_id) REFERENCES %s(id))", repoTable.getTableName());
    }

    public String getInsertQuery(String name, Integer repoId) {
        return String.format(
                "INSERT INTO %s (name, repo_id) VALUES (%s, %s)",
                this.tableName,
                String.format("'%s'", name),
                repoId
        );
    }

    public String getSelectQuery(String name, Integer repoId) {
        return String.format("SELECT * FROM %s WHERE name = '%s' AND repo_id = %s;", this.tableName, name, repoId);
    }

    /*
    Get query to select all branches for a given repo.
     */
    public String getSelectQuery(Integer repoId) {
        return String.format("SELECT * FROM %s WHERE repo_id = %s;", this.tableName, repoId);
    }

    public String getUpdateQuery(Integer id, String name, Integer repoId) {
        return String.format(
                "UPDATE %s SET name = %s, repo_id = %s WHERE id = %s",
                this.tableName,
                String.format("'%s'", name),
                repoId,
                id
        );
    }
}
