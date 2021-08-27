package org.intellij.sdk.codesync.filters;

import com.neva.commons.gitignore.GitIgnore;
import org.apache.commons.io.filefilter.AbstractFileFilter;

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

    public IgnoreFileFilter(String repoPath) {
        if (repoPath == null) {
            throw new IllegalArgumentException("The repoPath must not be null");
        } else {
            this.gitIgnore = new GitIgnore(new File(repoPath));
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
