// Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.intellij.sdk.codesync;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.intellij.sdk.codesync.Constants.*;
import static org.intellij.sdk.codesync.Utils.*;

/**
 * Listener to detect project open and close.
 */

public class ProjectOpenCloseListener implements ProjectManagerListener {

  /**
   * Invoked on project open.
   *
   * @param project opening project
   */

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
          if (event.toString().startsWith(FILE_CREATE_EVENT)) {
            FileCreateHandler(event, repoName, repoPath);
            return;
          }
          if (event.toString().startsWith(FILE_DELETE_EVENT)) {
            FileDeleteHandler(event, repoName, repoPath);
            return;
          }
          if (event.toString().startsWith(FILE_RENAME_EVENT)) {
            FileRenameHandler(event, repoName, repoPath);
            return;
          }

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
        ChangesHandler(event, project);
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
