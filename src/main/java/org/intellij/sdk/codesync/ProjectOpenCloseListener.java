// Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.intellij.sdk.codesync;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import name.fraser.neil.plaintext.diff_match_patch;
import name.fraser.neil.plaintext.diff_match_patch.Patch;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Listener to detect project open and close.
 */
public class ProjectOpenCloseListener implements ProjectManagerListener {

  /**
   * Invoked on project open.
   *
   * @param project opening project
   */

  String CODESYNC_ROOT = "/usr/local/bin/.codesync";
  String DIFFS_REPO = String.format("%s/.diffs", CODESYNC_ROOT);
  String MAGIC_STRING = "IntellijIdeaRulezzz";
  String CURRENT_GIT_BRANCH_COMMAND = "git rev-parse --abbrev-ref HEAD";
  @Override
  public void projectOpened(@NotNull Project project) {
    // Ensure this isn't part of testing
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      return;
    }

    EditorFactory.getInstance().getEventMulticaster().addDocumentListener(new DocumentListener() {
      @Override
      public void documentChanged(@NotNull DocumentEvent event) {
        Document document = event.getDocument();
        VirtualFile file = FileDocumentManager.getInstance().getFile(document);
        String filePath = file.getPath();
        String repoName = project.getName();
        String repoPath = project.getBasePath();
        String branch = "default";
        if (repoPath == null) { return; }
        float time = System.currentTimeMillis();
        System.out.println(String.format("Event: %s", time));
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

        String s = String.format("%s/", repoPath);
        String[] rel_path_arr = filePath.split(s);
        String relPath = rel_path_arr[rel_path_arr.length - 1];


        String shadowPath = String.format("%s/%s/%s/%s", CODESYNC_ROOT, repoName, branch, relPath);
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
        LinkedList<Patch> patches = dmp.patch_make(shadowText, currentText);

        // Create text representation of patches objects
        String diffs = dmp.patch_toText(patches);

//        System.out.println(diffs);
        // Create YAML dump
        Map<String, String> data = new HashMap<String, String>();
        data.put("repo", repoName);
        data.put("branch", branch);
        data.put("file", relPath);
        data.put("diff", diffs);
        final Date currentTime = new Date();
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String created_at = sdf.format(currentTime);
        data.put("created_at", created_at);
        Yaml yaml = new Yaml();
        String diffFileName = String.format("%s/%s.yml", DIFFS_REPO, System.currentTimeMillis());
        // Write diff file
        FileWriter writer = null;
        try {
          writer = new FileWriter(diffFileName);
        } catch (IOException e) {
          e.printStackTrace();
        }
        yaml.dump(data, writer);

//        // TODO: Look into following events for bew file/deleted file
//        VirtualFileManager.getInstance().addVirtualFileListener(new VirtualFileAdapter() {
//          @Override
//          public void fileCreated(VirtualFileEvent event) {
//            System.out.println(event);
//          }
//
//          @Override
//          public void fileDeleted(VirtualFileEvent event) {
//            System.out.println(event);
//          }
//        });
      }
    }, Disposer.newDisposable());
  }

  /**
   * Invoked on project close.
   *
   * @param project closing project
   */
  @Override
  public void projectClosed(@NotNull Project project) {

  }

}
