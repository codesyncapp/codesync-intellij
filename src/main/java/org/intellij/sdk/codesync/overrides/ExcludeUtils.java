package org.intellij.sdk.codesync.overrides;

import com.neva.commons.gitignore.PathPatternList;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class ExcludeUtils {
    static PathPatternList readExcludeFile(File file, String basePath) {
        if (file.exists() && file.canRead()) {
            BufferedReader reader = null;
            PathPatternList list = null;

            Object var5;
            try {
                reader = new BufferedReader(new FileReader(file));

                String line;
                while((line = reader.readLine()) != null) {
                    if (line.length() > 0 && line.charAt(0) != '#') {
                        if (list == null) {
                            list = new PathPatternList(basePath);
                        }

                        list.add(line);
                    }
                }

                return list;
            } catch (Throwable var15) {
                var5 = null;
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException var14) {
                    }
                }

            }

            return (PathPatternList)var5;
        } else {
            return null;
        }
    }

    static String getRelativePath(File wd, File file) {
        int skip = file.getPath().length() > wd.getPath().length() ? 1 : 0;
        String relName = file.getPath().substring(wd.getPath().length() + skip);
        if (File.separatorChar != '/') {
            relName = relName.replace(File.separatorChar, '/');
        }

        return relName;
    }

}
