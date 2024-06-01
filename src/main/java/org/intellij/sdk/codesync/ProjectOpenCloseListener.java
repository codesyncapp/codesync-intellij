// Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.intellij.sdk.codesync;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.serviceContainer.AlreadyDisposedException;
import org.intellij.sdk.codesync.database.SQLiteConnection;
import org.intellij.sdk.codesync.locks.CodeSyncLock;
import org.intellij.sdk.codesync.utils.CommonUtils;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

import static org.intellij.sdk.codesync.Constants.*;

/**
 * Listener to detect project open and close.
 */
public class ProjectOpenCloseListener implements ProjectManagerListener {

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
    CodeSyncLogger.info("Running project close listener for project: " + project.getName());
    try{
      SQLiteConnection.getInstance().disconnect();
    } catch (SQLException e){
      CodeSyncLogger.error("Error while disconnecting database: " + CommonUtils.getStackTrace(e));
    }

    // Release all the locks acquired by this project.
    CodeSyncLock.releaseAllLocks(LockFileType.PROJECT_LOCK, project.getName());
    CodeSyncLock.releaseAllLocks(LockFileType.HANDLE_BUFFER_LOCK, project.getName());
    CodeSyncLock.releaseAllLocks(LockFileType.POPULATE_BUFFER_LOCK, project.getName());

    try {
      // remove listeners to file updates.
      project.getMessageBus().connect().disconnect();
    } catch (AlreadyDisposedException ignored){}

  }
}
