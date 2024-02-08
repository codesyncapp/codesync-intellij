package org.intellij.sdk.codesync.database.models;

/*
    This class is model for RepoBranch table, and will contain all accessor and utility methods for managing RepoBranch.
*/
public class RepoBranch {
    private String name;
    private Integer repoId, id;

    public RepoBranch(String name, Integer repoId, Integer id) {
        this.name = name;
        this.repoId = repoId;
        this.id = id;
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
}
