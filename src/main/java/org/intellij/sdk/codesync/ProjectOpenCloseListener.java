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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
        String file_path = file.getPath();
        String currentText = document.getText();
        currentText = currentText.replace( MAGIC_STRING, "").trim();
        // TODO: Get on run time
        String branch = "plugins";
        String repoName = project.getName();
        String repoPath = project.getBasePath();
        String s = String.format("%s/", repoPath);
        String[] rel_path_arr = file_path.split(s);
        String relPath = rel_path_arr[rel_path_arr.length - 1];
        if (relPath.startsWith("/")) {
          relPath = relPath.replace("/", "");
        }
        String shadowPath = String.format("%s/%s/%s/%s", CODESYNC_ROOT, repoName, branch, relPath);
        File f = new File(shadowPath);
        if (!f.exists()) {
          // TODO: Create shadow file?
          return;
        }
        // Read shadow file
        String shadowText = ReadFileToString.readLineByLineJava8(shadowPath);
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
