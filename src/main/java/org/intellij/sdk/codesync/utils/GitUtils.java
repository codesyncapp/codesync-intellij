package org.intellij.sdk.codesync.utils;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;

import java.io.IOException;

import static org.intellij.sdk.codesync.Constants.DEFAULT_BRANCH;

public class GitUtils {
    public static String getBranchName (String repoPath) {
        try (Git git = Git.open(new java.io.File(repoPath))) {
            return git.getRepository().getBranch();
        } catch (IOException e) {
            return DEFAULT_BRANCH;
        }
    }

    public static String getCommitHash(String repoPath) {
        try (Git git = Git.open(new java.io.File(repoPath))) {
            ObjectId objectId = git.getRepository().exactRef(Constants.HEAD).getObjectId();
            return objectId != null ? objectId.name() : null;
        } catch (IOException error) {
            return null;
        }
    }
}
