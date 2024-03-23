package org.intellij.sdk.codesync.database.tables;

import org.intellij.sdk.codesync.database.SQLiteConnection;
import org.intellij.sdk.codesync.database.models.RepoBranch;
import org.intellij.sdk.codesync.database.queries.RepoBranchQueries;
import org.intellij.sdk.codesync.exceptions.database.RepoBranchNotFound;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

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

    public RepoBranch get(String name, Integer repoId) throws SQLException, RepoBranchNotFound {
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
        throw new RepoBranchNotFound(String.format("RepoBranch with name '%s' not found.", name));
    }

    public RepoBranch find(String name, Integer repoId) throws SQLException {
        try {
            return get(name, repoId);
        } catch (RepoBranchNotFound e) {
            return null;
        }
    }

    public ArrayList<RepoBranch> findAll(Integer repoId) throws SQLException {
        ArrayList<RepoBranch> repoBranches = new ArrayList<>();
        try (Statement statement = SQLiteConnection.getInstance().getConnection().createStatement()) {
            ResultSet resultSet = statement.executeQuery(this.repoBranchQueries.getSelectQuery(repoId));
            if (resultSet.isBeforeFirst()) {
                while (resultSet.next()) {
                    repoBranches.add(
                        new RepoBranch(
                            resultSet.getInt("id"),
                            resultSet.getString("name"),
                            resultSet.getInt("repo_id")
                        )
                    );
                }
                return repoBranches;
            }
        }
        return repoBranches;
    }

    public Integer getBranchCount(Integer repoId) throws SQLException {
        try (Statement statement = SQLiteConnection.getInstance().getConnection().createStatement()) {
            ResultSet resultSet = statement.executeQuery(this.repoBranchQueries.getBranchCountQuery(repoId));
            if (resultSet.isBeforeFirst()) {
                return resultSet.getInt(1);
            }
        }
        return 0;
    }

    public RepoBranch getOrCreate(RepoBranch repoBranch) throws SQLException {
        RepoBranch existingRepoBranch = find(repoBranch.getName(), repoBranch.getRepoId());
        if (existingRepoBranch == null) {
            return insert(repoBranch);
        } else {
            repoBranch.setId(existingRepoBranch.getId());
            repoBranch.setRepoId(existingRepoBranch.getRepoId());
            update(repoBranch);
        }
        return repoBranch;
    }

    public RepoBranch insert(RepoBranch repoBranch) throws SQLException {
        try (Statement statement = SQLiteConnection.getInstance().getConnection().createStatement()) {
            statement.executeUpdate(this.repoBranchQueries.getInsertQuery(repoBranch.getName(), repoBranch.getRepoId()));
            return find(repoBranch.getName(), repoBranch.getRepoId());
        }
    }

    public void update(RepoBranch repoBranch) throws SQLException {
        try (Statement statement = SQLiteConnection.getInstance().getConnection().createStatement()) {
            statement.executeUpdate(this.repoBranchQueries.getUpdateQuery(repoBranch.getId(), repoBranch.getName(), repoBranch.getRepoId()));
        }
    }
}
