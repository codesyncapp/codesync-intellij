package org.intellij.sdk.codesync.repoManagers;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public abstract class BaseRepoManager {
    /*
        This is the path that will contain the repo file, This will mainly be composed of the following components
            1. base repo manager path, this will normally come from the constants file e.g. Constants.SHADOW_REPO
            2. repoPath (path of the projectRepo) contains the source code files.
            3. branchName (git branch name)
    */
    abstract String getBaseRepoBranchDir();

    /*
    Copy file present at the `srcFilePath` to a new location at `destFilePath`.

    @param  srcFilePath  the path to the file to copy.
    @param  destFilePath  the path to the target file.
     */
    public void copyFile(String srcFilePath, String destFilePath) throws IOException {
        Path src = Paths.get(srcFilePath);
        Path dest = Paths.get(destFilePath);

        // Create directory if it does not exist.
        if (!dest.getParent().toFile().exists()){
            dest.getParent().toFile().mkdirs();
        }

        Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
    }

    /*
    Delete the entire directory containing the repo.
    */
    public void delete() {
        try {
            FileUtils.deleteDirectory(new File(this.getBaseRepoBranchDir()));
        } catch (IOException e) {
            // Ignore error/
        }
    }

    /*
    Delete the one file from the repo.
    */
    public boolean deleteFile(String fileRelativePath) {
        return Paths.get(this.getBaseRepoBranchDir(), fileRelativePath).toFile().delete();
    }

    /*
    Given the relative file path, return the full path of the corresponding file inside the repo.

    @param relativeFilePath  relative path of the file whose repo path is needed.
     */
    public Path getFilePath(String relativeFilePath) {
        return Paths.get(this.getBaseRepoBranchDir(), relativeFilePath);
    }

    /*
    Given the relative file path check if the file exists in the repo.

     @param relativeFilePath  relative path of the file whose presence needs to be checked.
    */
    public boolean hasFile(String relativeFilePath) {
        return this.getFilePath(relativeFilePath).toFile().exists();
    }


    /*
    Given the absolute file path, return the relative path of the corresponding file inside the repo.

    @param relativeFilePath  absolute path of the file whose relative path is needed.
     */
    public String getRelativeFilePath(String filePath) {
        String relativeFilePath = filePath.replace(this.getBaseRepoBranchDir(), "");
        relativeFilePath = relativeFilePath.replaceFirst(String.valueOf(File.separatorChar), "");

        return relativeFilePath;
    }

    /*

     */
    public boolean renameFile(String oldRelativePath, String newRelativePath) {
        Path oldShadowPath = this.getFilePath(oldRelativePath);
        Path newShadowPath = this.getFilePath(newRelativePath);

        File oldShadowFile = oldShadowPath.toFile();
        if (oldShadowFile.exists()) {
            return oldShadowFile.renameTo(newShadowPath.toFile());
        }

        return false;
    }
}
