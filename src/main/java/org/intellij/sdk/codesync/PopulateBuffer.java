package org.intellij.sdk.codesync;

import org.apache.commons.text.similarity.*;
import org.intellij.sdk.codesync.exceptions.FileInfoError;
import org.intellij.sdk.codesync.exceptions.InvalidConfigFileError;
import org.intellij.sdk.codesync.factories.DiffFactory;
import org.intellij.sdk.codesync.files.ConfigFile;
import org.intellij.sdk.codesync.files.ConfigRepoBranch;
import org.intellij.sdk.codesync.repoManagers.OriginalsRepoManager;
import org.intellij.sdk.codesync.repoManagers.ShadowRepoManager;
import org.intellij.sdk.codesync.utils.CommonUtils;
import org.intellij.sdk.codesync.utils.FileUtils;

import static org.intellij.sdk.codesync.Constants.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


/*
    This class will handle non-IDE events, it will look through the files in
        1. config.yml file
        2. shadow repo
        3. project files
    To see if changes were made that were not captured by the IDE, if yes then a new diff file for those changes will
    be created and placed in the .diff directory.
    Changes that will be detected are
    1. new file creation events.
        If we find a file that is present in the project directory and is not present in shadow repo or
        the config file and is not a rename (We will detect this in step 2) then file must be newly created.
    2. file rename events
        If we find a new file whose content match with some file in the shadow repo,
        then file is probably a rename of that shadow file.
    3. file change events
        This is the simplest to handle, if the project file and shadow file do not have the same
        content then it means file was updated.
    4. file delete events
        If a file is not in the project repo but is present in the shadow repo and the config file then it was deleted.
*/
public class PopulateBuffer {
    public static final Set<String> renamedFiles = new HashSet<>();

    /*
        This will iterate through the files in the project repo and the see which files were updated and the changes
        were not captured by the IDE. It will then populate the buffer (i.e. create diff files) for those changes.
    */
    public static Map<String, Object> populateBufferForRepo(String repoPath, String branchName) {
        Map<String, Object> diffs = new HashMap<>();

        ConfigFile configFile;
        try {
            configFile = new ConfigFile(CONFIG_PATH);
        } catch (InvalidConfigFileError error) {
            CodeSyncLogger.logEvent(String.format(
                    "[INTELLIJ_PLUGIN][POPULATE_BUFFER] Config file error, %s.\n", error.getMessage()
            ));
            return diffs;
        }

        if (configFile.isRepoDisconnected(repoPath)){
            // Repo is not being synced, so no need to populate buffer for this repo.
            return diffs;
        }

        ConfigRepoBranch configRepoBranch = configFile.getRepo(repoPath).getRepoBranch(branchName);
        if (configRepoBranch == null) {
            // Branch is not synced yet, we need to call init for this branch. That will be handled by other flows
            // We can simply ignore this.
            return diffs;
        }

        String[] filePaths = FileUtils.listFiles(repoPath);
        String[] relativeFilePaths = Arrays.stream(filePaths)
                .map(filePath -> filePath.replace(repoPath, ""))
                .map(filePath -> filePath.replaceFirst("/", ""))
                .toArray(String[]::new);

        Map<String, Map<String, Object>> fileInfoMap = new HashMap<>();
        for (String relativeFilePath: relativeFilePaths) {
            Path filePath = Paths.get(repoPath, relativeFilePath);
            try {
                fileInfoMap.put(relativeFilePath, FileUtils.getFileInfo(filePath.toString()));
            } catch (FileInfoError error) {
                // Log the message and continue.
                CodeSyncLogger.logEvent(String.format(
                        "Error while getting the file info for %s, Error: %s", filePath, error.getMessage()
                ));
            }
        }

        ShadowRepoManager shadowRepoManager = new ShadowRepoManager(repoPath, branchName);
        OriginalsRepoManager originalsRepoManager = new OriginalsRepoManager(repoPath, branchName);

        for (String relativeFilePath: relativeFilePaths) {
            Path repoBranchPath = Paths.get(repoPath, branchName);
            Path filePath = Paths.get(repoPath, relativeFilePath);
            String previousFileContent = "", diff = "", currentFileContent;
            boolean isRename = false;
            boolean isBinary = FileUtils.isBinaryFile(filePath.toFile());
            File shadowFile = shadowRepoManager.getFilePath(relativeFilePath).toFile();

            // If file reference is present in the configFile, then we simply need to check if it was updated.
            // We only track changes to non-binary files, hence the check in here.
            if (configRepoBranch.hasFile(relativeFilePath) && !isBinary) {
                if (shadowFile.exists()) {
                    // If shadow file exists then it means existing file was updated and we simple need to
                    // add a diff file.
                    previousFileContent = FileUtils.readFileToString(shadowFile);
                } else {
                    Map<String, Object> fileInfo = fileInfoMap.get(relativeFilePath);
                    if (fileInfo != null && (Integer) fileInfo.get("size") > FILE_SIZE_AS_COPY) {
                        previousFileContent = FileUtils.readFileToString(filePath.toFile());
                    }
                }
                currentFileContent = FileUtils.readFileToString(filePath.toFile());

                diff = CommonUtils.computeDiff(previousFileContent, currentFileContent);
            }

            // If
            //  1. file reference is not present in the config file AND
            //  2. shadow file is not present
            // Then the file could either be a new file or a renamed file.
            //  We will first perform rename-check to either rule-out that possibility or handle file rename.
            if (!configRepoBranch.hasFile(relativeFilePath) && !shadowFile.exists() && !FileUtils.isBinaryFile(filePath.toFile())) {
                Map<String, Object> renameResult = checkForRename(repoPath, filePath.toString(), shadowRepoManager);
                String shadowFilePath = (String) renameResult.get("shadowFilePath");
                String oldRelativePath = shadowRepoManager.getRelativeFilePath(shadowFilePath);
                String oldProjectFilePath = Paths.get(repoBranchPath.toString(), oldRelativePath).toString();
                String newProjectFilePath = Paths.get(repoBranchPath.toString(), relativeFilePath).toString();

                isRename = !oldRelativePath.equals(relativeFilePath);

                if (isRename) {
                    // Remove old file from shadow repo.
                    shadowRepoManager.deleteFile(oldRelativePath);

                    diff = DiffFactory.getFileRenameDiff(
                            oldProjectFilePath, newProjectFilePath, oldRelativePath, relativeFilePath
                    );
                    renamedFiles.add(oldRelativePath);
                }
            }

            Boolean isNewFile = !configRepoBranch.hasFile(relativeFilePath) &&
                    !isRename &&
                    !originalsRepoManager.getFilePath(relativeFilePath).toFile().exists() &&
                    !shadowRepoManager.getFilePath(relativeFilePath).toFile().exists();

            if (isNewFile) {
                diff = "";
                originalsRepoManager.copyFiles(new String[]{filePath.toString()});
            }

            shadowRepoManager.copyFiles(new String[]{filePath.toString()});

            if (!diff.isEmpty() || isNewFile) {
                Map<String, Object> diffContentMap = new HashMap<>();
                Map<String, Object> fileInfo = fileInfoMap.get(relativeFilePath);
                Date date = CommonUtils.parseDate((String) fileInfo.get("modifiedTime"));

                diffContentMap.put("diff", diff);
                diffContentMap.put("is_rename", isRename);
                diffContentMap.put("is_new_file", isNewFile);
                diffContentMap.put("is_binary", isBinary);
                diffContentMap.put("created_at", CommonUtils.formatDate(date, DATETIME_FORMAT));

                diffs.put(relativeFilePath, diffContentMap);
            }
        }

        return diffs;
    }

