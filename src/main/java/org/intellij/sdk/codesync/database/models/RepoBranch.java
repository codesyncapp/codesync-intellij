package org.intellij.sdk.codesync.database.models;

import org.intellij.sdk.codesync.database.tables.RepoBranchTable;
import org.intellij.sdk.codesync.database.tables.RepoFileTable;
import org.intellij.sdk.codesync.exceptions.database.RepoFileNotFound;

import java.sql.SQLException;
import java.util.ArrayList;

/*
    This class is model for RepoBranch table, and will contain all accessor and utility methods for managing RepoBranch.
*/
public class RepoBranch extends Model {
    private String name;
    private Integer repoId, id;


    public RepoBranch(Integer id, String name, Integer repoId) {
        this.id = id;
        this.name = name;
        this.repoId = repoId;
    }

    public RepoBranch(String name, Integer repoId) {
        this.id = null;
        this.name = name;
        this.repoId = repoId;
    }

    public static RepoBranchTable getTable() {
        return RepoBranchTable.getInstance();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getRepoId() {
        return repoId;
    }

    public Integer getId() {
        return id;
    }

    private void getOrCreate() throws SQLException {
        RepoBranch repoBranch = getTable().getOrCreate(this);
        if (repoBranch != null) {
            this.id = repoBranch.getId();
        } else {
            throw new SQLException("Error saving RepoBranch");
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

    public void setId(Integer id) {
        this.id = id;
    }

    public void setRepoId(Integer repoId) {
        this.repoId = repoId;
    }

    public ArrayList<RepoFile> getFiles() throws SQLException {
        return RepoFileTable.getInstance().findAll(this.id);
    }

    public RepoFile getFile(String relativeFilePath) throws SQLException, RepoFileNotFound {
        return RepoFileTable.getInstance().get(relativeFilePath, this.id);
    }

    /*
    Check if this branch has valid files or not. Files with `null` values are considered invalid. This check is useful
    to query if all files are invalid or not.
    */
    public boolean hasValidFiles() throws SQLException {
        ArrayList<RepoFile> repoFiles = getFiles();

        // User can delete files in a branch, so if no files are present then return `true`.
        if (repoFiles.isEmpty()) {
            return true;
        }
        for (RepoFile repoFile : repoFiles) {
            // return `true` if even a single file is valid (i.e. is not `null`).
            if (repoFile.getServerFileId() != null) {
                return true;
            }
        }
        return false;
    }

    public void updateFileId(String filePath, Integer serverFileId) throws SQLException {
        RepoFile repoFile = new RepoFile(filePath, this.getId(), serverFileId);
        repoFile.save();
    }

    public void removeFile(String fileRelativePath) throws SQLException {
        try {
            getFile(fileRelativePath).delete();
        } catch (RepoFileNotFound e) {
            // Do nothing here.
        }
    }
}
