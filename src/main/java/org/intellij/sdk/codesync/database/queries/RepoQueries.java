
package org.intellij.sdk.codesync.database.queries;

import org.intellij.sdk.codesync.database.models.Repo;
import org.intellij.sdk.codesync.database.tables.UserTable;

import java.util.ArrayList;

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

    public String getBulkInsertQuery(ArrayList<Repo> repos) {
        // If there is only one element, return the insert query for that element.
        if (repos.size() == 1) {
            Repo repo = repos.get(0);
            return getInsertQuery(
                repo.getServerRepoId(),
                repo.getName(),
                repo.getPath(),
                repo.getUserId(),
                repo.getState().toString()
            );
        }
        StringBuilder query = new StringBuilder(String.format(
            "INSERT INTO %s (server_repo_id, name, path, user_id, state) VALUES ", tableName
        ));

        // Process n-1 elements
        for (int i = 0; i < repos.size() - 1; i++) {
            query.append(String.format(
                " (%s, %s, %s, %s, %s),",
                repos.get(i).getServerRepoId(),
                String.format("'%s'", repos.get(i).getName()),
                String.format("'%s'", repos.get(i).getPath()),
                repos.get(i).getUserId(),
                String.format("'%s'", repos.get(i).getState().toString())
            ));
        }

        int lastElement = repos.size() - 1;

        // Process the last element.
        query.append(String.format(
            " (%s, %s, %s, %s, %s);",
            repos.get(lastElement).getServerRepoId(),
            String.format("'%s'", repos.get(lastElement).getName()),
            String.format("'%s'", repos.get(lastElement).getPath()),
            repos.get(lastElement).getUserId(),
            String.format("'%s'", repos.get(lastElement).getState().toString())
        ));

        return query.toString();
    }

    public String getSelectQuery(String repoPath) {
        return String.format("SELECT * FROM %s WHERE path = '%s';", this.tableName, repoPath);
    }

    public String getSelectAllQuery() {
        return String.format("SELECT * FROM %s;", this.tableName);
    }

    /*
    Get the query to fetch repos with given paths.
     */
    public String getSelectAllQuery(ArrayList<String> repoPaths) {
        return String.format(
            "SELECT * FROM %s WHERE path IN (%s);",
            this.tableName,
            String.join(", ", repoPaths.stream().map(path -> String.format("'%s'", path)).toArray(String[]::new))
        );
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
