package org.intellij.sdk.codesync.utils;

import org.intellij.sdk.codesync.CodeSyncLogger;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.intellij.sdk.codesync.Constants.*;

public class DiffUtils {
    public static void writeDiffToYml(
            String repoPath, String branchName, String relPath, String diff, Boolean isNewFile,
            Boolean isDeleted, Boolean isRename, Boolean isDirRename
    ) {
        writeDiffToYml(
                repoPath, branchName, relPath, diff, isNewFile, isDeleted, isRename, isDirRename,

                // Pass the current time as createdAt if it is not passed in the argument.
                CodeSyncDateUtils.getCurrentDatetime()
        );

    }

    public static void writeDiffToYml(
            String repoPath, String branchName, String relPath, String diff, Boolean isNewFile,
            Boolean isDeleted, Boolean isRename, Boolean isDirRename, String createdAt
    ) {
        // Create YAML dump
        Map<String, Object> data = new HashMap<>();
        data.put("repo_path", repoPath);
        data.put("branch", branchName);
        data.put("file_relative_path", relPath);
        if (!diff.isEmpty()) {
            data.put("diff", diff);
        }
        if (isNewFile) {
            data.put("is_new_file", true);
            data.put("added_at", CodeSyncDateUtils.getCurrentDatetime());
        }
        if (isDeleted) {
            data.put("is_deleted", true);
        }
        if (isRename) {
            data.put("is_rename", true);
        }
        if (isDirRename) {
            data.put("is_dir_rename", true);
        }
        data.put("source", DIFF_SOURCE);
        data.put("created_at", createdAt);
        data.put("commit_hash", GitUtils.getCommitHash(repoPath));

        writeToYML(data);
    }

    private static void writeToYML(Map<String, Object> data) {
        final DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);

        Yaml yaml = new Yaml(options);
        Path diffFilePath = Paths.get(DIFFS_REPO, String.format("%s.yml", System.currentTimeMillis()));
        try {
            // Write diff file
            FileWriter writer = new FileWriter(diffFilePath.toFile());
            yaml.dump(data, writer);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            CodeSyncLogger.error(String.format("Error writing to diff file. Error: %s", e.getMessage()));
            e.printStackTrace();
        }
    }
}
