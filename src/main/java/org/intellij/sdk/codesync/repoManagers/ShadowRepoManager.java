package org.intellij.sdk.codesync.repoManagers;

import org.intellij.sdk.codesync.Utils;

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

        if (Utils.isWindows()) {
            this.projectRepoPath = this.projectRepoPath.replace(":", "");
        }

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

        if (Utils.isWindows()) {
            this.projectRepoPath = this.projectRepoPath.replace(":", "");
        }


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
        copyFiles(filePaths, this.projectRepoPath);
    }

    /*
    Copy all the files provided in the argument to shadow repo.

    @param  filePaths  list of absolute paths of the files to copy.
    @param  projectRepoPath  String path of the project root of the source file, this is used to
        generated relative path in the target repo.
     */
    public void copyFiles(String[] filePaths, String projectRepoPath) {
        for (String filePath: filePaths) {
            String originalFilePath = filePath;

            if (Utils.isWindows()) {
                filePath = filePath.replace(":", "");
            }
            String to = Paths.get(this.shadowRepoDir, filePath.replace(projectRepoPath, "")).toString();
            try {
                this.copyFile(originalFilePath, to);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
