package org.intellij.sdk.codesync.database.queries;

import org.intellij.sdk.codesync.database.models.RepoFile;
import org.intellij.sdk.codesync.database.tables.RepoBranchTable;
import org.intellij.sdk.codesync.database.tables.RepoTable;

import java.util.ArrayList;

public class RepoFileQueries extends CommonQueries {
    private final String tableName;

    public RepoFileQueries(String tableName) {
        this.tableName = tableName;
    }

    public String getCreateTableQuery() {
        RepoBranchTable repoBranchTable = RepoBranchTable.getInstance();

        return String.format("CREATE TABLE IF NOT EXISTS %s (", this.tableName) +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, path TEXT, repo_branch_id INTEGER, server_file_id BIGINT, " +
                String.format("FOREIGN KEY(repo_branch_id) REFERENCES %s(id))", repoBranchTable.getTableName());
    }

    public String getInsertQuery(String path, Integer repoBranchId, Integer serverFileId) {
        return String.format(
                "INSERT INTO %s (path, repo_branch_id, server_file_id) VALUES (%s, %s, %s)",
                this.tableName,
                String.format("'%s'", path),
                repoBranchId,
                serverFileId
        );
    }

    public String getBulkInsertQuery(ArrayList<RepoFile> repoFiles) {

        // If there is only one element, return the insert query for that element.
        if (repoFiles.size() == 1) {
            RepoFile repoFile = repoFiles.get(0);
            return getInsertQuery(repoFile.getPath(), repoFile.getRepoBranchId(), repoFile.getServerFileId());
        }
        StringBuilder query = new StringBuilder(String.format(
            "INSERT INTO %s (path, repo_branch_id, server_file_id) VALUES ", tableName
        ));

        // Process n-1 elements
        for (int i = 0; i < repoFiles.size() - 1; i++) {
            query.append(String.format(
                " (%s, %s, %s),",
                String.format("'%s'", repoFiles.get(i).getPath()),
                repoFiles.get(i).getRepoBranchId(),
                repoFiles.get(i).getServerFileId()
            ));
        }

        int lastElement = repoFiles.size() - 1;

        // Process the last element.
        query.append(String.format(
            " (%s, %s, %s);",
            String.format("'%s'", repoFiles.get(lastElement).getPath()),
            repoFiles.get(lastElement).getRepoBranchId(),
            repoFiles.get(lastElement).getServerFileId()
        ));

        return query.toString();
    }

    public String getSelectQuery(String path, Integer repoBranchId) {
        return String.format("SELECT * FROM %s WHERE path = '%s' AND repo_branch_id = %s;", this.tableName, path, repoBranchId);
    }

    /*
    Get query to select all files for a given branch.
    */
    public String getSelectQuery(Integer repoBranchId) {
        return String.format("SELECT * FROM %s WHERE repo_branch_id = %s;", this.tableName, repoBranchId);
    }

    public String getSelectQuery(String repoPath, String repoBranchName, String filePath) {
        String repoBranchTable = RepoBranchTable.getInstance().getTableName();
        String repoTable = RepoTable.getInstance().getTableName();
        return String.format("SELECT * FROM %s as rf ", this.tableName) +
            String.format("INNER JOIN %s rb on rb.id = rf.repo_branch_id ", repoBranchTable) +
            String.format("INNER JOIN %s r on r.id = rb.repo_id ", repoTable) +
            String.format("WHERE r.path = '%s' ", repoPath) +
            String.format("AND rb.name = '%s' ", repoBranchName) +
            String.format("AND rf.path = '%s'", filePath);
    }

    public String getUpdateQuery(Integer id, String path, Integer repoBranchId, Integer serverFileId) {
        return String.format(
                "UPDATE %s SET path = %s, repo_branch_id = %s, server_file_id = %s WHERE id = %s",
                this.tableName,
                String.format("'%s'", path),
                repoBranchId,
                serverFileId,
                id
        );
    }

    public String getDeleteQuery(Integer id) {
        return String.format("DELETE FROM %s WHERE id = %s", this.tableName, id);
    }
}
