package org.intellij.sdk.codesync.repoManagers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class BaseRepoManager {

    /*
    Copy file present at the `srcFilePath` to a new location at `destFilePath`.

    @param  srcFilePath  the path to the file to copy.
    @param  destFilePath  the path to the target file.
     */
    public void copyFile(String srcFilePath, String destFilePath) throws IOException {
        Path src = Paths.get(srcFilePath);
        Path dest = Paths.get(destFilePath);

        // Create directory if it does not exist.
        if (!dest.getParent().toFile().exists()){
            dest.getParent().toFile().mkdirs();
        }

        Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
    }
}
