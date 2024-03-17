package org.intellij.sdk.codesync.database.tables;

import org.intellij.sdk.codesync.database.SQLiteConnection;
import org.intellij.sdk.codesync.database.models.RepoFile;
import org.intellij.sdk.codesync.database.queries.RepoFileQueries;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class RepoFileTable extends DBTable {
    private final String tableName = "repo_file";
    private static RepoFileTable instance;
    private final RepoFileQueries repoFileQueries;

    private RepoFileTable() {
        this.repoFileQueries = new RepoFileQueries(tableName);
    }

    public static RepoFileTable getInstance() {
        if (instance == null) {
            instance = new RepoFileTable();
        }
        return instance;
    }

    @Override
    public String getTableName() {
        return tableName;
    }

    @Override
    protected String getCreateTableQuery() {
        return repoFileQueries.getCreateTableQuery();
    }

    public RepoFile get(String path, Integer repoBranchId) throws SQLException {
        try (Statement statement = SQLiteConnection.getInstance().getConnection().createStatement()) {
            ResultSet resultSet = statement.executeQuery(this.repoFileQueries.getSelectQuery(path, repoBranchId));
            if (resultSet.isBeforeFirst()) {
                return new RepoFile(
                    resultSet.getInt("id"),
                    resultSet.getString("path"),
                    resultSet.getInt("repo_branch_id"),
                    resultSet.getInt("server_file_id")
                );
            }
        }
        return null;
    }

    public ArrayList<RepoFile> get(Integer repoBranchId) throws SQLException {
        ArrayList<RepoFile> repoFiles = new ArrayList<>();
        try (Statement statement = SQLiteConnection.getInstance().getConnection().createStatement()) {
            ResultSet resultSet = statement.executeQuery(this.repoFileQueries.getSelectQuery(repoBranchId));
            if (resultSet.isBeforeFirst()) {
                while (resultSet.next()) {
                    repoFiles.add(
                        new RepoFile(
                            resultSet.getInt("id"),
                            resultSet.getString("path"),
                            resultSet.getInt("repo_branch_id"),
                            resultSet.getInt("server_file_id")
                        )
                    );
                }
            }
        }
        return repoFiles;
    }

    public RepoFile getOrCreate(RepoFile repoFile) throws SQLException {
        RepoFile existingRepoFile = get(repoFile.getPath(), repoFile.getRepoBranchId());
        if (existingRepoFile == null) {
            return insert(repoFile);
        } else {
            repoFile.setId(existingRepoFile.getId());
            repoFile.setServerFileId(existingRepoFile.getServerFileId());
            update(repoFile);
        }
        return repoFile;
    }

    public RepoFile insert(RepoFile repoFile) throws SQLException {
        try (Statement statement = SQLiteConnection.getInstance().getConnection().createStatement()) {
            statement.executeUpdate(this.repoFileQueries.getInsertQuery(repoFile.getPath(), repoFile.getRepoBranchId(), repoFile.getServerFileId()));
            return get(repoFile.getPath(), repoFile.getRepoBranchId());
        }
    }

    public void update(RepoFile repoFile) throws SQLException {
        try (Statement statement = SQLiteConnection.getInstance().getConnection().createStatement()) {
            statement.executeUpdate(this.repoFileQueries.getUpdateQuery(repoFile.getId(), repoFile.getPath(), repoFile.getRepoBranchId(), repoFile.getServerFileId()));
        }
    }
}
