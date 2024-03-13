package org.intellij.sdk.codesync.database.models;

import org.intellij.sdk.codesync.database.tables.RepoTable;
import org.intellij.sdk.codesync.enums.RepoState;
import org.intellij.sdk.codesync.exceptions.SQLiteDBConnectionError;
import org.intellij.sdk.codesync.exceptions.SQLiteDataError;

import java.sql.SQLException;

/*
    This class is model for Repo table, and will contain all accessor and utility methods for managing Repo.
*/
public class Repo extends Model {
    private String name, path;
    private Integer id, userId;
    private RepoState state;
    private RepoTable table;

    /*
    This constructor is used to create a Repo object with the given parameters.
    This will be useful when we are creating a new Repo object from the database.
    */
    public Repo(Integer id, String name, String path, Integer userId, RepoState state) {
        this.name = name;
        this.path = path;
        this.id = id;
        this.userId = userId;
        this.state = state;
        this.table = RepoTable.getInstance();
    }

    /*
    This constructor is used to create a Repo object with the given parameters.
    This will be useful when we are creating a new Repo object to insert into the database.
    */
    public Repo(String name, String path, Integer userId, RepoState state) {
        this.id = null;
        this.name = name;
        this.path = path;
        this.userId = userId;
        this.state = state;
        this.table = RepoTable.getInstance();
    }

    /*
    This method is used to create a new Repo object in the database.
     */
    private void getOrCreate() throws SQLException {
        // Get the Repo object from the database if it exists, else create a new Repo object in the database.
        Repo repo = this.table.getOrCreate(this);
        if (repo != null) {
            this.id = repo.getId();
        } else {
            throw new SQLiteDataError("Error saving Repo");
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
    public RepoTable getTable() {
        return table;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public Integer getId() {
        return id;
    }
    public Integer getUserId() {
        return userId;
    }
    public RepoState getState() {
        return state;
    }
    public Boolean isDeleted() {
        return state == RepoState.DELETED;
    }

    public Boolean isDisconnected() {
        return state == RepoState.DISCONNECTED;
    }

    public void setId(Integer id) {
        this.id = id;
    }
}
