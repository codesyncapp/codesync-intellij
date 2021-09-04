package org.intellij.sdk.codesync;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class ReadFileToString
{
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
            return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            CodeSyncLogger.logEvent(String.format("Could read file '%s'. Error: %s", file.getPath(), e.getMessage()));
            return null;
        }
    }
}