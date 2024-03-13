package org.intellij.sdk.codesync.database.models;

import org.intellij.sdk.codesync.database.tables.RepoBranchTable;

import java.sql.SQLException;

/*
    This class is model for RepoBranch table, and will contain all accessor and utility methods for managing RepoBranch.
*/
public class RepoBranch extends Model {
    private String name;
    private Integer repoId, id;

    private RepoBranchTable table;

    public RepoBranch(Integer id, String name, Integer repoId) {
        this.id = id;
        this.name = name;
        this.repoId = repoId;
        this.table = RepoBranchTable.getInstance();
    }

    public RepoBranch(String name, Integer repoId) {
        this.id = null;
        this.name = name;
        this.repoId = repoId;
        this.table = RepoBranchTable.getInstance();
    }

    public String getName() {
        return name;
    }

    public Integer getRepoId() {
        return repoId;
    }

    public Integer getId() {
        return id;
    }

    private void getOrCreate() throws SQLException {
        RepoBranch repoBranch = this.table.getOrCreate(this);
        if (repoBranch != null) {
            this.id = repoBranch.getId();
        } else {
            throw new SQLException("Error saving RepoBranch");
        }
    }

    private void update() throws SQLException {
        this.table.update(this);
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
}
