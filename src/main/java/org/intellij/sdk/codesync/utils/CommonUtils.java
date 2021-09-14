package org.intellij.sdk.codesync.utils;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
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

import static org.intellij.sdk.codesync.Constants.DATE_TIME_FORMAT;

public class CommonUtils {
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
    }

    @Nullable
    public static VirtualFile findSingleFile(@NotNull String fileName, @NotNull Project project) {
        if (PathUtil.isValidFileName(fileName)) {
            File file = Paths.get(project.getBasePath(), fileName).toFile();
            return LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
        }
        return null;
    }
}
