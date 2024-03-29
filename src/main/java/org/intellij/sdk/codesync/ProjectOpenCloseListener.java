// Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.intellij.sdk.codesync;
import kotlin.Pair;

import com.intellij.ide.startup.StartupManagerEx;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.serviceContainer.AlreadyDisposedException;
import kt.org.intellij.sdk.codesync.tasks.TaskExecutor;
import org.apache.commons.codec.digest.DigestUtils;
import org.intellij.sdk.codesync.alerts.ActivityAlerts;
import org.intellij.sdk.codesync.codeSyncSetup.CodeSyncSetup;
import org.intellij.sdk.codesync.database.Database;
import org.intellij.sdk.codesync.database.SQLiteConnection;
import org.intellij.sdk.codesync.exceptions.common.FileNotInModuleError;
import org.intellij.sdk.codesync.locks.CodeSyncLock;
import org.intellij.sdk.codesync.state.RepoStatus;
import org.intellij.sdk.codesync.state.StateUtils;
import org.intellij.sdk.codesync.utils.FileUtils;
import org.intellij.sdk.codesync.utils.ProjectUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.intellij.sdk.codesync.Constants.*;
import static org.intellij.sdk.codesync.Utils.*;
import static org.intellij.sdk.codesync.codeSyncSetup.CodeSyncSetup.createSystemDirectories;

/**
 * Listener to detect project open and close.
 */
public class ProjectOpenCloseListener implements ProjectManagerListener {
  private static final Map<String, Pair<Project, DocumentListener>> changeHandlers = new HashMap<>();

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
    // Create system directories required by the plugin.
    createSystemDirectories();
    Database.setupDbFilesAndTables(DATABASE_PATH);
    String repoDirPath = ProjectUtils.getRepoPath(project);
    CodeSyncLock codeSyncProjectLock = new CodeSyncLock(
        LockFileType.PROJECT_LOCK,
        // We are using one way hash here to avoid special character (e.g. : in path) issues on some Operating Systems
        repoDirPath != null ? DigestUtils.sha256Hex(repoDirPath): null
    );

    // This code is executed multiple times when a project window is opened,
    // causing the callbacks to be registered many times, this lock would prevent the
    // listeners from being registered multiple times.
    if (codeSyncProjectLock.isLockAcquired(project.getName())) {
      CodeSyncLogger.logConsoleMessage("Skipping the callback registration.");
      return;
    }

    // Acquire the lock now.
    // Keep a very low expiry to make sure, if user switches between projects then lock does not cause issues.
    Instant expiry = Instant.now().plus(5, ChronoUnit.SECONDS);
    codeSyncProjectLock.acquireLock(project.getName(), expiry);

    // Populate state
    StateUtils.populateState(project);

    PopulateBuffer.startPopulateBufferDaemon(project);

    // Schedule buffer handler.
    HandleBuffer.scheduleBufferHandler(project);

    // Start alerts daemon
    ActivityAlerts.startActivityAlertDaemon(project);

    // Start the task executor.
    TaskExecutor.INSTANCE.start();

    StartupManagerEx.getInstance(project).runWhenProjectIsInitialized(() -> {
      if (project.isDisposed()) return;
      VirtualFile[] contentRoots = ProjectUtils.getAllContentRoots(project);

      // Populate state for all the opened modules. Module is the term used for projects opened using "Attach" option
      // in the IDE open dialog box.
      for (VirtualFile contentRoot: contentRoots) {

        if(Utils.isIndividualFileOpen(contentRoot.getPath())){
          continue;
        }

        String repoPath = FileUtils.normalizeFilePath(contentRoot.getPath());
        String repoName = contentRoot.getName();
        RepoStatus repoStatus = StateUtils.getRepoStatus(repoPath);
        if (repoStatus == RepoStatus.DISCONNECTED) {
          CodeSyncSetup.reconnectRepoAsync(project, repoPath, repoName);
        } else {
          CodeSyncSetup.setupCodeSyncRepoAsync(project, repoPath, repoName, false, false);
        }
      }

      for (Pair<Project, DocumentListener> pair: changeHandlers.values()) {
        if (pair.getFirst().isDisposed()) {
          this.disposeProjectListeners(pair.getFirst());
        }
      }
    });

    project.getMessageBus().connect().subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
      @Override
      public void after(@NotNull List<? extends VFileEvent> events) {
        String repoPath;

        // Abort if account has been deactivated.
        if (StateUtils.getGlobalState().isAccountDeactivated) {
          return;
        }

        for (VFileEvent event : events) {
          VirtualFile virtualFile = event.getFile();

          if (virtualFile == null) {
            // Ignore events not belonging to current project.
            CodeSyncLogger.logConsoleMessage("Ignoring event because event does not know which file was affected.");
            return;
          }

          try {
            repoPath = ProjectUtils.getRepoPath(virtualFile, project);
          } catch (FileNotInModuleError error) {
            // Ignore events not belonging to current project.
            CodeSyncLogger.logConsoleMessage("Ignoring event because event does not belong to any of the module files.");
            return;
          }

          String eventString = event.toString();
          ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();

          if (!projectFileIndex.isInContent(virtualFile)) {
            // Ignore events not belonging to current project.
            CodeSyncLogger.logConsoleMessage("Ignoring event from other project. ");
            CodeSyncLogger.logConsoleMessage(eventString);
            return;
          }

          // handle the events
          if (eventString.startsWith(FILE_CREATE_EVENT) | eventString.startsWith(FILE_COPY_EVENT)) {
            String filePath = FileUtils.normalizeFilePath(event.getPath());

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
        }
      }
    });

    DocumentListener changesHandler = new DocumentListener() {
      @Override
      public void documentChanged(@NotNull DocumentEvent event) {
        if (!project.isDisposed()){

          // Abort if account is has been deactivated.
          if (StateUtils.getGlobalState().isAccountDeactivated) {
            return;
          }

          ChangesHandler(event, project);
        }
      }
    };
    changeHandlers.put(project.getBasePath(), new Pair<>(project, changesHandler));
    EditorFactory.getInstance().getEventMulticaster().addDocumentListener(changesHandler, Disposer.newDisposable());
  }

  /**
   * Invoked on project close.
   *
   * @param project closing project
   */
  @Override
  public void projectClosed(@NotNull Project project) {
    disposeProjectListeners(project);
  }

  public void disposeProjectListeners(Project project) {

    try{
      SQLiteConnection.getInstance().disconnect();
    } catch (SQLException e){
      CodeSyncLogger.error("Error while disconnecting database: " + e.getMessage());
    }

    // Release all the locks acquired by this project.
    CodeSyncLock.releaseAllLocks(LockFileType.PROJECT_LOCK, project.getName());
    CodeSyncLock.releaseAllLocks(LockFileType.HANDLE_BUFFER_LOCK, project.getName());
    CodeSyncLock.releaseAllLocks(LockFileType.POPULATE_BUFFER_LOCK, project.getName());

    DocumentListener changesHandler = changeHandlers.get(project.getBasePath()).getSecond();
    if (changesHandler != null) {
      EditorFactory.getInstance().getEventMulticaster().removeDocumentListener(changesHandler);
    }

    try {
      // remove listeners to file updates.
      project.getMessageBus().connect().disconnect();
    } catch (AlreadyDisposedException ignored){}

  }
}
