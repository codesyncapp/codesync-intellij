/*
This is copied from com.neva.commons.gitignore.GitIgnore to make this class attributes public so that they can
be overridden in child classes.
 */
package org.intellij.sdk.codesync.overrides;

import com.neva.commons.gitignore.PathPattern;
import com.neva.commons.gitignore.PathPatternList;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class GitIgnore {
    public final File rootDir;
    public Map<File, PathPatternList> patternListCache = new HashMap();
    public List<PathPatternList> patternDefaults = new LinkedList();

    public String getFileName() {
        return ".gitignore";
    }

    public GitIgnore(File rootDir) {
        this.rootDir = rootDir;
        this.addPatterns(rootDir);
    }

    public GitIgnore addPatterns(File dir) {
        return this.addPatterns(dir, "");
    }

    public GitIgnore addPatterns(File dir, String basePath) {
        PathPatternList patterns = this.getDirectoryPattern(dir, basePath);
        if (patterns != null) {
            this.patternDefaults.add(patterns);
        }

        return this;
    }

    public boolean isExcluded(File file) {
        File curDir = this.rootDir;
        String filePath = ExcludeUtils.getRelativePath(curDir, file);
        Vector<PathPatternList> stack = new Vector(10);
        StringBuilder pathBuilder = new StringBuilder(filePath.length());
        stack.addAll(this.patternDefaults);

        while(true) {
            int length = pathBuilder.length();
            int offset = filePath.indexOf(47, pathBuilder.length() + 1);
            boolean isDirectory = true;
            if (offset == -1) {
                offset = filePath.length();
                isDirectory = file.isDirectory();
            }

            pathBuilder.insert(pathBuilder.length(), filePath, pathBuilder.length(), offset);
            String currentPath = pathBuilder.toString();

            for(int i = stack.size() - 1; i >= 0; --i) {
                PathPatternList patterns = (PathPatternList)stack.get(i);
                PathPattern pattern = patterns.findPattern(currentPath, isDirectory);
                if (pattern != null) {
                    return pattern.isExclude();
                }
            }

            if (!isDirectory || pathBuilder.length() >= filePath.length()) {
                return false;
            }

            curDir = new File(curDir, pathBuilder.substring(length, offset));
            PathPatternList patterns = this.getDirectoryPattern(curDir, currentPath);
            if (patterns != null) {
                stack.add(patterns);
            }
        }
    }

    private PathPatternList getDirectoryPattern(File dir, String dirPath) {
        return this.getPatternList(new File(dir, this.getFileName()), dirPath);
    }

    private PathPatternList getPatternList(File file, String basePath) {
        PathPatternList list = (PathPatternList)this.patternListCache.get(file);
        if (list == null) {
            list = ExcludeUtils.readExcludeFile(file, basePath);
            if (list == null) {
                return null;
            }

            this.patternListCache.put(file, list);
        }

        return list;
    }
}
