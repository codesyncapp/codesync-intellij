package org.intellij.sdk.codesync.database.models;

import org.intellij.sdk.codesync.database.tables.RepoBranchTable;
import org.intellij.sdk.codesync.database.tables.RepoTable;
import org.intellij.sdk.codesync.database.tables.UserTable;
import org.intellij.sdk.codesync.enums.RepoState;
import org.intellij.sdk.codesync.exceptions.SQLiteDataError;
import org.intellij.sdk.codesync.exceptions.database.RepoBranchNotFound;
import org.intellij.sdk.codesync.exceptions.database.UserNotFound;

import java.sql.SQLException;
import java.util.ArrayList;

/*
    This class is model for Repo table, and will contain all accessor and utility methods for managing Repo.
*/
public class Repo extends Model {
    private String name, path;
    private Integer id, userId, serverRepoId;
    private RepoState state;

    /*
    This constructor is used to create a Repo object with the given parameters.
    This will be useful when we are creating a new Repo object from the database.
    */
    public Repo(Integer id, Integer serverRepoId, String name, String path, Integer userId, RepoState state) {
        this.name = name;
        this.path = path;
        this.id = id;
        this.serverRepoId = serverRepoId;
        this.userId = userId;
        this.state = state;
    }

    /*
    This constructor is used to create a Repo object with the given parameters.
    This will be useful when we are creating a new Repo object to insert into the database.
    */
    public Repo(Integer serverRepoId, String name, String path, Integer userId, RepoState state) {
        this.id = null;
        this.serverRepoId = serverRepoId;
        this.name = name;
        this.path = path;
        this.userId = userId;
        this.state = state;
    }

    /*
    This method is used to create a new Repo object in the database.
     */
    private void getOrCreate() throws SQLException {
        // Get the Repo object from the database if it exists, else create a new Repo object in the database.
        Repo repo = getTable().getOrCreate(this);
        if (repo != null) {
            this.id = repo.getId();
        } else {
            throw new SQLiteDataError("Error saving Repo");
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
    public static RepoTable getTable() {
        return RepoTable.getInstance();
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
    public Integer getServerRepoId() {
        return serverRepoId;
    }
    public Integer getUserId() {
        return userId;
    }
    public RepoState getState() {
        return state;
    }
    public void setState(RepoState state) {
        this.state = state;
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

    public ArrayList<RepoBranch> getBranches() throws SQLException {
        return RepoBranchTable.getInstance().findAll(this.id);
    }

    /*
    Utility method to get the user object from the database.
     */
    public User getUser() throws SQLException, UserNotFound {
        return UserTable.getInstance().get(this.userId);
    }

    /*
    Utility method to get the branch object from the database.
     */
    public RepoBranch getBranch(String branchName) throws SQLException, RepoBranchNotFound {
        return RepoBranchTable.getInstance().get(branchName, this.id);
    }

    public boolean hasSyncedBranches() throws SQLException {
        return RepoBranchTable.getInstance().getBranchCount(this.id) > 0;
    }

    /*
    Utility method to check if the repo is active.
    */
    public boolean isActive() {
        return this.state == RepoState.SYNCED && this.userId != null && this.serverRepoId != null;
    }

}
