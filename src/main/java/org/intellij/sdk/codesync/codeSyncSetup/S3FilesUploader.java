package org.intellij.sdk.codesync.codeSyncSetup;

import com.intellij.openapi.project.Project;
import org.intellij.sdk.codesync.CodeSyncLogger;
import org.intellij.sdk.codesync.exceptions.InvalidYmlFileError;
import org.intellij.sdk.codesync.files.S3UploadQueueFile;
import org.intellij.sdk.codesync.utils.CommonUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static org.intellij.sdk.codesync.Constants.S3_UPLOAD_QUEUE_DIR;

public class S3FilesUploader {
    private final Project project;
    private final ArrayList<S3FileUploader> s3FileUploaderList = new ArrayList<>();
    private static final Set<String> filesBeingProcessed = new HashSet<>();

    public static void registerFileBeingProcessed(String filePath) {
        filesBeingProcessed.add(filePath);
    }

    public static void removeFileBeingProcessed(String filePath) {
        filesBeingProcessed.remove(filePath);
    }

    public static void triggerS3Uploads (Project project) {
        S3FilesUploader s3FilesUploader = new S3FilesUploader(Paths.get(S3_UPLOAD_QUEUE_DIR), project);
        s3FilesUploader.processFiles();
    }

    public S3FilesUploader (Path s3YMLFilesDirectory, Project project) {
        this.project = project;
        File[] s3YMLFiles = s3YMLFilesDirectory.toFile().listFiles(
            (dir, name) -> name.toLowerCase().endsWith("yml")
        );

        if (s3YMLFiles == null) {
            return;
        }

        for (File s3YMLFile: s3YMLFiles) {
            try {
                S3UploadQueueFile s3UploadQueueFile = new S3UploadQueueFile(s3YMLFile);
                this.s3FileUploaderList.add(
                    new S3FileUploader(s3UploadQueueFile)
                );
            } catch (InvalidYmlFileError | FileNotFoundException error) {
                CodeSyncLogger.error(
                    String.format(
                        "[S3_FILE_UPLOAD]: Error while reading S3 yml file. Error: %s",
                        CommonUtils.getStackTrace(error)
                    )
                );
            }
        }
    }

    /*
    Process al s3 YML files and upload pending files to S3.
    */
    private void processFiles () {
        for (S3FileUploader s3FileUploader: this.s3FileUploaderList) {
            String filePath = s3FileUploader.getQueueFilePath();
            if (!filesBeingProcessed.contains(filePath)){
                CodeSyncLogger.info(String.format("[S3_FILE_UPLOAD]: Processing file: %s", filePath));
                registerFileBeingProcessed(filePath);
                s3FileUploader.triggerAsyncTask(project);
            } else {
                CodeSyncLogger.info(String.format("[S3_FILE_UPLOAD]: File is already being processed: %s", filePath));
            }
        }
    }
}
