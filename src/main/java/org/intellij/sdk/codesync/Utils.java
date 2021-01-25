package org.intellij.sdk.codesync;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

public class Utils {

    public static String GetGitBranch(String repoPath) {
        String branch = "default";
        String CURRENT_GIT_BRANCH_COMMAND = "git rev-parse --abbrev-ref HEAD";

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

    public static void WriteDiffToYml(String repoName, String branch, String relPath, String diffs, Boolean isNewFile) {
        String DIFF_SOURCE = "intellij";
        String CODESYNC_ROOT = "/usr/local/bin/.codesync";
        String DIFFS_REPO = String.format("%s/.diffs", CODESYNC_ROOT);

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
            data.put("is_new_file", "true");
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
}
