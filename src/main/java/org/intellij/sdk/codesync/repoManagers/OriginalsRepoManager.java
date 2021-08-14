package org.intellij.sdk.codesync.repoManagers;

import java.io.IOException;

import static org.intellij.sdk.codesync.Constants.ORIGINALS_REPO;

public class OriginalsRepoManager extends BaseRepoManager {
    String originalsDirectory, repoPath;

    /*
    Constructor for OriginalsRepoManager.

    @param  repoPath  an absolute path giving the base location of the repo.
    */
    public OriginalsRepoManager(String repoPath) {
        this.originalsDirectory = ORIGINALS_REPO;
        this.repoPath = repoPath;
    }

    /*
    Constructor for OriginalsRepoManager.

    @param  originalsDirectory  an absolute path giving the directory containing all original repos.
    @param  repoPath  an absolute path giving the base location of the repo.
    */
    public OriginalsRepoManager(String originalsDirectory, String repoPath) {
        this.originalsDirectory = originalsDirectory;
        this.repoPath = repoPath;
    }

    /*
    Copy all the files provided in the argument to originals repo.

    @param  filePaths  list of absolute paths of the files to copy.
     */
    public void copyFiles(String[] filePaths) {
        for (String filePath: filePaths) {
            String to = String.format(
                    "%s%s",
                    // remove trailing forward slash if present.
                    this.originalsDirectory.replaceFirst("/$",""),
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
