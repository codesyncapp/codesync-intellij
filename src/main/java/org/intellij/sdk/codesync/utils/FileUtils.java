package org.intellij.sdk.codesync.utils;

import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.io.IOUtils;
import org.intellij.sdk.codesync.CodeSyncLogger;
import org.intellij.sdk.codesync.exceptions.FileInfoError;
import org.intellij.sdk.codesync.exceptions.FileNotFoundError;
import org.intellij.sdk.codesync.files.IgnoreFile;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.intellij.sdk.codesync.Constants.GIT_REPO;
import static org.intellij.sdk.codesync.Constants.IGNORABLE_DIRECTORIES;

public class FileUtils
{
    public static String readFileToString(String filePath) {
        return readFileToString(new File(filePath));
    }

    public static String readFileToString(File file) {
        try {
            return org.apache.commons.io.FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            CodeSyncLogger.error(String.format("Could not read file '%s'. Error: %s", file.getPath(), e.getMessage()));
            return null;
        }
    }

    public static String readURLToString(String url) {
        try {
            return IOUtils.toString(new URL(url), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    public static JSONObject readURLToJson(String url) {
        String jsonString = readURLToString(url);
        JSONObject jsonObject = null;
        if (jsonString != null) {
            try {
                jsonObject = (JSONObject) JSONValue.parseWithException(jsonString);
            } catch (ParseException e) {
                CodeSyncLogger.error(String.format("Error parsing json of plugin user json file. Error: %s", e.getMessage()));
                // Ignore error.
            }
        }

        return jsonObject;
    }

    /*
    List absolute file paths of all the files in a directory.

    This is recursively list all the files containing in the given directory and all its subdirectories.
    */
    public static String[] listFiles(String directory) {
        String[] filePaths = {};
        IgnoreFile ignoreFile;

        try {
            Stream<Path> filePathStream = Files.walk(Paths.get(directory))
                    .filter(Files::isRegularFile)
                    .filter(path -> !FileUtils.isIgnorableFile(path.toString(), directory));

            try {
                ignoreFile = new IgnoreFile(Paths.get(directory).toString());
                filePathStream = filePathStream.filter(path -> !ignoreFile.shouldIgnore(path.toFile()));
            } catch (FileNotFoundError error) {
                // Do not filter anything if ignore file is not present.
            }

            filePaths = filePathStream.map(Path::toString).toArray(String[]::new);
        }
        catch (IOException e) {
            e.printStackTrace();
        }

       return filePaths;
    }

    public static boolean isBinaryFile(String filePath){
        return isBinaryFile(new File(filePath));
    }

    /*
    Check if the given file is a binary or a text file.
    */
    public static boolean isBinaryFile(File file) {
        try {
            return !isTextFile(file);
        } catch (Exception e){
            return false;
        }
    }

    private static boolean isTextFile(File f) throws Exception {
        if(!f.exists())
            return false;
        FileInputStream in = new FileInputStream(f);
        int size = in.available();
        if(size > 1000)
            size = 1000;
        byte[] data = new byte[size];
        in.read(data);
        in.close();
        String s = new String(data, "ISO-8859-1");
        String s2 = s.replaceAll(
                "[a-zA-Z0-9ßöäü\\.\\*!\"§\\$\\%&/()=\\?@~'#:,;\\"+
                        "+><\\|\\[\\]\\{\\}\\^°²³\\\\ \\n\\r\\t_\\-`´âêîô"+
                        "ÂÊÔÎáéíóàèìòÁÉÍÓÀÈÌÒ©‰¢£¥€±¿»«¼½¾™ª]", "");
        // will delete all text signs

        if (s.length() == 0 | s2.length() == 0) {
            return true;
        }
        double d = (double)(s.length() - s2.length()) / (double)(s.length());
        // percentage of text signs in the text
        return d > 0.95;
    }

    public static Map<String, Object> getFileInfo(String filePath) throws FileInfoError {
        Map<String, Object> fileInfo = new HashMap<>();
        File file = new File(filePath);
        Path path = file.toPath();
        try {
            BasicFileAttributes fileAttributes = Files.readAttributes(path, BasicFileAttributes.class);
            fileInfo.put("size", fileAttributes.size());
            fileInfo.put("creationTime", CodeSyncDateUtils.formatDate(fileAttributes.creationTime()));
            fileInfo.put("modifiedTime", CodeSyncDateUtils.formatDate(fileAttributes.lastModifiedTime()));
            fileInfo.put("isBinary", isBinaryFile(file));
        } catch (IOException error) {
            throw new FileInfoError(error.getMessage());
        }

        return fileInfo;
    }

    /*
    Get the creation time attribute of the given file as an instance of `Instant` class.
    */
    public static Instant getFileCreationDate(File file) throws FileInfoError {
        BasicFileAttributes basicFileAttributes = null;
        try {
            basicFileAttributes = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
        } catch (IOException error) {
            throw new FileInfoError(error.getMessage());
        }

        return Instant.ofEpochMilli(basicFileAttributes.creationTime().toMillis());
    }

    public static boolean shouldIgnoreFile(String relPath, String repoPath) {
        if (relPath.startsWith("/") || isIgnorableFile(relPath)) {  return true; }
        try {
            IgnoreFile ignoreFile = new IgnoreFile(repoPath);
            return ignoreFile.shouldIgnore(Paths.get(repoPath, relPath).toFile());
        } catch (FileNotFoundError fileNotFoundError) {
            fileNotFoundError.printStackTrace();
            return false;
        }
    }

    public static Boolean isIgnorableFile(String relativePath) {
        for (String pathFragment: IGNORABLE_DIRECTORIES) {
            if(relativePath.startsWith(pathFragment)) {
                return true;
            }
        }
        return relativePath.startsWith(GIT_REPO);
    }

    public static Boolean isIgnorableFile(String absolutePath, String repoPath) {
        String relativePath = absolutePath.replace(repoPath, "").
                replaceFirst(Pattern.quote(String.valueOf(File.separatorChar)), "");

        return isIgnorableFile(relativePath);
    }

    public static boolean match(String path, String pattern) {
        PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher(String.format("glob:%s", pattern));
        return pathMatcher.matches(Paths.get(path));
    }

    /*
    Normalize file path depending on the operating system.

    This method will make sure proper slashes are used for each OS.
    */
    public static String normalizeFilePath(String filePath) {
        if (CommonUtils.isWindows()){
            // For some reason people at intelli-j thought it would be a good idea to confuse users by using
            // forward slashes in paths instead of windows path separator.
            filePath = filePath.replaceAll("/", "\\\\");
        }

        return filePath;
    }

    public static String normalizeFilePath(VirtualFile virtualFile) {
        return normalizeFilePath(virtualFile.getPath());
    }

    /*
    Checks the and returns true if dangling file is older than 5 days.
    */
    public static boolean isStaleFile(File file) {
        Instant creationTime;
        try {
            creationTime = getFileCreationDate(file);
        } catch (FileInfoError e) {
            // Could not get the file details so skipping file removal.
            return false;
        }
        // Check if the file is 5 days older than the current time
        Instant currentTime = Instant.now();
        return creationTime.isBefore(currentTime.minus(5, ChronoUnit.DAYS));
    }
}
