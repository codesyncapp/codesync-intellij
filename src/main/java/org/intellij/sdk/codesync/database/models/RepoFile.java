package org.intellij.sdk.codesync.database.models;

import org.intellij.sdk.codesync.database.tables.RepoFileTable;

import java.sql.SQLException;

/*
    This class is model for RepoFile table, and will contain all accessor and utility methods for managing RepoFile.
*/
public class RepoFile extends Model {
    private String path;
    private Integer repoBranchId, id, serverFileId;

    public RepoFile(Integer id, String path, Integer repoBranchId, Integer serverFileId) {
        this.id = id;
        this.path = path;
        this.repoBranchId = repoBranchId;
        this.serverFileId = serverFileId;
    }

    public RepoFile(String path, Integer repoBranchId, Integer serverFileId) {
        this.id = null;
        this.path = path;
        this.repoBranchId = repoBranchId;
        this.serverFileId = serverFileId;
    }

    public static RepoFileTable getTable() {
        return RepoFileTable.getInstance();
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

    private void getOrCreate() throws SQLException {
        RepoFile repoFile = getTable().getOrCreate(this);
        if (repoFile != null) {
            this.id = repoFile.getId();
        } else {
            throw new SQLException("Error saving RepoFile");
        }
    }

    private void update() throws SQLException {
        getTable().update(this);
    }

    public void save() throws SQLException {
        if (this.id == null) {
            this.getOrCreate();
        } else {
            this.update();
        }
    }

    public void delete() throws SQLException {
        getTable().delete(this);
    }

    public void setId(Integer id) {
        this.id = id;
    }
    public void setRepoBranchId(Integer repoBranchId) {
        this.repoBranchId = repoBranchId;
    }
    public void setServerFileId(Integer serverFileId) {
        this.serverFileId = serverFileId;
    }
}
