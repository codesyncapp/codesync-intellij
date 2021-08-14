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
    public void removeFileId (String fileRelativePath) {
        this.files.remove(fileRelativePath);
    }


    /*
    Check if any file in this branch has invalid files or not. Files with `null` values are considered invalid.
    */
    public boolean hasInvalidFiles() {
        for (Map.Entry<String, Integer> fileEntry : files.entrySet()) {
            if (fileEntry.getValue() == null){
                return true;
            }
        }

        // No invalid file found.
        return false;
    }
}
