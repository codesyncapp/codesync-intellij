package org.intellij.sdk.codesync.database.models;

import org.intellij.sdk.codesync.enums.RepoState;

/*
    This class is model for Repo table, and will contain all accessor and utility methods for managing Repo.
*/
public class Repo {
    private String name, path;
    private Integer id, userId;
    private RepoState state;

    public Repo(String name, String path, Integer id, Integer userId, RepoState state) {
        this.name = name;
        this.path = path;
        this.id = id;
        this.userId = userId;
        this.state = state;
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
