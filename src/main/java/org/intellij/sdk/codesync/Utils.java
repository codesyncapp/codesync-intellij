package org.intellij.sdk.codesync;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent;
import name.fraser.neil.plaintext.diff_match_patch;
import org.intellij.sdk.codesync.exceptions.InvalidConfigFileError;
import org.intellij.sdk.codesync.files.ConfigFile;
import org.intellij.sdk.codesync.repoManagers.DeletedRepoManager;
import org.intellij.sdk.codesync.repoManagers.OriginalsRepoManager;
import org.intellij.sdk.codesync.repoManagers.ShadowRepoManager;
import org.intellij.sdk.codesync.utils.DiffUtils;
import org.intellij.sdk.codesync.utils.FileUtils;
import org.json.simple.JSONObject;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.intellij.sdk.codesync.Constants.*;

public class Utils {
    private static String OS = System.getProperty("os.name").toLowerCase();

    public static boolean isWindows() {
        return OS.contains("win");
    }

    public static boolean isMac() {
        return OS.contains("mac");
    }

    public static boolean isUnix() {
        return (OS.contains("nix") || OS.contains("nux") || OS.contains("aix"));
    }

    public static boolean isSolaris() {
        return OS.contains("sunos");
    }

    public static Boolean shouldSkipEvent(String repoPath) {
        // Skip if config does not exist
        File config = new File(CONFIG_PATH);
        if (!config.exists()) {
            return true;
        }
        try {
            ConfigFile configFile = new ConfigFile(CONFIG_PATH);
            return configFile.hasRepo(repoPath);
        } catch (InvalidConfigFileError e) {
            return true;
        }
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

        Path syncIgnorePath = Paths.get(repoPath, ".syncignore");
        if (!syncIgnorePath.toFile().exists()) {
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
                if (pattern.endsWith(String.valueOf(File.separatorChar))) {
                    pattern = pattern.substring(0, pattern.length() - 1);
                }
                excludePaths.add(pattern);
                continue;
            }
            shouldIgnoreByMatch = match(relPath, pattern) || shouldIgnoreByMatch;
            String dirPattern = pattern.replace(String.valueOf(File.separatorChar), "");
            if (dirPattern.endsWith("*")) {
                dirPattern = dirPattern.substring(0, dirPattern.length() - 1);
            }
            File file = Paths.get(repoPath, dirPattern).toFile();
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
        if (isWindows()) {
            processBuilder.command("cmd", "/C", CURRENT_GIT_BRANCH_COMMAND);
        } else {
            // Run a shell command
            processBuilder.command("/bin/bash", "-c", CURRENT_GIT_BRANCH_COMMAND);
        }

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
        if (eventFile.isDirectory()) {
            return;
        }

        String relativeFilePath = filePath
                .replace(repoPath, "")
                .replaceFirst(Pattern.quote(String.valueOf(File.separatorChar)), "");

        if (shouldSkipEvent(repoPath) || shouldIgnoreFile(relativeFilePath, repoPath)) { return; }
        String branchName = Utils.GetGitBranch(repoPath);

        OriginalsRepoManager originalsRepoManager = new OriginalsRepoManager(repoPath, branchName);
        originalsRepoManager.copyFiles(new String[] {filePath});

        ShadowRepoManager shadowRepoManager = new ShadowRepoManager(repoPath, branchName);
        shadowRepoManager.copyFiles(new String[] {filePath});

        DiffUtils.writeDiffToYml(repoPath, branchName, relativeFilePath, "", true,
                false, false, false
        );
        System.out.println(String.format("FileCreated: %s", filePath));
    }

    public static void FileDeleteHandler(VFileEvent event, String repoPath) {
        String filePath = Objects.requireNonNull(event.getFile()).getPath();

        if (Utils.isWindows()){
            filePath = filePath.replaceAll("/", "\\\\");
        }

        String relativeFilePath = filePath
                .replace(repoPath, "")
                .replaceFirst(Pattern.quote(String.valueOf(File.separatorChar)), "");

        if (shouldSkipEvent(repoPath) || shouldIgnoreFile(relativeFilePath, repoPath)) { return; }
        String branchName = Utils.GetGitBranch(repoPath);

        if (event.getFile().isDirectory()) {
            handleDirDelete(repoPath, branchName, relativeFilePath);
            return;
        }

        DeletedRepoManager deletedRepoManager = new DeletedRepoManager(repoPath, branchName);
        ShadowRepoManager shadowRepoManager = new ShadowRepoManager(repoPath, branchName);

        deletedRepoManager.copyFiles(
                new String[]{shadowRepoManager.getFilePath(relativeFilePath).toString()},
                shadowRepoManager.getBaseRepoBranchDir()
        );

        DiffUtils.writeDiffToYml(repoPath, branchName, relativeFilePath, "", false,
                true, false, false);
        System.out.println(String.format("FileDeleted: %s", filePath));
    }

