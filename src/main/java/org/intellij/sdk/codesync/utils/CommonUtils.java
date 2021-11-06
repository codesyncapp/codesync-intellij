package org.intellij.sdk.codesync.utils;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.util.PathUtil;
import name.fraser.neil.plaintext.diff_match_patch;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.File;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicReference;

import static org.intellij.sdk.codesync.Constants.DATE_TIME_FORMAT;

public class CommonUtils {
    private static String OS = System.getProperty("os.name").toLowerCase();

    public static boolean isWindows() {
        return OS.contains("win");
    }

    public static boolean isMac() {
        return OS.contains("mac");
    }

    public static boolean isUnix() {
        return (OS.contains("nix") || OS.contains("nux") || OS.contains("aix"));
    }

    public static boolean isSolaris() {
        return OS.contains("sunos");
    }

    @Nullable
    public static Date parseDate(String dateString) {
        SimpleDateFormat pattern = new SimpleDateFormat(DATE_TIME_FORMAT);
        try {
            return new Date(pattern.parse(dateString).getTime());
        } catch (ParseException pe) {
            return null;
        }
    }

    public static String formatDate(Date date) {
        return formatDate(date, DATE_TIME_FORMAT);
    }

    public static String formatDate(Date date, String format) {
        SimpleDateFormat pattern = new SimpleDateFormat(format);
        pattern.setTimeZone(TimeZone.getTimeZone("UTC"));
        return pattern.format(date);
    }

    public static String formatDate(FileTime date) {
        SimpleDateFormat pattern = new SimpleDateFormat(DATE_TIME_FORMAT);
        pattern.setTimeZone(TimeZone.getTimeZone("UTC"));
        return pattern.format(date.toMillis());
    }

    /*
        Convert Date object to POSIX time.
         */
    public static Long getPosixTime(Date date) {
        return date.getTime() / 1000L;
    }

    /*
        Convert Date String to POSIX time.
         */
    public static Long getPosixTime(String dateString) {
        Date date = parseDate(dateString);
        if (date ==  null) {
            return null;
        }
        return date.getTime() / 1000L;
    }

    public static String computeDiff(String initialVersion, String latterVersion) {
        diff_match_patch dmp = new diff_match_patch();
        LinkedList<diff_match_patch.Patch> patches = dmp.patch_make(initialVersion, latterVersion);

        // return text representation of patches objects
        return dmp.patch_toText(patches);
    }

    public static boolean getBoolValue(Map<String, Object> map, String key, boolean defaultValue) {
        Boolean binaryValue = (Boolean) map.getOrDefault(key, defaultValue);
        return (binaryValue != null ? binaryValue: false);
    }

    public static String getCurrentDatetime()  {
        Date currentTime = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_TIME_FORMAT);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(currentTime);
    }

    public static Project getCurrentProject() {
        return CommonUtils.invokeAndWait(
            () -> {
                Project[] allProjects = ProjectManager.getInstance().getOpenProjects();
                Project foundProject = null;

                for (Project project: allProjects) {
                    Window window = WindowManager.getInstance().suggestParentWindow(project);
                    if (window != null && window.isFocused()) {
                        foundProject = project;
                    }

                    if (foundProject != null) {
                        break;
                    }
                }

                return foundProject;

            },
            ModalityState.defaultModalityState()
        );
    }

    @Nullable
    public static VirtualFile findSingleFile(@NotNull String fileName, @NotNull Project project) {
        if (PathUtil.isValidFileName(fileName)) {
            String repoPath = project.getBasePath();
            if (isWindows()){
                // For some reason people at intelli-j thought it would be a good idea to confuse users by using
                // forward slashes in paths instead of windows path separator.
                repoPath = repoPath.replaceAll("/", "\\\\");
            }

            File file = Paths.get(repoPath, fileName).toFile();
            return LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
        }
        return null;
    }

    /**
     * Runs the passed computation synchronously on the EDT and returns the result.
     *
     * ref: https://www.programcreek.com/java-api-examples/?code=saros-project%2Fsaros%2Fsaros-master%2Fintellij%2Fsrc%2Fsaros%2Fintellij%2Fui%2Futil%2FSafeDialogUtils.java#
     *
     * <p>If an exception occurs during the execution it is thrown back to the caller, including
     * <i>RuntimeException<i> and <i>Error</i>.
     *
     * @param <T> the type of the result of the computation
     * @param <E> the type of the exception that might be thrown by the computation
     * @param computation the computation to run
     * @param modalityState the modality state to use
     * @return returns the result of the computation
     * @throws E any exception that occurs while executing the computation
     * @see Application#invokeAndWait(Runnable, ModalityState)
     */
    @SuppressWarnings("unchecked")
    public static <T, E extends Throwable> T invokeAndWait(
            @NotNull Computable<T> computation, @NotNull ModalityState modalityState)
            throws E {

        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Throwable> throwable = new AtomicReference<>();
        Application application = ApplicationManager.getApplication();

        application.invokeAndWait(
                () -> {
                    try {
                        result.set((T) computation.compute());

                    } catch (Throwable t) {
                        throwable.set(t);
                    }
                },
                modalityState);

        Throwable t = throwable.get();

        if (t == null) return result.get();

        if (t instanceof Error) throw (Error) t;

        if (t instanceof RuntimeException) throw (RuntimeException) t;

        throw (E) t;
    }
}
