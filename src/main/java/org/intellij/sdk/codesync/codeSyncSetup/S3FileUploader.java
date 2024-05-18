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
import org.intellij.sdk.codesync.utils.CommonUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

public class S3FileUploader {
    S3UploadQueueFile s3UploadQueueFile;
    Integer runCount;
    Map<String, Object> failedFilePathsAndURLs = new HashMap<>();

    final Integer maxRunCount = 10;

    public S3FileUploader (S3UploadQueueFile s3UploadQueueFile) {
        this.s3UploadQueueFile = s3UploadQueueFile;
        this.runCount = this.s3UploadQueueFile.getRunCount();
    }
    public S3FileUploader (String repoPath, String branch, Map<String, Object> filePathAndURLs) {
        this.runCount = 0;
        this.s3UploadQueueFile = new S3UploadQueueFile(repoPath, branch, filePathAndURLs, runCount);
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
    public boolean isValid() {
        if (!this.s3UploadQueueFile.hasValidFields()){
            return false;
        }

        // Make sure to not exceed the max run count.
        return this.runCount <= this.maxRunCount;
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
                CodeSyncLogger.info(String.format("[S3_FILE_UPLOAD]: Uploading file '%s' to S3.", fileUrl.getKey()));
                codeSyncClient.uploadToS3(originalsFile, (Map<String, Object>) fileUrl.getValue());
                CodeSyncLogger.info(String.format("[S3_FILE_UPLOAD]: Uploaded file '%s' to S3.", fileUrl.getKey()));

                // File uploaded with success, so delete it from the originals.
                originalsRepoManager.deleteFile(originalsFile.getPath());
            } catch (RequestError error) {
                this.failedFilePathsAndURLs.put(fileUrl.getKey(), fileUrl.getValue());
                CodeSyncLogger.critical(
                    String.format(
                        "[S3_FILE_UPLOAD]: Could not upload file '%s' to S3. Error %s",
                        fileUrl.getKey(),
                        error.getMessage()
                    )
                );
            } catch (ClassCastException error) {
                CodeSyncLogger.critical(
                    String.format(
                        "[S3_FILE_UPLOAD]: Could not process yaml content. Error %s",
                        CommonUtils.getStackTrace(error)
                    )
                );
            }
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
                this.s3UploadQueueFile.setRunCount(++this.runCount);
                this.saveURLs();
            } catch (InvalidYmlFileError | FileNotFoundException error) {
                CodeSyncLogger.critical(
                    String.format(
                        "[S3_FILE_UPLOAD]: Error while saving files that could not be uploaded. Error %s",
                        CommonUtils.getStackTrace(error)
                    )
                );
            }

            S3FilesUploader.removeFileBeingProcessed(this.getQueueFilePath());
        } else {
            this.s3UploadQueueFile.removeFile();
        }
    }

    public void triggerAsyncTask(Project project) {
        if (!this.isValid()) {
            CodeSyncLogger.info(String.format(
                "[S3_FILE_UPLOAD]: File '%s' is not valid. Skipping upload.",
                this.getQueueFilePath()
            ));
            this.s3UploadQueueFile.removeFile();
            return;
        }
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "CodeSync: Uploading files…") {
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
