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
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import name.fraser.neil.plaintext.diff_match_patch;
import name.fraser.neil.plaintext.diff_match_patch.Patch;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
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
  String ORIGINALS_REPO = String.format("%s/.originals", CODESYNC_ROOT);
  String CONFIG_PATH = String.format("%s/config.yml", CODESYNC_ROOT);

  String MAGIC_STRING = "IntellijIdeaRulezzz";
  String CURRENT_GIT_BRANCH_COMMAND = "git rev-parse --abbrev-ref HEAD";
  String VFS_CREATE_EVENT = "VfsEvent[create file";

  @Override
  public void projectOpened(@NotNull Project project) {
    // Ensure this isn't part of testing
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      return;
    }

    project.getMessageBus().connect().subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
      @Override
      public void after(@NotNull List<? extends VFileEvent> events) {
        String repoName = project.getName();
        String repoPath = project.getBasePath();

        // handle the events
        for (VFileEvent event : events) {
          if (!event.toString().startsWith(VFS_CREATE_EVENT)) {
            return;
          }
          String filePath = event.getFile().getPath();
          String s = String.format("%s/", repoPath);
          String[] rel_path_arr = filePath.split(s);
          String relPath = rel_path_arr[rel_path_arr.length - 1];
          String branch = Utils.GetGitBranch(repoPath);

          String destOriginals = String.format("%s/%s/%s/%s", ORIGINALS_REPO, repoName, branch, relPath);
          String[] destOriginalsPathSplit = destOriginals.split("/");
          String[] newArray = Arrays.copyOfRange(destOriginalsPathSplit, 0, destOriginalsPathSplit.length-1);
          String destOriginalsBasePath = String.join("/", newArray);

          String destShadow = String.format("%s/%s/%s/%s", CODESYNC_ROOT, repoName, branch, relPath);
          String[] destShadowPathSplit = destShadow.split("/");
          newArray = Arrays.copyOfRange(destShadowPathSplit, 0, destShadowPathSplit.length-1);
          String destShadowBasePath = String.join("/", newArray);

          File f_originals_base = new File(destOriginalsBasePath);
          f_originals_base.mkdirs();
          File f_shadow_base = new File(destShadowBasePath);
          f_shadow_base.mkdirs();

          File file = new File(filePath);
          File f_originals = new File(destOriginals);
          File f_shadow = new File(destOriginals);

          try {
            Files.copy(file.toPath(), f_originals.toPath());
          } catch (FileAlreadyExistsException e) {

          } catch (IOException e) {
            e.printStackTrace();
          }

          try {
            Files.copy(file.toPath(), f_shadow.toPath());
          } catch (FileAlreadyExistsException e) {

          } catch (IOException e) {
            e.printStackTrace();
          }
          Utils.WriteDiffToYml(repoName, branch, relPath, "", true);
        }
      }
    });

    String repoName = project.getName();
    String repoPath = project.getBasePath();

    // Skip if config does not exist
    File config = new File(CONFIG_PATH);
    if (!config.exists()) {
      return;
    }
    // Ensure repo is synced
    Yaml yaml = new Yaml();
    InputStream inputStream;
    try {
      inputStream = new FileInputStream(CONFIG_PATH);
    } catch (FileNotFoundException e) {
      return;
    }
    Map<String, Map<String, Map<String, Object>>> obj = yaml.load(inputStream);
    if (!obj.get("repos").keySet().contains(repoName) || !obj.get("repos").get(repoName).get("path").equals(repoPath)) {
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
        String branch = Utils.GetGitBranch(repoPath);
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
        Utils.WriteDiffToYml(repoName, branch, relPath, diffs, false);
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
