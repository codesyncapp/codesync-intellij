package org.intellij.sdk.codesync.utils;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.util.PathUtil;
import name.fraser.neil.plaintext.diff_match_patch;
import org.intellij.sdk.codesync.Constants;
import org.intellij.sdk.codesync.state.PluginState;
import org.intellij.sdk.codesync.state.StateUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.File;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.intellij.sdk.codesync.Constants.DATE_TIME_FORMAT;

public class CommonUtils {
    private static final String OS = System.getProperty("os.name").toLowerCase();

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

    public static String getOS() {
        if (isWindows()) {
            return Constants.PlatformIdentifier.WINDOWS;
        }
        if (isMac()) {
            return Constants.PlatformIdentifier.MAC_OS;
        }
        if (isSolaris()) {
            return Constants.PlatformIdentifier.SOLARIS;
        }

        // Default is Unix.
        return Constants.PlatformIdentifier.UNIX;
    }

    public static Calendar getIDEBuildDate () {
        return ApplicationInfo.getInstance().getBuildDate();
    }

    /*
    Returns true if the IDE version is equal to or older than the year and month given in the argument.

    Example:
        To check if IDE build is 2020.3 we can call `isIDEOlderOrEqual(2020, 3)`,
        `true` would mean IDE is 2020.3 or older and `false` would mean IDE is pre-2020.3
     */
    public static boolean isIDEOlderOrEqual(int year, int month) {
        Calendar buildDate = getIDEBuildDate();
        int buildYear = buildDate.get(Calendar.YEAR);
        int buildMonth = buildDate.get(Calendar.MONTH);

        // build year higher than the given year means IDE is older than the given date.
        if (buildYear > year) {
            return true;
        }

        // build year less than the given year means IDE was released before the given date.
        if (buildYear < year) {
            return false;
        }

        // If IDE build year and given year is same then we only need to compare build months.
        return buildMonth >= month;

    }

    @Nullable
    public static Date parseDate(String dateString, String format) {
        SimpleDateFormat pattern = new SimpleDateFormat(format);
        try {
            return new Date(pattern.parse(dateString).getTime());
        } catch (ParseException pe) {
            return null;
        }
    }

    @Nullable
    public static Date parseDate(String dateString) {
        return parseDate(dateString, DATE_TIME_FORMAT);
    }

    /*
    Using instant because it handles time zone correctly.
     */
    @Nullable
    public static Instant parseDateToInstant(String dateString, String format) {
        Date date = parseDate(dateString, format);
        if (date != null) {
            return date.toInstant();
        }
        return null;
    }

    @Nullable
    public static Instant parseDateToInstant(String dateString) {
        return parseDateToInstant(dateString, DATE_TIME_FORMAT);
    }

    public static String formatDate(Instant instant) {
        return formatDate(Date.from(instant), DATE_TIME_FORMAT);
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
        return formatDate(new Date());
    }

    public static Project getCurrentProject(String repoPath) {
        PluginState pluginState = StateUtils.getState(repoPath);
        if (pluginState != null) {
            return pluginState.project;
        }

        return getCurrentProject();
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
    public static VirtualFile findSingleFile(@NotNull String fileName, String repoPath) {
        if (PathUtil.isValidFileName(fileName)) {
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

    public static String getMacAddress() {
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            NetworkInterface networkInterface = NetworkInterface.getByInetAddress(localHost);
            byte[] hardwareAddress = networkInterface.getHardwareAddress();
            String[] hexadecimal = new String[hardwareAddress.length];
            for (int i = 0; i < hardwareAddress.length; i++) {
                hexadecimal[i] = String.format("%02X", hardwareAddress[i]);
            }
            return String.join(".", hexadecimal);
        } catch (UnknownHostException | SocketException | NullPointerException e) {
            return "";
        }
    }
}
