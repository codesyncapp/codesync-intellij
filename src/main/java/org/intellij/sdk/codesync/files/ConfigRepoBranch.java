package org.intellij.sdk.codesync.files;

import java.util.Collections;
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
    public void removeFileId (String relativeFilePath) {
        this.files.remove(relativeFilePath);
    }
    public boolean hasFile (String relativeFilePath) {
        return this.files.containsKey(relativeFilePath);
    }

    public Map<String, Integer> getFiles () {
        return this.files;
    }
    /*
    Update all files of this branch.
     */
    public void updateFiles (Map<String, Integer> files) {
        this.files = files;
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

    /*
    Check if this branch has valid files or not. Files with `null` values are considered invalid. This check is useful
    to query if all files are invalid or not.
    */
    public boolean hasValidFiles() {
        if (files.size() == 0) {
            return true;
        }

        // return `true` if even a single file is valid (i.e. is not `null`).
        return Collections.frequency(files.values(), null) != files.size();
     }
}
