package org.intellij.sdk.codesync.database.tables;

import org.intellij.sdk.codesync.database.SQLiteConnection;
import org.intellij.sdk.codesync.database.models.RepoFile;
import org.intellij.sdk.codesync.database.queries.RepoFileQueries;
import org.intellij.sdk.codesync.exceptions.database.RepoFileNotFound;

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

    public RepoFile get(String path, Integer repoBranchId) throws SQLException, RepoFileNotFound {
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
        throw new RepoFileNotFound(String.format("RepoFile with path '%s' and branch '%s' not found.", path, repoBranchId));
    }

    public RepoFile find(String path, Integer repoBranchId) throws SQLException {
        try {
            return get(path, repoBranchId);
        } catch (RepoFileNotFound e) {
            return null;
        }
    }

    /*
    Get RepoFile by path, repoBranchName and filePath.

    This will join the related tables to get the RepoFile.
     */
    public RepoFile get(String repoPath, String repoBranchName, String filePath) throws SQLException, RepoFileNotFound {
        // TODO: Perform performance comparison between join and separate queries.
        try (Statement statement = SQLiteConnection.getInstance().getConnection().createStatement()) {
            ResultSet resultSet = statement.executeQuery(this.repoFileQueries.getSelectQuery(repoPath, repoBranchName, filePath));
            if (resultSet.isBeforeFirst()) {
                return new RepoFile(
                    resultSet.getInt("id"),
                    resultSet.getString("path"),
                    resultSet.getInt("repo_branch_id"),
                    resultSet.getInt("server_file_id")
                );
            }
        }
        throw new RepoFileNotFound(String.format("RepoFile with repo path '%s', branch '%s' and path '%s' not found.", repoPath, repoBranchName, filePath));
    }

    public ArrayList<RepoFile> findAll(Integer repoBranchId) throws SQLException {
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
        RepoFile existingRepoFile = find(repoFile.getPath(), repoFile.getRepoBranchId());
        if (existingRepoFile == null) {
            return insert(repoFile);
        } else {
            repoFile.setId(existingRepoFile.getId());
            update(repoFile);
        }
        return repoFile;
    }

    public RepoFile insert(RepoFile repoFile) throws SQLException {
        try (Statement statement = SQLiteConnection.getInstance().getConnection().createStatement()) {
            statement.executeUpdate(this.repoFileQueries.getInsertQuery(repoFile.getPath(), repoFile.getRepoBranchId(), repoFile.getServerFileId()));
            return find(repoFile.getPath(), repoFile.getRepoBranchId());
        }
    }

    /*
    Bulk insert repo files into the database.
    */
    public void bulkInsert(ArrayList<RepoFile> repoFiles) throws SQLException {
        try (Statement statement = SQLiteConnection.getInstance().getConnection().createStatement()) {
            statement.executeUpdate(this.repoFileQueries.getBulkInsertQuery(repoFiles));
        }
    }

    public void update(RepoFile repoFile) throws SQLException {
        try (Statement statement = SQLiteConnection.getInstance().getConnection().createStatement()) {
            statement.executeUpdate(this.repoFileQueries.getUpdateQuery(repoFile.getId(), repoFile.getPath(), repoFile.getRepoBranchId(), repoFile.getServerFileId()));
        }
    }

    public void delete(RepoFile repoFile) throws SQLException {
        try (Statement statement = SQLiteConnection.getInstance().getConnection().createStatement()) {
            statement.executeUpdate(this.repoFileQueries.getDeleteQuery(repoFile.getId()));
        }
    }
}
