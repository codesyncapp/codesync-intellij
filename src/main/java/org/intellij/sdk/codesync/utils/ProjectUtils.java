package org.intellij.sdk.codesync.utils;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.intellij.sdk.codesync.exceptions.common.FileNotInModuleError;
import org.intellij.sdk.codesync.locks.CodeSyncLock;
import org.intellij.sdk.codesync.state.PluginState;
import org.intellij.sdk.codesync.state.StateUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.intellij.sdk.codesync.Constants.DELAY_BETWEEN_BUFFER_TASKS_IN_SECONDS;

public class ProjectUtils {

    /*
    Check if a daemon can be started with the given locking credentials.
    */
    public static boolean canRunDaemon(String lockType, String lockCategory, String lockOwner) {
        CodeSyncLock codeSyncLock = new CodeSyncLock(lockType, lockCategory);

        // if lock was not acquired or if it was acquired by this project then refresh the lock and return true.
        return codeSyncLock.acquireLock(lockOwner);
    }

    /*
    Start a daemon to run the given task. Use delay parameters defined in constants.
     */
    public static void startDaemonProcess(Runnable task) {
        startDaemonProcess(
            task, DELAY_BETWEEN_BUFFER_TASKS_IN_SECONDS, DELAY_BETWEEN_BUFFER_TASKS_IN_SECONDS, TimeUnit.SECONDS
        );
    }

    /*
    Start a daemon process with the given parameters.

    This method expects a Runnable task, initial delay, delay between tasks and the time unit for the delay values.
     */
    public static void startDaemonProcess(Runnable task, long initialDelay, long delayBetweenTasks, TimeUnit delayUnit) {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleWithFixedDelay(task, initialDelay, delayBetweenTasks, delayUnit);
    }

    /*
    Gets the base path for the project and returns that path after normalisation.
     */
    public static String getRepoPath(Project project) {
        String repoPath = project.getBasePath();
        if (repoPath != null) {
            return FileUtils.normalizeFilePath(project.getBasePath());
        }
        return null;
    }

    public static String getRepoPath(VirtualFile virtualFile, Project project) throws FileNotInModuleError {
        ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
        VirtualFile moduleContentRoot = projectFileIndex.getContentRootForFile(virtualFile);

        if (moduleContentRoot == null) {
            throw new FileNotInModuleError(String.format(
                "File '%s' does not belong to the project '%s' index.", virtualFile.getPath(), project.getName()
            ));
        }

        return FileUtils.normalizeFilePath(moduleContentRoot);
    }

    public static VirtualFile getRepoRoot(VirtualFile virtualFile, Project project) {
        ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
        return projectFileIndex.getContentRootForFile(virtualFile);
    }

    public static String getRepoName(VirtualFile virtualFile, Project project) throws FileNotInModuleError {
        ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
        VirtualFile moduleContentRoot = projectFileIndex.getContentRootForFile(virtualFile);

        if (moduleContentRoot == null) {
            throw new FileNotInModuleError(String.format(
                "File '%s' does not belong to the project '%s' index.", virtualFile.getPath(), project.getName()
            ));
        }

        return moduleContentRoot.getName();
    }

    public static boolean isParent(String prospectiveParent, String prospectiveChild) {
        Path parent = Paths.get(prospectiveParent);
        Path child = Paths.get(prospectiveChild);
        return child.startsWith(parent);
    }

    public static boolean isChild(String prospectiveChild, String prospectiveParent) {
        Path parent = Paths.get(prospectiveParent);
        Path child = Paths.get(prospectiveChild);
        // We are assuming the path itself is not a child of self. but it is a parent of self.
        return !child.equals(parent) && child.startsWith(parent);
    }

    public static VirtualFile[] getAllContentRoots(Project project) {
        VirtualFile[] contentRoots = ProjectRootManager.getInstance(project).getContentRoots();

        int index = 0;
        while (index < contentRoots.length) {
            VirtualFile contentRoot = contentRoots[index];

            // Remove all the children of the `contentRoot`.
            contentRoots = Arrays.stream(contentRoots).distinct().filter(
                path -> !isChild(Objects.requireNonNull(path.getCanonicalPath()), contentRoot.getCanonicalPath())
            ).toArray(VirtualFile[]::new);

            index += 1;
        }

        return contentRoots;
    }

    public static PluginState getModuleState(VirtualFile virtualFile, Project project) throws FileNotInModuleError {
        String repoPath = getRepoPath(virtualFile, project);
        return StateUtils.getState(repoPath);
    }

    public static String getPluginVersion() {
        String version = "unknown";
        IdeaPluginDescriptor ideaPluginDescriptor = PluginManagerCore.getPlugin(PluginId.getId("com.codesync"));
        if (ideaPluginDescriptor != null) {
            version = ideaPluginDescriptor.getVersion();
        }
        return version;
    }
}
