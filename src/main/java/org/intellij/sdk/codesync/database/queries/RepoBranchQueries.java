package org.intellij.sdk.codesync.database.queries;

import org.intellij.sdk.codesync.database.models.RepoBranch;
import org.intellij.sdk.codesync.database.tables.RepoTable;

import java.util.ArrayList;

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

    public String getBulkInsertQuery(ArrayList<RepoBranch> repoBranches) {

        // If there is only one element, return the insert query for that element.
        if (repoBranches.size() == 1) {
            RepoBranch repoBranch = repoBranches.get(0);
            return getInsertQuery(repoBranch.getName(), repoBranch.getRepoId());
        }
        StringBuilder query = new StringBuilder(String.format(
            "INSERT INTO %s (name, repo_id) VALUES ", tableName
        ));

        // Process n-1 elements
        for (int i = 0; i < repoBranches.size() - 1; i++) {
            query.append(String.format(
                " (%s, %s),",
                String.format("'%s'", repoBranches.get(i).getName()),
                repoBranches.get(i).getRepoId()
            ));
        }

        int lastElement = repoBranches.size() - 1;

        // Process the last element.
        query.append(String.format(
            " (%s, %s);",
            String.format("'%s'", repoBranches.get(lastElement).getName()),
            repoBranches.get(lastElement).getRepoId()
        ));

        return query.toString();
    }

    public String getSelectQuery(String name, Integer repoId) {
        return String.format("SELECT * FROM %s WHERE name = '%s' AND repo_id = %s;", this.tableName, name, repoId);
    }

    public String getSelectQuery(Integer repoId, ArrayList<String> branchNames) {
        return String.format(
            "SELECT * FROM %s WHERE repo_id = %s AND name IN (%s);",
            this.tableName,
            repoId,
            String.join(", ", branchNames.stream().map(name -> String.format("'%s'", name)).toArray(String[]::new))
        );
    }

    /*
    Get query to select all branches for a given repo.
     */
    public String getSelectQuery(Integer repoId) {
        return String.format("SELECT * FROM %s WHERE repo_id = %s;", this.tableName, repoId);
    }

    /*
    Get query to select all branches for a given repo.
     */
    public String getBranchCountQuery(Integer repoId) {
        return String.format("SELECT COUNT(*) FROM %s WHERE repo_id = %s;", this.tableName, repoId);
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
