package org.intellij.sdk.codesync.filters;

import java.nio.file.Path;
import java.nio.file.PathMatcher;

public class GitFileMatcher implements PathMatcher {
    private final String gitDirectoryName = ".git/";

    @Override
    public boolean matches(Path path) {
        return path.toString().contains(gitDirectoryName);
    }
}
