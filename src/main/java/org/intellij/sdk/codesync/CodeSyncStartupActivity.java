package org.intellij.sdk.codesync;

import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import kt.org.intellij.sdk.codesync.tasks.TaskExecutor;
import org.intellij.sdk.codesync.alerts.ActivityAlerts;
import org.intellij.sdk.codesync.codeSyncSetup.CodeSyncSetup;
import org.intellij.sdk.codesync.database.migrations.MigrateRepo;
import org.intellij.sdk.codesync.database.migrations.MigrationManager;
import org.intellij.sdk.codesync.exceptions.common.FileNotInModuleError;
import org.intellij.sdk.codesync.state.RepoStatus;
import org.intellij.sdk.codesync.state.StateUtils;
import org.intellij.sdk.codesync.utils.CommonUtils;
import org.intellij.sdk.codesync.utils.FileUtils;
import org.intellij.sdk.codesync.utils.ProjectUtils;
import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.ui.Messages;

import java.io.IOException;
import java.util.List;

import static org.intellij.sdk.codesync.Constants.*;
import static org.intellij.sdk.codesync.Constants.FILE_RENAME_EVENT;
import static org.intellij.sdk.codesync.Utils.*;
import static org.intellij.sdk.codesync.Utils.ChangesHandler;
import static org.intellij.sdk.codesync.codeSyncSetup.CodeSyncSetup.createSystemDirectories;
import org.intellij.sdk.codesync.state.StateUtils;
import org.intellij.sdk.codesync.state.PluginState;

public class CodeSyncStartupActivity implements StartupActivity {

    @Override
    public void runActivity(@NotNull Project project) {
        // Create system directories required by the plugin.
        createSystemDirectories();
        String repoDirPath = ProjectUtils.getRepoPath(project);
        CodeSyncLogger.info("[Startup Activity]: Repo dir path: " + repoDirPath);

        // Run migrations
        // This will be removed after we are sure that migrations are done for all users.
        // We need these removed for performance reasons.
        MigrationManager.getInstance().populateCache();
        MigrationManager.getInstance().runMigrationsAsync();

        // Populate state
        StateUtils.populateState(project);

        PopulateBuffer.startPopulateBufferDaemon(project);

        // Schedule buffer handler.
        HandleBuffer.scheduleBufferHandler(project);

        // Check if user needs to authenticate
        CodeSyncSetup.checkUserAuthStatus(project);

        // Start the task executor.
        TaskExecutor.INSTANCE.start();

        if (project.isDisposed()) return;
        VirtualFile[] contentRoots = ProjectUtils.getAllContentRoots(project);

        // Populate state for all the opened modules. Module is the term used for projects opened using "Attach" option
        // in the IDE open dialog box.
        for (VirtualFile contentRoot : contentRoots) {
            if (Utils.isIndividualFileOpen(contentRoot.getPath())) {
                continue;
            }

            String repoPath = FileUtils.normalizeFilePath(contentRoot.getPath());
            String repoName = contentRoot.getName();

            // Ignore repos that are being migrated.
            if (MigrateRepo.getInstance().getReposBeingMigrated().contains(repoPath)) {
                continue;
            }

            RepoStatus repoStatus = StateUtils.getRepoStatus(repoPath);
            if (repoStatus == RepoStatus.DISCONNECTED) {
                CodeSyncSetup.reconnectRepoAsync(project, repoPath, repoName);
            } else if (repoStatus == RepoStatus.SYNCED_VIA_PARENT) {
                NotificationManager.getInstance().notifyInformation("You are good to go! Your repo is synced with the parent repo.", project);
            } else {
                CodeSyncSetup.setupCodeSyncRepoAsync(project, repoPath, repoName, false, false);
            }
        }
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
                            CodeSyncLogger.error(String.format(
                                "Error while renaming file: %s", CommonUtils.getStackTrace(e)
                            ));
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

                    // Abort if account has been deactivated.
                    if (StateUtils.getGlobalState().isAccountDeactivated) {
                        return;
                    }

                    ChangesHandler(event, project);
                }
            }
        };
        EditorFactory.getInstance().getEventMulticaster().addDocumentListener(changesHandler, Disposer.newDisposable());
    }
}
