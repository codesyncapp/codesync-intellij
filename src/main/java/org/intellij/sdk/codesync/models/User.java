package org.intellij.sdk.codesync.models;

/*
Class for holding user related information.
 */
public class User {
    public String email;
    public Long repoCount;
    public UserPlan plan;

    public User(String email, Long repoCount, UserPlan plan) {
        this.email = email;
        this.repoCount = repoCount;
        this.plan = plan;
    }
}