    public static void handleDirDelete(String repoPath, String branch, String relativeDirPath) {
        System.out.printf("Populating buffer for dir '%S' delete.", relativeDirPath);
        DeletedRepoManager deletedRepoManager = new DeletedRepoManager(repoPath, branch);
        ShadowRepoManager shadowRepoManager = new ShadowRepoManager(repoPath, branch);
        Path shadowDirectoryPath = shadowRepoManager.getFilePath(relativeDirPath);

        try {
            Stream<Path> files = Files.walk(shadowDirectoryPath).filter(Files::isRegularFile);
            files.forEach((path) -> {
                String filePath = path.toString();
                String relativeFilePath = filePath
                        .replace(repoPath, "")
                        .replaceFirst(Pattern.quote(String.valueOf(File.separatorChar)), "");


                Path shadowFilePath = shadowRepoManager.getFilePath(relativeFilePath);
                Path deletedFilePath = deletedRepoManager.getFilePath(relativeFilePath);

                File deletedRepoFile = deletedFilePath.toFile();
                File shadowFile = shadowFilePath.toFile();

                if (deletedRepoFile.exists() || !shadowFile.exists()) {
                    // If file already exists in .deleted directory or does not exist in shadow repo
                    // then no further action is needed.
                    return;
                }

                deletedRepoManager.copyFiles(
                        new String[]{shadowRepoManager.getFilePath(relativeFilePath).toString()},
                        shadowRepoManager.getBaseRepoBranchDir()
                );

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

        if (Utils.isWindows()){
            oldAbsPath = oldAbsPath.replaceAll("/", "\\\\");
            newAbsPath = newAbsPath.replaceAll("/", "\\\\");
        }

        String newRelativeFilePath = newAbsPath
                .replace(repoPath, "")
                .replaceFirst(Pattern.quote(String.valueOf(File.separatorChar)), "");

        if (shouldSkipEvent(repoPath) || shouldIgnoreFile(newRelativeFilePath, repoPath)) { return; }

        String branch = Utils.GetGitBranch(repoPath);
        // See if it is for directory or a file
        File file = new File(newAbsPath);
        handleRename(repoPath, branch, oldAbsPath, newAbsPath, file.isFile());
    }

    public static void handleRename(
            String repoPath, String branch, String oldAbsPath, String newAbsPath, Boolean isFile
    ) {
        String newRelativeFilePath = newAbsPath
                .replace(repoPath, "")
                .replaceFirst(Pattern.quote(String.valueOf(File.separatorChar)), "");

        String oldRelativeFilePath = oldAbsPath
                .replace(repoPath, "")
                .replaceFirst(Pattern.quote(String.valueOf(File.separatorChar)), "");

        ShadowRepoManager shadowRepoManager = new ShadowRepoManager(repoPath, branch);

        // Rename shadow path
        shadowRepoManager.renameFile(oldRelativeFilePath, newRelativeFilePath);

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
        diff.put("old_rel_path", oldRelativeFilePath);
        diff.put("new_rel_path", newRelativeFilePath);
        DiffUtils.writeDiffToYml(repoPath, branch, newRelativeFilePath, diff.toJSONString(),
                false, false, true, false);
    }

    public static void handleDirRename(String oldPath, String newPath, String repoPath, String branch) {
        System.out.printf("Populating buffer for dir rename to %s", newPath);

        try {
            Stream<Path> files = Files.walk(Paths.get(newPath)).filter(Files::isRegularFile);
            files.forEach((path) -> {
                String newFilePath = path.toString();
                String oldFilePath = newFilePath.replace(newPath, oldPath);

                String newRelativePath = newFilePath
                        .replace(repoPath, "")
                        .replaceFirst(Pattern.quote(String.valueOf(File.separatorChar)), "");

                String oldRelativePath = oldFilePath
                        .replace(repoPath, "")
                        .replaceFirst(Pattern.quote(String.valueOf(File.separatorChar)), "");

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

        String repoPath = project.getBasePath();
        if (Utils.isWindows()){
            // For some reason people at intellij thought it would be a good idea to confuse users by using
            // forward slashes in paths instead of windows path separator.
            repoPath = repoPath.replaceAll("/", "\\\\");
        }

        handleDocumentUpdates(file, repoPath, document.getText());
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

        String relativeFilePath = filePath
                .replace(repoPath, "")
                .replaceFirst(Pattern.quote(String.valueOf(File.separatorChar)), "");

        if (shouldSkipEvent(repoPath) || shouldIgnoreFile(relativeFilePath, repoPath)) { return; }

        // Skipping duplicate events for key press
        if (!filePath.contains(repoPath)) {
            return;
        }

        currentText = currentText.replace(MAGIC_STRING, "").trim();

        ShadowRepoManager shadowRepoManager = new ShadowRepoManager(repoPath, branch);
        Path shadowPath = shadowRepoManager.getFilePath(relativeFilePath);
        if (!shadowPath.toFile().exists()) {
            // TODO: Create shadow file?
            return;
        }

        // Read shadow file
        String shadowText = FileUtils.readLineByLineJava8(shadowPath);
        // If shadow text is same as current content, no need to compute diffs
        if (shadowText.equals(currentText)) {
            return;
        }
        // Update shadow file
        try {
            FileWriter myWriter = new FileWriter(shadowPath.toFile());
            myWriter.write(currentText);
            myWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        diff_match_patch dmp = new diff_match_patch();
        LinkedList<diff_match_patch.Patch> patches = dmp.patch_make(shadowText, currentText);

        // Create text representation of patches objects
        String diffs = dmp.patch_toText(patches);
        DiffUtils.writeDiffToYml(repoPath, branch, relativeFilePath, diffs, false,
                false, false, false);
    }
}
