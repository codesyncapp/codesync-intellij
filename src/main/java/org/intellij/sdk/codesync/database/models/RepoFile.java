package org.intellij.sdk.codesync.database.models;

import org.intellij.sdk.codesync.database.tables.RepoFileTable;

import java.sql.SQLException;

/*
    This class is model for RepoFile table, and will contain all accessor and utility methods for managing RepoFile.
*/
public class RepoFile extends Model {
    private String path;
    private Integer repoBranchId, id, serverFileId;

    private RepoFileTable table;

    public RepoFile(Integer id, String path, Integer repoBranchId, Integer serverFileId) {
        this.id = id;
        this.path = path;
        this.repoBranchId = repoBranchId;
        this.serverFileId = serverFileId;

        this.table = RepoFileTable.getInstance();
    }

    public RepoFile(String path, Integer repoBranchId, Integer serverFileId) {
        this.id = null;
        this.path = path;
        this.repoBranchId = repoBranchId;
        this.serverFileId = serverFileId;

        this.table = RepoFileTable.getInstance();
    }

    public String getPath() {
        return path;
    }
    public Integer getRepoBranchId() {
        return repoBranchId;
    }
    public Integer getId() {
        return id;
    }
    public Integer getServerFileId() {
        return serverFileId;
    }

    private void create() throws SQLException {
        RepoFile repoFile = this.table.insert(this);
        if (repoFile != null) {
            this.id = repoFile.getId();
        } else {
            throw new SQLException("Error saving RepoFile");
        }
    }

    private void update() throws SQLException {
        this.table.update(this);
    }

    public void save() throws SQLException {
        if (this.id == null) {
            this.create();
        } else {
            this.update();
        }
    }
}
