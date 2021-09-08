package org.intellij.sdk.codesync.overrides;

import java.io.File;

/*
This class overrides com.neva.commons.gitignore.GitIgnore so that it works for .syncignore.
 */
public class SyncIgnore extends GitIgnore {
    public SyncIgnore(File rootDir) {
        super(rootDir);
    }

    public String getFileName() {
        return ".syncignore";
    }
}
