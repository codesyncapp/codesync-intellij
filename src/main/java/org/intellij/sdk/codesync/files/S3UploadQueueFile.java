package org.intellij.sdk.codesync.files;

import org.intellij.sdk.codesync.exceptions.InvalidYmlFileError;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.intellij.sdk.codesync.Constants.S3_UPLOAD_QUEUE_DIR;

public class S3UploadQueueFile extends CodeSyncYmlFile {
    File ymlFile;
    String repoPath, branch;
    Integer runCount=0;
    Map<String, Object> filePathAndURLs = new HashMap<>();

    /*
    Constructor to create a new file.
     */
    public S3UploadQueueFile(String repoPath, String branch, Map<String, Object> filePathAndURLs, Integer runCount) {
        this.repoPath = repoPath;
        this.branch = branch;
        this.filePathAndURLs = filePathAndURLs;
        this.runCount = runCount;

        this.ymlFile = Paths.get(
            S3_UPLOAD_QUEUE_DIR, String.format("%s.yml", System.currentTimeMillis())
        ).toFile();

        // Create empty file.
        createFile(this.ymlFile.getPath());
    }

    /*
    Constructor to load an existing file.
     */
    public S3UploadQueueFile (File ymlFilePath) throws InvalidYmlFileError, FileNotFoundException {
        this.ymlFile = ymlFilePath;

        if (!this.ymlFile.isFile()) {
            throw new InvalidYmlFileError("S3 Upload Queue file path must be absolute path pointing to a file.");
        }
        Map<String, Object> contentsMap = this.readYml();
        if (contentsMap == null) {
            return;
        }

        this.repoPath = (String) contentsMap.get("repo_path");
        this.branch = (String) contentsMap.get("branch");
        this.runCount = (Integer) contentsMap.get("run_count");
        this.filePathAndURLs = (Map<String, Object>) contentsMap.get("file_path_and_urls");
    }

    @Override
    public File getYmlFile() {
        return this.ymlFile;
    }

    @Override
    public Map<String, Object> getYMLAsHashMap() {
        Map<String, Object> ymlContent = new HashMap<>();
        ymlContent.put("repo_path", this.repoPath);
        ymlContent.put("branch", this.branch);
        ymlContent.put("run_count", this.runCount);
        ymlContent.put("file_path_and_urls", this.filePathAndURLs);

        return ymlContent;
    }

    /*
    Return `true` if there are S3 files that need to be uploaded, `false` otherwise.
     */
    public boolean hasValidFields() {
        return repoPath != null && branch != null && !filePathAndURLs.isEmpty();
    }

    public Integer getRunCount () {
        return this.runCount;
    }
    public void setRunCount(Integer runCount) {
        this.runCount = runCount;
    }
    public String getRepoPath () {
        return this.repoPath;
    }
    public String getBranch () {
        return this.branch;
    }
    public Map<String, Object> getFilePathAndURLs () {
        return this.filePathAndURLs;
    }
    public void setFilePathAndURLs (Map<String, Object> filePathAndURLs) {
        this.filePathAndURLs = filePathAndURLs;
    }
}
