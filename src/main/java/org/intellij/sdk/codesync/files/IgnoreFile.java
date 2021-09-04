package org.intellij.sdk.codesync.files;

import org.intellij.sdk.codesync.exceptions.FileNotFoundError;

import org.intellij.sdk.codesync.filters.IgnoreFileFilter;

import java.io.File;
import java.io.FileFilter;
import java.nio.file.Paths;

/*
File for handling .*ignore files like .gitignore, .syncignore etc.
*/
public class IgnoreFile {
    File ignoreFile;
    FileFilter fileFilter;

    public enum IgnoreFileType {
        GITIGNORE  (".gitignore"),
        SYNCIGNORE (".syncignore");

        private final String fileName;

        IgnoreFileType(String fileName) {
            this.fileName = fileName;
        }

        public String getFileName() {
            return this.fileName;
        }
    }

    public IgnoreFile(String repoPath) throws FileNotFoundError {
        File ignoreFile;

        // First check if syncignore file is present.
        ignoreFile = Paths.get(repoPath, IgnoreFileType.SYNCIGNORE.getFileName()).toFile();
        if (ignoreFile.isFile()) {
            this.ignoreFile = ignoreFile;
            this.fileFilter = new IgnoreFileFilter(ignoreFile.getParent(), IgnoreFileType.SYNCIGNORE);

            // return now.
            return;
        }

        ignoreFile = Paths.get(repoPath, IgnoreFileType.GITIGNORE.getFileName()).toFile();
        if (ignoreFile.isFile()) {
            this.ignoreFile = ignoreFile;
            this.fileFilter = new IgnoreFileFilter(ignoreFile.getParent(), IgnoreFileType.GITIGNORE);

            // return now.
            return;
        }

        throw new FileNotFoundError(String.format(
                "None of the ignore files '%s' exists in %s.",
                String.join(",", new String[]{IgnoreFileType.SYNCIGNORE.getFileName(), IgnoreFileType.GITIGNORE.getFileName()}),
                repoPath
        ));
    }

    /*
    Test if the given file should be ignore or accepted.
    */
    public boolean shouldIgnore(String filePath) {
        return shouldIgnore(new File(filePath));
    }

    /*
    Test if the given file should be ignore or accepted.
    */
    public boolean shouldIgnore(File file) {
        return !this.fileFilter.accept(file);
    }

    public FileFilter getFileFilter() {
        return this.fileFilter;
    }
}
