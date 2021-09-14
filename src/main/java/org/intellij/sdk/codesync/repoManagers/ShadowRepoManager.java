package org.intellij.sdk.codesync.repoManagers;

import java.io.IOException;
import java.nio.file.Paths;

import static org.intellij.sdk.codesync.Constants.SHADOW_REPO;

public class ShadowRepoManager extends BaseRepoManager {
    String shadowDirectory, projectRepoPath, branchName;

    private String shadowRepoDir;

    /*
    Constructor for ShadowRepoManager.

    @param  repoPath  an absolute path giving the base location of the repo.
    */
    public ShadowRepoManager (String projectRepoPath, String branchName) {
        this.shadowDirectory = SHADOW_REPO;
        this.projectRepoPath = projectRepoPath;
        this.branchName = branchName;

        this.shadowRepoDir  = Paths.get(this.shadowDirectory, this.projectRepoPath, this.branchName).toString();
    }

    /*
    Constructor for ShadowRepoManager.

    @param  shadowDirectory  an absolute path giving the directory containing all shadow repos.
    @param  repoPath  an absolute path giving the base location of the repo.
    */
    public ShadowRepoManager (String shadowDirectory, String projectRepoPath, String branchName) {
        this.shadowDirectory = shadowDirectory;
        this.projectRepoPath = projectRepoPath;
        this.branchName = branchName;

        this.shadowRepoDir  = Paths.get(this.shadowDirectory, this.projectRepoPath, this.branchName).toString();
    }

    public String getBaseRepoBranchDir(){
        return this.shadowRepoDir;
    }


    /*
    Copy all the files provided in the argument to shadow repo.

    @param  filePaths  list of absolute paths of the files to copy.
     */
    public void copyFiles(String[] filePaths) {
        for (String filePath: filePaths) {
            String to = Paths.get(this.shadowRepoDir, filePath.replace(this.projectRepoPath, "")).toString();
            try {
                this.copyFile(filePath, to);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
