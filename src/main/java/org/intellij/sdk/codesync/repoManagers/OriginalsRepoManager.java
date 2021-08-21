package org.intellij.sdk.codesync.repoManagers;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import static org.intellij.sdk.codesync.Constants.ORIGINALS_REPO;

public class OriginalsRepoManager extends BaseRepoManager {
    String originalsDirectory, repoPath, branchName;

    /*
    Constructor for OriginalsRepoManager.

    @param  repoPath  an absolute path giving the base location of the repo.
    */
    public OriginalsRepoManager(String repoPath, String branchName) {
        this.originalsDirectory = ORIGINALS_REPO;
        this.repoPath = repoPath;
        this.branchName = branchName;
    }

    /*
    Constructor for OriginalsRepoManager.

    @param  originalsDirectory  an absolute path giving the directory containing all original repos.
    @param  repoPath  an absolute path giving the base location of the repo.
    */
    public OriginalsRepoManager(String originalsDirectory, String repoPath, String branchName) {
        this.originalsDirectory = originalsDirectory;
        this.repoPath = repoPath;
        this.branchName = branchName;
    }

    /*
    Copy all the files provided in the argument to originals repo.

    @param  filePaths  list of absolute paths of the files to copy.
     */
    public void copyFiles(String[] filePaths) {
        String originalsRepoDir  = Paths.get(this.originalsDirectory, this.repoPath, this.branchName).toString();

        for (String filePath: filePaths) {
            String to = Paths.get(originalsRepoDir, filePath.replace(this.repoPath, "")).toString();

            try {
                this.copyFile(filePath, to);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void delete() {
        try {
            String originalsDirectory = Paths.get(this.originalsDirectory, this.repoPath, this.branchName).toString();
            FileUtils.deleteDirectory(new File(originalsDirectory));
        } catch (IOException e) {
            // Ignore error/
        }
    }

}
