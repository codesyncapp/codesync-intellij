package org.intellij.sdk.codesync;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent;
import name.fraser.neil.plaintext.diff_match_patch;
import org.intellij.sdk.codesync.utils.DiffUtils;
import org.intellij.sdk.codesync.utils.FileUtils;
import org.json.simple.JSONObject;
import org.yaml.snakeyaml.Yaml;


import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.stream.Stream;

import static org.intellij.sdk.codesync.Constants.*;

public class Utils {

    public static Boolean shouldSkipEvent(String repoPath) {
        // Skip if config does not exist
        File config = new File(CONFIG_PATH);
        if (!config.exists()) {
            return true;
        }
        // Ensure repo is synced
        Yaml yaml = new Yaml();
        InputStream inputStream;
        try {
            inputStream = new FileInputStream(CONFIG_PATH);
        } catch (FileNotFoundException e) {
            return true;
        }
        Map<String, Map<String, Map<String, Object>>> obj = yaml.load(inputStream);
        return !obj.get("repos").containsKey(repoPath);
    }

    public static Boolean IsGitFile(String path) {
        return path.startsWith(GIT_REPO);
    }

    public static boolean match(String path, String pattern) {
        PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher(String.format("glob:%s", pattern));
        return pathMatcher.matches(Paths.get(path));
    }

    public static boolean shouldIgnoreFile(String relPath, String repoPath) {
        if (relPath.startsWith("/") || Utils.IsGitFile(relPath)) {  return true; }
        String syncIgnorePath = String.format("%s/.syncignore", repoPath);
        File f = new File(syncIgnorePath);
        if (!f.exists()) {
            return false;
        }
        Boolean shouldIgnoreByMatch = false;
        String[] basePathArr = relPath.split("/");
        String baseDir = basePathArr[0];

        String relBaseDir = String.join("/", Arrays.copyOfRange(basePathArr, 0, basePathArr.length-1));
        // Read file
        String syncIgnore = FileUtils.readLineByLineJava8(syncIgnorePath);
        String[] patterns = syncIgnore.split("\n");
        List<String> syncIgnoreDirectories = new ArrayList<String>();
        List<String> excludePaths = new ArrayList<String>();
        for (String pattern : patterns)
        {
            if (pattern.startsWith("!") || pattern.startsWith(REGEX_REPLACE_LEADING_EXCAPED_EXCLAMATION)) {
                pattern = pattern
                        .replace(REGEX_REPLACE_LEADING_EXCAPED_EXCLAMATION, "")
                        .replace("!", "");
                if (pattern.endsWith("/")) {
                    pattern = pattern.substring(0, pattern.length() - 1);
                }
                excludePaths.add(pattern);
                continue;
            }
            shouldIgnoreByMatch = match(relPath, pattern) || shouldIgnoreByMatch;
            String dirPattern = pattern.replace("/", "");
            if (dirPattern.endsWith("*")) {
                dirPattern = dirPattern.substring(0, dirPattern.length() - 1);
            }
            File file = new File(String.format("%s/%s", repoPath, dirPattern));
            if (file.exists() && file.isDirectory()) {
                syncIgnoreDirectories.add(dirPattern);
            }
        }

        // Also ignore top level directories present in .syncignore e.g. node_modules/, .git/, .idea/
        Boolean shouldIgnoreBaseDir = basePathArr.length > 1 && syncIgnoreDirectories.contains(baseDir);
        // Handle case of tests/ with !tests/b.py
        // Skip if base path is in ignorePaths OR relPath is in ignorePaths
        if (excludePaths.contains(relPath) || excludePaths.contains(relBaseDir)) {
            shouldIgnoreBaseDir = false;
            shouldIgnoreByMatch = false;
        }

        if (shouldIgnoreByMatch || shouldIgnoreBaseDir) {
            System.out.println(String.format("Skipping : %s/%s", repoPath, relPath));
        }

        return shouldIgnoreByMatch || shouldIgnoreBaseDir;
    }

