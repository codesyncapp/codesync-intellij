package org.intellij.sdk.codesync.repoManagers;

import org.intellij.sdk.codesync.utils.CommonUtils;

import java.io.IOException;
import java.nio.file.Paths;

import static org.intellij.sdk.codesync.Constants.DELETED_REPO;

public class DeletedRepoManager extends BaseRepoManager {
    String deletedDirectory, projectRepoPath, branchName;

    private String deletedRepoDir;

    /*
    Constructor for DeletedRepoManager.

    @param  repoPath  an absolute path giving the base location of the repo.
    */
    public DeletedRepoManager(String projectRepoPath, String branchName) {
        this.deletedDirectory = DELETED_REPO;
        this.projectRepoPath = projectRepoPath;
        this.branchName = branchName;

        if (CommonUtils.isWindows()) {
            this.projectRepoPath = this.projectRepoPath.replace(":", "");
        }

        this.deletedRepoDir  = Paths.get(this.deletedDirectory, this.projectRepoPath, this.branchName).toString();
    }

    /*
    Constructor for DeletedRepoManager.

    @param  deletedDirectory  an absolute path giving the directory containing all deleted repos.
    @param  repoPath  an absolute path giving the base location of the repo.
    */
    public DeletedRepoManager(String deletedDirectory, String projectRepoPath, String branchName) {
        this.deletedDirectory = deletedDirectory;
        this.projectRepoPath = projectRepoPath;
        this.branchName = branchName;

        if (CommonUtils.isWindows()) {
            this.projectRepoPath = this.projectRepoPath.replace(":", "");
        }

        this.deletedRepoDir  = Paths.get(this.deletedDirectory, this.projectRepoPath, this.branchName).toString();
    }

    public String getBaseRepoBranchDir(){
        return this.deletedRepoDir;
    }


    /*
    Copy all the files provided in the argument to deleted repo.

    @param  filePaths  list of absolute paths of the files to copy.
     */
    public void copyFiles(String[] filePaths) {
        copyFiles(filePaths, this.projectRepoPath);
    }

    /*
    Copy all the files provided in the argument to deleted repo.

    @param  filePaths  list of absolute paths of the files to copy.
    @param  projectRepoPath  String path of the project root of the source file, this is used to
        generated relative path in the target repo.
    */
    public void copyFiles(String[] filePaths, String projectRepoPath) {
        for (String filePath: filePaths) {
            String originalFilePath = filePath;

            if (CommonUtils.isWindows()) {
                filePath = filePath.replace(":", "");
            }

            String to = Paths.get(this.deletedRepoDir, filePath.replace(projectRepoPath, "")).toString();
            try {
                this.copyFile(originalFilePath, to);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
