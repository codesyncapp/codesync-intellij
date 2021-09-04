package org.intellij.sdk.codesync.filters;

import org.apache.commons.io.filefilter.AbstractFileFilter;
import org.intellij.sdk.codesync.files.IgnoreFile;
import org.intellij.sdk.codesync.overrides.GitIgnore;
import org.intellij.sdk.codesync.overrides.SyncIgnore;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;


public class IgnoreFileFilter extends AbstractFileFilter {
    private final GitIgnore gitIgnore;

    public IgnoreFileFilter(File repoPath) {
        if (repoPath == null) {
            throw new IllegalArgumentException("The repoPath must not be null");
        } else {
            this.gitIgnore = new GitIgnore(repoPath);
        }
    }

    public IgnoreFileFilter(String repoPath, IgnoreFile.IgnoreFileType ignoreFileType) {
        if (repoPath == null) {
            throw new IllegalArgumentException("The repoPath must not be null");
        } else {
            if (ignoreFileType == IgnoreFile.IgnoreFileType.GITIGNORE) {
                this.gitIgnore = new GitIgnore(new File(repoPath));
            } else {
                this.gitIgnore = new SyncIgnore(new File(repoPath));
            }
        }
    }

    public boolean accept(File file) {
        PathMatcher gitFileMatcher = new GitFileMatcher();

        return !this.gitIgnore.isExcluded(file) && !gitFileMatcher.matches(Paths.get(file.getPath()));
    }

    public boolean accept(Path path) {
        return this.accept(path.toFile());
    }

    public boolean accept(String name) {
        return this.accept(new File(name));
    }

}