    /*
    Check if the given file is a result of file-rename or not.

    returns true if this file is a result of a rename, false otherwise.
    */
    public static Map<String, Object> checkForRename(String repoPath, String filePath, ShadowRepoManager shadowRepoManager) {
        int matchingFilesCount = 0;
        String matchingFilePath = "";
        Map<String, Object> renameResult = new HashMap<>();

        String fileContents = FileUtils.readFileToString(filePath);

        if (fileContents.isEmpty()) {
            renameResult.put("isRename", false);
            renameResult.put("shadowFilePath", matchingFilePath);

            return renameResult;
        }

        String[] shadowFilePaths = FileUtils.listFiles(shadowRepoManager.getBaseRepoBranchDir());
        // Filter out binary files.
        shadowFilePaths = Arrays.stream(shadowFilePaths)
                .filter(path -> !FileUtils.isBinaryFile(path))
                .toArray(String[]::new);

        for (String shadowFilePath: shadowFilePaths) {
            String relativeFilePath = shadowRepoManager.getRelativeFilePath(shadowFilePath);

            if (Paths.get(repoPath, relativeFilePath).toFile().exists()) {
                // Skip the shadow files that have corresponding files in the project repo.
                continue;
            }

            String shadowContent = FileUtils.readFileToString(shadowFilePath);
            Double similarityRatio = compare(fileContents, shadowContent);

            if (similarityRatio > SEQUENCE_MATCHER_RATIO) {
                matchingFilesCount += 1;
                matchingFilePath = shadowFilePath;
            }
        }
        renameResult.put("isRename", matchingFilesCount == 1);
        renameResult.put("shadowFilePath", matchingFilePath);
        return renameResult;
    }

    public static Double compare(String first, String second) {
        JaroWinklerSimilarity jaroWinklerSimilarity = new JaroWinklerSimilarity();
        return jaroWinklerSimilarity.apply(first, second);
    }
}
