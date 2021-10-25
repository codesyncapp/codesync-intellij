package org.intellij.sdk.codesync.utils;

import org.intellij.sdk.codesync.CodeSyncLogger;
import org.intellij.sdk.codesync.exceptions.FileInfoError;
import org.intellij.sdk.codesync.exceptions.FileNotFoundError;
import org.intellij.sdk.codesync.files.IgnoreFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.intellij.sdk.codesync.Constants.GIT_REPO;

public class FileUtils
{
    public static String readLineByLineJava8(Path filePath) {
        return readLineByLineJava8(filePath.toString());
    }

    //Read file content into the string with - Files.lines(Path path, Charset cs)
    public static String readLineByLineJava8(String filePath)
    {
        StringBuilder contentBuilder = new StringBuilder();

        try (Stream<String> stream = Files.lines( Paths.get(filePath), StandardCharsets.UTF_8))
        {
            stream.forEach(s -> contentBuilder.append(s).append("\n"));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return contentBuilder.toString();
    }

    public static String readFileToString(String filePath) {
        return readFileToString(new File(filePath));
    }

    public static String readFileToString(File file) {
        try {
            return org.apache.commons.io.FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            CodeSyncLogger.logEvent(String.format("Could read file '%s'. Error: %s", file.getPath(), e.getMessage()));
            return null;
        }
    }

    /*
        List absolute file paths of all the files in a directory.

        This is recursively list all the files containing in the given directory and all its sub-directories.
         */
    public static String[] listFiles(String directory) {
        String[] filePaths = {};
        IgnoreFile ignoreFile;

        try {
            Stream<Path> filePathStream = Files.walk(Paths.get(directory))
                    .filter(Files::isRegularFile);

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
            fileInfo.put("creationTime", CommonUtils.formatDate(fileAttributes.creationTime()));
            fileInfo.put("modifiedTime", CommonUtils.formatDate(fileAttributes.lastModifiedTime()));
            fileInfo.put("isBinary", isBinaryFile(file));
        } catch (IOException error) {
            throw new FileInfoError(error.getMessage());
        }

        return fileInfo;
    }

    public static boolean shouldIgnoreFile(String relPath, String repoPath) {
        if (relPath.startsWith("/") || IsGitFile(relPath)) {  return true; }
        try {
            IgnoreFile ignoreFile = new IgnoreFile(Paths.get(repoPath).toString());
            return ignoreFile.shouldIgnore(Paths.get(relPath).toFile());
        } catch (FileNotFoundError fileNotFoundError) {
            fileNotFoundError.printStackTrace();
            return false;
        }
    }

    public static Boolean IsGitFile(String path) {
        return path.startsWith(GIT_REPO);
    }

    public static boolean match(String path, String pattern) {
        PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher(String.format("glob:%s", pattern));
        return pathMatcher.matches(Paths.get(path));
    }
}
