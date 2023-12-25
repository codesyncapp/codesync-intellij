package org.intellij.sdk.codesync.codeSyncSetup;

import com.intellij.openapi.project.Project;
import org.intellij.sdk.codesync.CodeSyncLogger;
import org.intellij.sdk.codesync.exceptions.InvalidYmlFileError;
import org.intellij.sdk.codesync.files.S3UploadQueueFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.stream.Stream;

import static org.intellij.sdk.codesync.Constants.S3_UPLOAD_QUEUE_DIR;

public class S3FilesUploader {
    private final Project project;
    private final ArrayList<S3FileUploader> s3FileUploaderList = new ArrayList<>();

    public static void triggerS3Uploads (Project project) {
        S3FilesUploader s3FilesUploader = new S3FilesUploader(Paths.get(S3_UPLOAD_QUEUE_DIR), project);
        s3FilesUploader.processFiles();
    }

    public S3FilesUploader (Path s3YMLFilesDirectory, Project project) {
        this.project = project;

        File[] s3YMLFiles = {};
        try (Stream<Path> stream = Files.list(s3YMLFilesDirectory)) {
            s3YMLFiles = (File[]) stream
                .filter(file -> !Files.isDirectory(file))
                .filter(path -> path.endsWith("yml"))
                .toArray();
        } catch (IOException e) {
            CodeSyncLogger.error("[S3_FILE_UPLOAD]: Error while getting list of yml files, containing S3 file metadata.");
        }

        for (File s3YMLFile: s3YMLFiles) {
            try {
                S3UploadQueueFile s3UploadQueueFile = new S3UploadQueueFile(s3YMLFile);
                this.s3FileUploaderList.add(
                    new S3FileUploader(s3UploadQueueFile)
                );
            } catch (InvalidYmlFileError | FileNotFoundException error) {
                CodeSyncLogger.error(
                    String.format("[S3_FILE_UPLOAD]: Error while reading S3 yml file. Error: %s", error.getMessage())
                );

            }
        }
    }

    /*
    Process al s3 YML files and upload pending files to S3.
    */
    public void processFiles () {
        for (S3FileUploader s3FileUploader: this.s3FileUploaderList) {
            s3FileUploader.triggerAsyncTask(project);
        }
    }
}
