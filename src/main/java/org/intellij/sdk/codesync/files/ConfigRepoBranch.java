package org.intellij.sdk.codesync.files;

import java.util.Map;

public class ConfigRepoBranch {
    public String branchName;
    public Map<String, Integer> files;

    public ConfigRepoBranch (String branchName, Map<String, Integer> files) {
        this.branchName = branchName;
        this.files  = files;
    }

    public Map<String, Integer> getYMLAsHashMap() {
        return this.files;
    }

    public Integer getFileId(String relativeFilePath) {
        return this.files.get(relativeFilePath);
    }

    public void updateFileId (String relativeFilePath, Integer fileId) {
        this.files.put(relativeFilePath, fileId);
    }
}
