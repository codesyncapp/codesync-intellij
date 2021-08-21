package org.intellij.sdk.codesync.repoManagers;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import static org.intellij.sdk.codesync.Constants.SHADOW_REPO;

public class ShadowRepoManager extends BaseRepoManager {
    String shadowDirectory, repoPath, branchName;

    /*
    Constructor for ShadowRepoManager.

    @param  repoPath  an absolute path giving the base location of the repo.
    */
    public ShadowRepoManager (String repoPath, String branchName) {
        this.shadowDirectory = SHADOW_REPO;
        this.repoPath = repoPath;
        this.branchName = branchName;
    }

    /*
    Constructor for ShadowRepoManager.

    @param  shadowDirectory  an absolute path giving the directory containing all shadow repos.
    @param  repoPath  an absolute path giving the base location of the repo.
    */
    public ShadowRepoManager (String shadowDirectory, String repoPath, String branchName) {
        this.shadowDirectory = shadowDirectory;
        this.repoPath = repoPath;
        this.branchName = branchName;
    }

    /*
    Copy all the files provided in the argument to shadow repo.

    @param  filePaths  list of absolute paths of the files to copy.
     */
    public void copyFiles(String[] filePaths) {
        String shadowRepoDir  = Paths.get(this.shadowDirectory, this.repoPath, this.branchName).toString();

        for (String filePath: filePaths) {
            String to = Paths.get(shadowRepoDir, filePath.replace(this.repoPath, "")).toString();
            try {
                this.copyFile(filePath, to);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void delete() {
        try {
            String shadowDirectory = Paths.get(this.shadowDirectory, this.repoPath, this.branchName).toString();
            FileUtils.deleteDirectory(new File(shadowDirectory));
        } catch (IOException e) {
            // Ignore error/
        }
    }

}
