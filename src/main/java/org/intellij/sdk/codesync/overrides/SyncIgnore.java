package org.intellij.sdk.codesync.overrides;

import com.neva.commons.gitignore.ExcludeUtils;
import com.neva.commons.gitignore.GitIgnore;
import com.neva.commons.gitignore.PathPatternList;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/*
This class overrides com.neva.commons.gitignore.GitIgnore so that it works for .syncignore.
 */
public class SyncIgnore extends GitIgnore {
    public static final String FILE_NAME = ".syncignore";
    public Map<File, PathPatternList> patternListCache = new HashMap();

    public SyncIgnore(File rootDir) {
        super(rootDir);
    }

    private PathPatternList getDirectoryPattern(File dir, String dirPath) {
        return this.getPatternList(new File(dir, FILE_NAME), dirPath);
    }

    private PathPatternList getPatternList(File file, String basePath) {
        PathPatternList list = (PathPatternList)this.patternListCache.get(file);
        if (list == null) {
            list = SyncIgnoreExcludeUtils.readExcludeFile(file, basePath);
            if (list == null) {
                return null;
            }

            this.patternListCache.put(file, list);
        }

        return list;
    }

}
