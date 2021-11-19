// Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.intellij.sdk.codesync;

import com.intellij.ide.startup.StartupManagerEx;
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
import org.intellij.sdk.codesync.codeSyncSetup.CodeSyncSetup;
import org.intellij.sdk.codesync.state.StateUtils;
import org.intellij.sdk.codesync.utils.CommonUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

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
    // TODO: Might not need this => Ensure this isn't part of testing
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      return;
    }

    StartupManagerEx.getInstance(project).runWhenProjectIsInitialized(() -> {
      if (project.isDisposed()) return;

      CodeSyncSetup.setupCodeSyncRepoAsync(project, false);
    });

    PopulateBuffer.startPopulateBufferDaemon();

    // Schedule buffer handler.
    HandleBuffer.scheduleBufferHandler();

    // Populate state
    StateUtils.populateState(project.getBasePath());

    project.getMessageBus().connect().subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
      @Override
      public void after(@NotNull List<? extends VFileEvent> events) {
        String repoPath = project.getBasePath();

        if (CommonUtils.isWindows()){
          // For some reason people at intellij thought it would be a good idea to confuse users by using
          // forward slashes in paths instead of windows path separator.
          repoPath = repoPath.replaceAll("/", "\\\\");
        }

        // handle the events
        for (VFileEvent event : events) {
          String eventString = event.toString();

          if (eventString.startsWith(FILE_CREATE_EVENT) | eventString.startsWith(FILE_COPY_EVENT)) {
            String filePath = event.getPath();

            if (CommonUtils.isWindows()) {
              filePath = filePath.replaceAll("/", "\\\\");
            }

            FileCreateHandler(filePath, repoPath);
            return;
          }
          if (eventString.startsWith(FILE_DELETE_EVENT)) {
            FileDeleteHandler(event, repoPath);
            return;
          }
          if (eventString.startsWith(FILE_RENAME_EVENT)) {
            try {
              FileRenameHandler(event, repoPath);
            } catch (IOException e) {
              e.printStackTrace();
            }
            return;
          }
          System.out.println(eventString);
        }
      }
    });

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
