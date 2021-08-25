package org.intellij.sdk.codesync.files;

import org.intellij.sdk.codesync.exceptions.FileNotFoundError;

import org.intellij.sdk.codesync.filters.IgnoreFileFilter;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

/*
File for handling .*ignore files like .gitignore, .syncignore etc.
*/
public class IgnoreFile {
    File ignoreFile;
    FileFilter fileFilter;

    public IgnoreFile(String filePath) throws FileNotFoundError {
        File ignoreFile = new File(filePath);

        if (!ignoreFile.isFile()) {
            throw new FileNotFoundError(String.format("File with path '%s' does not exist.", filePath));
        }

        this.ignoreFile = ignoreFile;
        this.fileFilter = new IgnoreFileFilter(ignoreFile.getParent());
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
