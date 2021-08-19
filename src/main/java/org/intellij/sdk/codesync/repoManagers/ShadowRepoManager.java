package org.intellij.sdk.codesync.repoManagers;

import java.io.IOException;

import static org.intellij.sdk.codesync.Constants.SHADOW_REPO;

public class ShadowRepoManager extends BaseRepoManager {
    String shadowDirectory, repoPath;

    /*
    Constructor for ShadowRepoManager.

    @param  repoPath  an absolute path giving the base location of the repo.
    */
    public ShadowRepoManager (String repoPath) {
        this.shadowDirectory = SHADOW_REPO;
        this.repoPath = repoPath;
    }

    /*
    Constructor for ShadowRepoManager.

    @param  shadowDirectory  an absolute path giving the directory containing all shadow repos.
    @param  repoPath  an absolute path giving the base location of the repo.
    */
    public ShadowRepoManager (String shadowDirectory, String repoPath) {
        this.shadowDirectory = shadowDirectory;
        this.repoPath = repoPath;
    }

    /*
    Copy all the files provided in the argument to shadow repo.

    @param  filePaths  list of absolute paths of the files to copy.
     */
    public void copyFiles(String[] filePaths) {
        for (String filePath: filePaths) {
            String to = String.format(
                "%s%s",
                // remove trailing forward slash if present.
                this.shadowDirectory.replaceFirst("/$",""),
                filePath
            );

            try {
                this.copyFile(filePath, to);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
