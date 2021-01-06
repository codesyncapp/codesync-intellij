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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;

import name.fraser.neil.plaintext.diff_match_patch;
import name.fraser.neil.plaintext.diff_match_patch.Patch;

//import name.fraser.neil.plaintext.StandardBreakScorer;
//import name.fraser.neil.plaintext.diff_match_patch;
import org.jetbrains.annotations.NotNull;

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
        currentText = currentText.replace("IntellijIdeaRulezzz ", "").trim();

//        const editor = vscode.window.activeTextEditor;
        // TODO: Get on run time
        String branch = "plugins";
        String repoName = project.getName();
        String repoPath = project.getBasePath();
        String s = String.format("%s/", repoPath);
        String[] rel_path_arr = file_path.split(s);
        String relPath = rel_path_arr[rel_path_arr.length - 1];
        String shadowPath = String.format("%s/%s/%s/%s", CODESYNC_ROOT, repoName, branch, relPath);
        File f = new File(shadowPath);
        if (!f.exists()) {
          // TODO: Create shadow file?
          return;
        }
        // Read shadow file
        Path shadowFilePath = Path.of(shadowPath);
        String shadowText = "";
        try {
          shadowText = Files.readString(shadowFilePath);
        } catch (IOException e) {
          e.printStackTrace();
        }
        // Update shadow file
        try {
          Files.writeString(shadowFilePath, currentText);
        } catch (IOException e) {
          e.printStackTrace();
        }

//        diff_match_patch dmp = new diff_match_patch(new StandardBreakScorer());
        diff_match_patch dmp = new diff_match_patch();
        LinkedList<Patch> patches = dmp.patch_make("Hello World.", "Goodbye World.");
        //  Create text representation of patches objects
        String diffs = dmp.patch_toText(patches);
        System.out.println(diffs);
//        System.out.println(currentText);
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
