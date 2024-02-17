package org.intellij.sdk.codesync.database.models;

import org.intellij.sdk.codesync.database.tables.RepoTable;
import org.intellij.sdk.codesync.enums.RepoState;

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
}
