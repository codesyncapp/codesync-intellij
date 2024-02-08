package org.intellij.sdk.codesync.database.models;

/*
    This class is model for RepoFile table, and will contain all accessor and utility methods for managing RepoFile.
*/
public class RepoFile {
    private String path;
    private Integer repoBranchId, id, serverFileId;

    public RepoFile(String path, Integer repoBranchId, Integer id, Integer serverFileId) {
        this.path = path;
        this.repoBranchId = repoBranchId;
        this.id = id;
        this.serverFileId = serverFileId;
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
}
