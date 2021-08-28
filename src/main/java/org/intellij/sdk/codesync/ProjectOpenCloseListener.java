// Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.intellij.sdk.codesync;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import org.intellij.sdk.codesync.codeSyncSetup.CodeSyncSetup;
import org.intellij.sdk.codesync.messages.CodeSyncMessages;
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

    // TODO: remove after testing.
    ProgressManager.getInstance().run(new Task.Backgroundable(project, "Initializing repo"){
      public void run(@NotNull ProgressIndicator progressIndicator) {

        // Set the progress bar percentage and text
        progressIndicator.setFraction(0.10);
        progressIndicator.setText("Initializing repo");

        boolean result = CodeSyncMessages.showYesNoMessage("This is a test title.", "This is a test title.", project);
        System.out.printf("User selected: %s%n", result ? "Yes": "No");

        // Finished
        progressIndicator.setFraction(1.0);
        progressIndicator.setText("Repo initialized");

      }});


    CodeSyncSetup.setupCodeSyncRepoAsync(project, false);

    // Schedule buffer handler.
    HandleBuffer.scheduleBufferHandler();

    project.getMessageBus().connect().subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
      @Override
      public void after(@NotNull List<? extends VFileEvent> events) {
        String repoPath = project.getBasePath();

        // handle the events
        for (VFileEvent event : events) {
          String eventString = event.toString();

          if (eventString.startsWith(FILE_CREATE_EVENT) | eventString.startsWith(FILE_COPY_EVENT)) {
            FileCreateHandler(event.getPath(), repoPath);
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
