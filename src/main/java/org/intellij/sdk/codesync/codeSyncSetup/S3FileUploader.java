package org.intellij.sdk.codesync.codeSyncSetup;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.intellij.sdk.codesync.CodeSyncLogger;
import org.intellij.sdk.codesync.clients.CodeSyncClient;
import org.intellij.sdk.codesync.exceptions.FileLockedError;
import org.intellij.sdk.codesync.exceptions.InvalidYmlFileError;
import org.intellij.sdk.codesync.exceptions.RequestError;
import org.intellij.sdk.codesync.files.S3UploadQueueFile;
import org.intellij.sdk.codesync.repoManagers.OriginalsRepoManager;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

public class S3FileUploader {
    S3UploadQueueFile s3UploadQueueFile;
    Integer failedCount;
    Map<String, Object> failedFilePathsAndURLs = new HashMap<>();

    public S3FileUploader (S3UploadQueueFile s3UploadQueueFile) {
        this.s3UploadQueueFile = s3UploadQueueFile;
        this.failedCount = this.s3UploadQueueFile.getFailedCount();
    }
    public S3FileUploader (String repoPath, String branch, Map<String, Object> filePathAndURLs) {
        this.failedCount = 0;
        this.s3UploadQueueFile = new S3UploadQueueFile(repoPath, branch, filePathAndURLs, failedCount);
    }

    public void saveURLs() throws InvalidYmlFileError, FileNotFoundException {
        try {
            this.s3UploadQueueFile.writeYml();
        } catch (FileLockedError error) {
            // This would never happen, but adding it for completeness.
            CodeSyncLogger.error("File locked error while creating a new file for S3 file upload queue.");
        }
    }

    /*
    Pre-Process and validate yml data. return `true` of we are good to proceed, `false` otherwise.
    */
    public boolean is_valid() {
        if (!this.s3UploadQueueFile.hasFiles()){
            return false;
        }

        // If previous 3 attempts have resulted in failures then do not try again.
        return this.failedCount <= 3;
    }

    public void uploadToS3(String repoPath, String branchName, Map<String, Object> fileUrls) {
        OriginalsRepoManager originalsRepoManager = new OriginalsRepoManager(repoPath, branchName);
        CodeSyncClient codeSyncClient = new CodeSyncClient();

        for (Map.Entry<String, Object> fileUrl : fileUrls.entrySet()) {
            File originalsFile = originalsRepoManager.getFilePath(fileUrl.getKey()).toFile();
            if (fileUrl.getValue().equals("")) {
                // Skip if file is empty.
                continue;
            }
            try {
                codeSyncClient.uploadToS3(originalsFile, (Map<String, Object>) fileUrl.getValue());

                // File uploaded with success, so delete it from the originals.
                originalsRepoManager.deleteFile(originalsFile.getPath());
            } catch (RequestError error) {
                this.failedFilePathsAndURLs.put(fileUrl.getKey(), fileUrl.getValue());
                CodeSyncLogger.critical(
                    String.format("[S3_FILE_UPLOAD]: Could not upload file to S3. Error %s", error.getMessage())
                );
            } catch (ClassCastException error) {
                CodeSyncLogger.critical(
                    String.format("[S3_FILE_UPLOAD]: Could not process yaml content. Error %s", error.getMessage())
                );
            }
        }

        if (this.failedFilePathsAndURLs.isEmpty()) {
            // Remove Originals repo as they are not needed anymore.
            originalsRepoManager.delete();
        }
    }

    // Process and upload files to S3.
    private void processFiles() {
        this.uploadToS3(
            this.s3UploadQueueFile.getRepoPath(),
            this.s3UploadQueueFile.getBranch(),
            this.s3UploadQueueFile.getFilePathAndURLs()
        );
        if (!this.failedFilePathsAndURLs.isEmpty()) {
            try {
                this.s3UploadQueueFile.setFilePathAndURLs(this.failedFilePathsAndURLs);
                this.s3UploadQueueFile.setFailedCount(++this.failedCount);
                this.saveURLs();
            } catch (InvalidYmlFileError | FileNotFoundException error) {
                CodeSyncLogger.critical(
                    String.format(
                        "[S3_FILE_UPLOAD]: Error while saving files that could not be uploaded. Error %s",
                        error.getMessage()
                    )
                );
            }
        } else {
            this.s3UploadQueueFile.removeFile();
        }
    }

    public void triggerAsyncTask(Project project) {
        if (!this.is_valid()) {
            this.s3UploadQueueFile.removeFile();
            return;
        }
        S3FilesUploader.registerFileBeingProcessed(this.getQueueFilePath());
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Syncing files") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                processFiles();
            }
        });
    }

    public String getQueueFilePath () {
        return this.s3UploadQueueFile.getYmlFile().getPath();
    }
}
