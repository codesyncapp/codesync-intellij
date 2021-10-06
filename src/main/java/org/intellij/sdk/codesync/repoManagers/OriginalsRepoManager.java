package org.intellij.sdk.codesync.repoManagers;

import org.intellij.sdk.codesync.Utils;

import java.io.IOException;
import java.nio.file.Paths;

import static org.intellij.sdk.codesync.Constants.ORIGINALS_REPO;

public class OriginalsRepoManager extends BaseRepoManager {
    String originalsDirectory, projectRepoPath, branchName;

    private String originalsRepoDir;

    /*
    Constructor for OriginalsRepoManager.

    @param  repoPath  an absolute path giving the base location of the repo.
    */
    public OriginalsRepoManager(String projectRepoPath, String branchName) {
        this.originalsDirectory = ORIGINALS_REPO;
        this.projectRepoPath = projectRepoPath;
        this.branchName = branchName;

        if (Utils.isWindows()) {
            this.projectRepoPath = this.projectRepoPath.replace(":", "");
        }

        this.originalsRepoDir  = Paths.get(this.originalsDirectory, this.projectRepoPath, this.branchName).toString();
    }

    /*
    Constructor for OriginalsRepoManager.

    @param  originalsDirectory  an absolute path giving the directory containing all original repos.
    @param  repoPath  an absolute path giving the base location of the repo.
    */
    public OriginalsRepoManager(String originalsDirectory, String projectRepoPath, String branchName) {
        this.originalsDirectory = originalsDirectory;
        this.projectRepoPath = projectRepoPath;
        this.branchName = branchName;

        if (Utils.isWindows()) {
            this.projectRepoPath = this.projectRepoPath.replace(":", "");
        }

        this.originalsRepoDir  = Paths.get(this.originalsDirectory, this.projectRepoPath, this.branchName).toString();
    }

    public String getBaseRepoBranchDir(){
        return this.originalsRepoDir;
    }

    /*
    Copy all the files provided in the argument to originals repo.

    @param  filePaths  list of absolute paths of the files to copy.
     */
    public void copyFiles(String[] filePaths) {
        copyFiles(filePaths, this.projectRepoPath);
    }

    /*
    Copy all the files provided in the argument to originals repo.

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

            String to = Paths.get(this.originalsRepoDir, filePath.replace(projectRepoPath, "")).toString();

            try {
                this.copyFile(originalFilePath, to);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