    public static String GetGitBranch(String repoPath) {
        String branch = DEFAULT_BRANCH;

        // Get current git branch name
        ProcessBuilder processBuilder = new ProcessBuilder().directory(new File(repoPath));
        // Run a shell command
        processBuilder.command("/bin/bash", "-c", CURRENT_GIT_BRANCH_COMMAND);
        try {
            Process process = processBuilder.start();
            StringBuilder output = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
            int exitVal = process.waitFor();
            if (exitVal == 0) {
                branch = output.toString();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return branch;
    }

    public static void FileCreateHandler(String filePath, String repoPath) {
        // Skip in case of directory
        File eventFile = new File(filePath);
        if (eventFile.isDirectory()) { return; }
        String s = String.format("%s/", repoPath);
        String[] rel_path_arr = filePath.split(s);
        String relPath = rel_path_arr[rel_path_arr.length - 1];
        if (shouldSkipEvent(repoPath) || shouldIgnoreFile(relPath, repoPath)) { return; }
        String branch = Utils.GetGitBranch(repoPath);

        String destOriginals = String.format("%s/%s/%s/%s", ORIGINALS_REPO, repoPath.substring(1), branch, relPath);
        String[] destOriginalsPathSplit = destOriginals.split("/");
        String[] newArray = Arrays.copyOfRange(destOriginalsPathSplit, 0, destOriginalsPathSplit.length-1);
        String destOriginalsBasePath = String.join("/", newArray);

        String destShadow = String.format("%s/%s/%s/%s", SHADOW_REPO, repoPath.substring(1), branch, relPath);
        String[] destShadowPathSplit = destShadow.split("/");
        newArray = Arrays.copyOfRange(destShadowPathSplit, 0, destShadowPathSplit.length-1);
        String destShadowBasePath = String.join("/", newArray);

        File f_originals_base = new File(destOriginalsBasePath);
        f_originals_base.mkdirs();
        File f_shadow_base = new File(destShadowBasePath);
        f_shadow_base.mkdirs();

        File file = new File(filePath);
        File f_originals = new File(destOriginals);
        File f_shadow = new File(destShadow);

        try {
            Files.copy(file.toPath(), f_originals.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (FileAlreadyExistsException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            Files.copy(file.toPath(), f_shadow.toPath());
        } catch (FileAlreadyExistsException e) {
            return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        DiffUtils.writeDiffToYml(repoPath, branch, relPath, "", true,
                false, false, false
        );
        System.out.println(String.format("FileCreated: %s", filePath));
    }

    public static void FileDeleteHandler(VFileEvent event, String repoPath) {
        String filePath = event.getFile().getPath();
        String s = String.format("%s/", repoPath);
        String[] rel_path_arr = filePath.split(s);
        String relPath = rel_path_arr[rel_path_arr.length - 1];
        if (shouldSkipEvent(repoPath) || shouldIgnoreFile(relPath, repoPath)) { return; }
        String branch = Utils.GetGitBranch(repoPath);
        if (event.getFile().isDirectory()) {
            handleDirDelete(repoPath, branch, relPath);
            return;
        }

        String destDeleted = String.format("%s/%s/%s/%s", DELETED_REPO, repoPath.substring(1), branch, relPath);
        String[] destDeletedPathSplit = destDeleted.split("/");
        String[] newArray = Arrays.copyOfRange(destDeletedPathSplit, 0, destDeletedPathSplit.length-1);
        String destDeletedBasePath = String.join("/", newArray);

        String shadowPath = String.format("%s/%s/%s/%s", SHADOW_REPO, repoPath.substring(1), branch, relPath);

        File f_deleted_base = new File(destDeletedBasePath);
        f_deleted_base.mkdirs();

        File f_deleted = new File(destDeleted);
        File f_shadow = new File(shadowPath);

        if (f_deleted.exists()) { return; }
        if (!f_shadow.exists()) { return; }

        try {
            Files.copy(f_shadow.toPath(), f_deleted.toPath());
        } catch (FileAlreadyExistsException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        DiffUtils.writeDiffToYml(repoPath, branch, relPath, "", false,
                true, false, false);
        System.out.println(String.format("FileDeleted: %s", filePath));
    }

    public static void handleDirDelete(String repoPath, String branch, String relativeDirPath) {
        System.out.printf("Populating buffer for dir '%S' delete.", relativeDirPath);
        String shadowDirPath = String.format("%s/%s/%s/%s", SHADOW_REPO, repoPath.substring(1), branch, relativeDirPath);

        try {
            Stream<Path> files = Files.walk(Paths.get(shadowDirPath)).filter(Files::isRegularFile);
            files.forEach((path) -> {
                String filePath = path.toString();

                String relativeFilePath = filePath.split(String.format("%s/%s/", repoPath.substring(1), branch))[1];
                String shadowFilePath = String.format("%s/%s/%s/%s", SHADOW_REPO, repoPath.substring(1), branch, relativeFilePath);

                String deletedRepoFilePath = String.format("%s/%s/%s/%s", DELETED_REPO, repoPath.substring(1), branch, relativeFilePath);
                File deletedRepoFile = new File(deletedRepoFilePath);
                File shadowFile = new File(shadowFilePath);

                if (deletedRepoFile.exists() || !shadowFile.exists()) {
                    // If file already exists in .deleted directory or does not exist in shadow repo
                    // then no further action is needed.
                    return;
                }

                String deletedFileDirectoryPath = deletedRepoFile.getParent();
                File deletedFileDirectory = new File(deletedFileDirectoryPath);
                deletedFileDirectory.mkdirs();

                try {
                    Files.copy(shadowFile.toPath(), deletedRepoFile.toPath());
                } catch (IOException e) {
                    e.printStackTrace();

                }

                DiffUtils.writeDiffToYml(repoPath, branch, relativeFilePath, "", false,
                        true, false, false);
                System.out.printf("FileDeleted: %s%n", filePath);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void FileRenameHandler(VFileEvent event, String repoPath) throws IOException {
        String oldAbsPath = ((VFilePropertyChangeEvent) event).getOldPath();
        String newAbsPath = ((VFilePropertyChangeEvent) event).getNewPath();
        String s = String.format("%s/", repoPath);
        String[] newRelPathArr = newAbsPath.split(s);
        String newRelPath = newRelPathArr[newRelPathArr.length - 1];

        if (shouldSkipEvent(repoPath) || shouldIgnoreFile(newRelPath, repoPath)) { return; }

        String branch = Utils.GetGitBranch(repoPath);
        // See if it is for directory or a file
        File file = new File(newAbsPath);
        handleRename(repoPath, branch, oldAbsPath, newAbsPath, file.isFile());
    }

    public static void handleRename(String repoPath, String branch, String oldAbsPath,
                                    String newAbsPath, Boolean isFile) {
        String s = String.format("%s/", repoPath);
        String[] oldRelPathArr = oldAbsPath.split(s);
        String oldRelPath = oldRelPathArr[oldRelPathArr.length - 1];
        String[] newRelPathArr = newAbsPath.split(s);
        String newRelPath = newRelPathArr[newRelPathArr.length - 1];
        // Rename shadow path
        String oldShadowPath = String.format("%s/%s/%s/%s", SHADOW_REPO, repoPath.substring(1), branch, oldRelPath);
        String newShadowPath = String.format("%s/%s/%s/%s", SHADOW_REPO, repoPath.substring(1), branch, newRelPath);
        File oldShadow = new File(oldShadowPath);
        File newShadow = new File(newShadowPath);
        oldShadow.renameTo(newShadow);

        if (!isFile) {
            System.out.println(String.format("RepoRenamed: %s, %s", oldAbsPath, newAbsPath));
            // Create diff
            JSONObject diff = new JSONObject();
            diff.put("old_path", oldAbsPath);
            diff.put("new_path", newAbsPath);
            handleDirRename(oldAbsPath, newAbsPath, repoPath, branch);
            return;
        }
        System.out.println(String.format("FileRenamed: %s, %s", oldAbsPath, newAbsPath));
        // Create diff
        JSONObject diff = new JSONObject();
        diff.put("old_abs_path", oldAbsPath);
        diff.put("new_abs_path", newAbsPath);
        diff.put("old_rel_path", oldRelPath);
        diff.put("new_rel_path", newRelPath);
        DiffUtils.writeDiffToYml(repoPath, branch, newRelPath, diff.toJSONString(),
                false, false, true, false);
    }

    public static void handleDirRename(String oldPath, String newPath, String repoPath, String branch) {
        System.out.printf("Populating buffer for dir rename to %s", newPath);

        try {
            Stream<Path> files = Files.walk(Paths.get(newPath)).filter(Files::isRegularFile);
            files.forEach((path) -> {
                String newFilePath = path.toString();
                String oldFilePath = newFilePath.replace(newPath, oldPath);
                String newRelativePath = newFilePath.replace(String.format("%s/", repoPath), "");
                String oldRelativePath = oldFilePath.replace(String.format("%s/", repoPath), "");

                JSONObject diff = new JSONObject();
                diff.put("old_abs_path", oldFilePath);
                diff.put("new_abs_path", newFilePath);
                diff.put("old_rel_path", oldRelativePath);
                diff.put("new_rel_path", newRelativePath);
                DiffUtils.writeDiffToYml(
                        repoPath, branch, newRelativePath, diff.toJSONString(),
                        false, false, true, false
                );
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void ChangesHandler(DocumentEvent event, Project project) {
        Document document = event.getDocument();
        VirtualFile file = FileDocumentManager.getInstance().getFile(document);
        handleDocumentUpdates(file, project.getBasePath(), document.getText());
    }

    public static void handleDocumentUpdates(VirtualFile file, String repoPath, String currentText) {
        if (file == null) {
            return;
        }
        float time = System.currentTimeMillis();
        System.out.println(String.format("Event: %s", time));
        String filePath = file.getPath();

        String branch = Utils.GetGitBranch(repoPath);
        if (repoPath == null) { return; }
        String s = String.format("%s/", repoPath);
        String[] rel_path_arr = filePath.split(s);
        String relPath = rel_path_arr[rel_path_arr.length - 1];
        if (shouldSkipEvent(repoPath) || shouldIgnoreFile(relPath, repoPath)) { return; }

        // Get current git branch name
        ProcessBuilder processBuilder = new ProcessBuilder().directory(new File(repoPath));
        // Run a shell command
        processBuilder.command("/bin/bash", "-c", CURRENT_GIT_BRANCH_COMMAND);
        try {
            Process process = processBuilder.start();
            StringBuilder output = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
            int exitVal = process.waitFor();
            if (exitVal == 0) {
                branch = output.toString();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Skipping duplicate events for key press
        if (!filePath.contains(repoPath)) {
            return;
        }

        currentText = currentText.replace(MAGIC_STRING, "").trim();

        String shadowPath = String.format("%s/%s/%s/%s", SHADOW_REPO, repoPath.substring(1), branch, relPath);
        File f = new File(shadowPath);
        if (!f.exists()) {
            // TODO: Create shadow file?
            return;
        }

        // Read shadow file
        String shadowText = FileUtils.readLineByLineJava8(shadowPath);
        // If shadow text is same as current content, no need to compute diffs
        if (shadowText.equals(currentText)) {
            return;
        }
//         System.out.println(String.format("%s, %s, %s, %s", System.currentTimeMillis(), filePath, currentText, shadowText));
        // Update shadow file
        try {
            FileWriter myWriter = new FileWriter(shadowPath);
            myWriter.write(currentText);
            myWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        diff_match_patch dmp = new diff_match_patch();
        LinkedList<diff_match_patch.Patch> patches = dmp.patch_make(shadowText, currentText);

        // Create text representation of patches objects
        String diffs = dmp.patch_toText(patches);
        DiffUtils.writeDiffToYml(repoPath, branch, relPath, diffs, false,
                false, false, false);
    }
}
