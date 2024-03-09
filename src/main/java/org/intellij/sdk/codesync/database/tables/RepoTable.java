package org.intellij.sdk.codesync.database.tables;

import org.intellij.sdk.codesync.database.SQLiteConnection;
import org.intellij.sdk.codesync.database.models.Repo;
import org.intellij.sdk.codesync.database.queries.RepoQueries;
import org.intellij.sdk.codesync.enums.RepoState;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/*
    This class is used to interact with the Repo table in the database.
*/
public class RepoTable extends DBTable {
    private final String tableName = "repo";
    private final RepoQueries repoQueries;

    private static RepoTable instance;

    private RepoTable() {
        this.repoQueries = new RepoQueries(tableName);
    }

    public static RepoTable getInstance() {
        if (instance == null) {
            instance = new RepoTable();
        }
        return instance;
    }

    @Override
    public String getTableName() {
        return tableName;
    }

    @Override
    protected String getCreateTableQuery() {
        return this.repoQueries.getCreateTableQuery();
    }

    public Repo get(String name) throws SQLException {
        try (Statement statement = SQLiteConnection.getInstance().getConnection().createStatement()) {
            ResultSet resultSet = statement.executeQuery(this.repoQueries.getSelectQuery(name));

            if (resultSet.isBeforeFirst()) {
                return new Repo(
                    resultSet.getInt("id"),
                    resultSet.getString("name"),
                    resultSet.getString("path"),
                    resultSet.getInt("user_id"),
                    RepoState.valueOf(resultSet.getString("state"))
                );
            }
        }
        return null;
    }

    public Repo insert(Repo repo) throws SQLException {
        try (Statement statement = SQLiteConnection.getInstance().getConnection().createStatement()) {
            statement.executeUpdate(
                this.repoQueries.getInsertQuery(repo.getName(), repo.getPath(), repo.getUserId(), repo.getState().toString())
            );
        }
        // return the user object with the id
        return get(repo.getName());
    }

    public void update(Repo repo) throws SQLException {
        try (Statement statement = SQLiteConnection.getInstance().getConnection().createStatement()) {
            statement.executeUpdate(
                this.repoQueries.getUpdateQuery(repo.getId(), repo.getName(), repo.getPath(), repo.getUserId(), repo.getState().toString())
            );
        }
    }
}
