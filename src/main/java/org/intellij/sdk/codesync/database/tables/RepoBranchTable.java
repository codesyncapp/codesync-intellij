package org.intellij.sdk.codesync.database.tables;

import org.intellij.sdk.codesync.database.SQLiteConnection;
import org.intellij.sdk.codesync.database.models.RepoBranch;
import org.intellij.sdk.codesync.database.queries.RepoBranchQueries;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class RepoBranchTable extends DBTable {
    private final String tableName = "repo_branch";
    private static RepoBranchTable instance;
    private final RepoBranchQueries repoBranchQueries;

    private RepoBranchTable() {
        this.repoBranchQueries = new RepoBranchQueries(tableName);
    }

    public static RepoBranchTable getInstance() {
        if (instance == null) {
            instance = new RepoBranchTable();
        }
        return instance;
    }

    @Override
    public String getTableName() {
        return tableName;
    }

    @Override
    protected String getCreateTableQuery() {
        return repoBranchQueries.getCreateTableQuery();
    }

    public RepoBranch get(String name, Integer repoId) throws SQLException {
        try (Statement statement = SQLiteConnection.getInstance().getConnection().createStatement()) {
            ResultSet resultSet = statement.executeQuery(this.repoBranchQueries.getSelectQuery(name, repoId));
            if (resultSet.isBeforeFirst()) {
                return new RepoBranch(
                    resultSet.getInt("id"),
                    resultSet.getString("name"),
                    resultSet.getInt("repo_id")
                );
            }
        }
        return null;
    }

    public RepoBranch insert(RepoBranch repoBranch) throws SQLException {
        try (Statement statement = SQLiteConnection.getInstance().getConnection().createStatement()) {
            statement.executeUpdate(this.repoBranchQueries.getInsertQuery(repoBranch.getName(), repoBranch.getRepoId()));
            return get(repoBranch.getName(), repoBranch.getRepoId());
        }
    }

    public void update(RepoBranch repoBranch) throws SQLException {
        try (Statement statement = SQLiteConnection.getInstance().getConnection().createStatement()) {
            statement.executeUpdate(this.repoBranchQueries.getUpdateQuery(repoBranch.getId(), repoBranch.getName(), repoBranch.getRepoId()));
        }
    }
}
