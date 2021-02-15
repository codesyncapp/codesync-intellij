package org.intellij.sdk.codesync;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent;
import name.fraser.neil.plaintext.diff_match_patch;
import org.json.simple.JSONObject;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;


import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.intellij.sdk.codesync.Constants.*;

public class Utils {

    public static Boolean shouldSkipEvent(String repoName, String repoPath) {
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
        return !obj.get("repos").keySet().contains(repoName) || !obj.get("repos").get(repoName).get("path").equals(repoPath);
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
        String syncIgnore = ReadFileToString.readLineByLineJava8(syncIgnorePath);
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

    public static void WriteDiffToYml(String repoName, String branch, String relPath, String diffs,
                                      Boolean isNewFile, Boolean isDeleted, Boolean isRename) {
        String DIFF_SOURCE = "intellij";

        final Date currentTime = new Date();
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

        // Create YAML dump
        Map<String, String > data = new HashMap<>();
        data.put("repo", repoName);
        data.put("branch", branch);
        data.put("file_relative_path", relPath);
        if (!diffs.isEmpty()) {
            data.put("diff", diffs);
        }
        if (isNewFile) {
            data.put("is_new_file", "1");
        }
        if (isDeleted) {
            data.put("is_deleted", "1");
        }
        if (isRename) {
            data.put("is_rename", "1");
        }
        data.put("source", DIFF_SOURCE);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String created_at = sdf.format(currentTime);
        data.put("created_at", created_at);

        final DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);

        Yaml yaml = new Yaml(options);
        String diffFileName = String.format("%s/%s.yml", DIFFS_REPO, System.currentTimeMillis());
        // Write diff file
        FileWriter writer = null;
        try {
            writer = new FileWriter(diffFileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        yaml.dump(data, writer);
    }

    public static void FileCreateHandler(VFileEvent event, String repoName, String repoPath) {
        String filePath = event.getFile().getPath();
        String s = String.format("%s/", repoPath);
        String[] rel_path_arr = filePath.split(s);
        String relPath = rel_path_arr[rel_path_arr.length - 1];
        if (shouldSkipEvent(repoName, repoPath) || shouldIgnoreFile(relPath, repoPath)) { return; }
        String branch = Utils.GetGitBranch(repoPath);

        String destOriginals = String.format("%s/%s/%s/%s", ORIGINALS_REPO, repoName, branch, relPath);
        String[] destOriginalsPathSplit = destOriginals.split("/");
        String[] newArray = Arrays.copyOfRange(destOriginalsPathSplit, 0, destOriginalsPathSplit.length-1);
        String destOriginalsBasePath = String.join("/", newArray);

        String destShadow = String.format("%s/%s/%s/%s", SHADOW_REPO, repoName, branch, relPath);
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

        if (f_originals.exists()) { return; }

        try {
            Files.copy(file.toPath(), f_originals.toPath());
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
        Utils.WriteDiffToYml(repoName, branch, relPath, "", true, false, false);
        System.out.println(String.format("FileCreated: %s", filePath));

    }

    public static void FileDeleteHandler(VFileEvent event, String repoName, String repoPath) {
        String filePath = event.getFile().getPath();
        String s = String.format("%s/", repoPath);
        String[] rel_path_arr = filePath.split(s);
        String relPath = rel_path_arr[rel_path_arr.length - 1];
        if (shouldSkipEvent(repoName, repoPath) || shouldIgnoreFile(relPath, repoPath)) { return; }
        String branch = Utils.GetGitBranch(repoPath);
        Utils.WriteDiffToYml(repoName, branch, relPath, "", false, true, false);
        System.out.println(String.format("FileDeleted: %s", filePath));
    }

    public static void FileRenameHandler(VFileEvent event, String repoName, String repoPath) {
        String oldAbsPath = ((VFilePropertyChangeEvent) event).getOldPath();
        String newAbsPath = ((VFilePropertyChangeEvent) event).getNewPath();
        String s = String.format("%s/", repoPath);
        String[] oldRelPathArr = oldAbsPath.split(s);
        String oldRelPath = oldRelPathArr[oldRelPathArr.length - 1];
        String[] newRelPathArr = newAbsPath.split(s);
        String newRelPath = newRelPathArr[newRelPathArr.length - 1];

        if (shouldSkipEvent(repoName, repoPath) || shouldIgnoreFile(newRelPath, repoPath)) { return; }

        String branch = Utils.GetGitBranch(repoPath);
        // Copy file to shadow reop
        String shadowPath= String.format("%s/%s/%s/%s", SHADOW_REPO, repoName, branch, newRelPath);
        String[] shadowPathSplit = shadowPath.split("/");
        String[] newArray = Arrays.copyOfRange(shadowPathSplit, 0, shadowPathSplit.length-1);
        String shadowBasePath = String.join("/", newArray);

        File f_shadow_base = new File(shadowBasePath);
        f_shadow_base.mkdirs();

        File file = new File(newAbsPath);
        File f_shadow = new File(shadowPath);

        try {
            Files.copy(file.toPath(), f_shadow.toPath());
        } catch (FileAlreadyExistsException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Create diff
        JSONObject diff = new JSONObject();
        diff.put("old_abs_path", oldAbsPath);
        diff.put("new_abs_path", newAbsPath);
        diff.put("old_rel_path", oldRelPath);
        diff.put("new_rel_path", newRelPath);
        Utils.WriteDiffToYml(repoName, branch, newRelPath, diff.toJSONString(),
                false, false, true);
    }

    public static void ChangesHandler(DocumentEvent event, Project project) {
        Document document = event.getDocument();
        VirtualFile file = FileDocumentManager.getInstance().getFile(document);
        if (file == null) {
            return;
        }
        float time = System.currentTimeMillis();
        System.out.println(String.format("Event: %s", time));
        String filePath = file.getPath();

        String repoName = project.getName();
        String repoPath = project.getBasePath();
        String branch = Utils.GetGitBranch(repoPath);
        if (repoPath == null) { return; }
        String s = String.format("%s/", repoPath);
        String[] rel_path_arr = filePath.split(s);
        String relPath = rel_path_arr[rel_path_arr.length - 1];
        if (shouldSkipEvent(repoName, repoPath) || shouldIgnoreFile(relPath, repoPath)) { return; }

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

        String currentText = document.getText();
        currentText = currentText.replace(MAGIC_STRING, "").trim();

        String shadowPath = String.format("%s/%s/%s/%s", SHADOW_REPO, repoName, branch, relPath);
        File f = new File(shadowPath);
        if (!f.exists()) {
            // TODO: Create shadow file?
            return;
        }

        // Read shadow file
        String shadowText = ReadFileToString.readLineByLineJava8(shadowPath);
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
//        System.out.println(diffs);
        Utils.WriteDiffToYml(repoName, branch, relPath, diffs, false, false, false);
    }

}
