package org.intellij.sdk.codesync.models;

/*
Class to hold information regarding user's plan.
 */
public class UserPlan {
    public Long size, fileCount, repoCount;

    public UserPlan(Long size, Long fileCount, Long repoCount) {
        this.size = size;
        this.fileCount = fileCount;
        this.repoCount = repoCount;
    }
}
