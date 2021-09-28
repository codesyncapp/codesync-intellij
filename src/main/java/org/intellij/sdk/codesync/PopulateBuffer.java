package org.intellij.sdk.codesync;

import com.intellij.openapi.project.Project;
import org.apache.commons.text.similarity.*;
import org.intellij.sdk.codesync.codeSyncSetup.CodeSyncSetup;
import org.intellij.sdk.codesync.exceptions.FileInfoError;
import org.intellij.sdk.codesync.exceptions.InvalidConfigFileError;
import org.intellij.sdk.codesync.exceptions.InvalidYmlFileError;
import org.intellij.sdk.codesync.factories.DiffFactory;
import org.intellij.sdk.codesync.files.ConfigFile;
import org.intellij.sdk.codesync.files.ConfigRepo;
import org.intellij.sdk.codesync.files.ConfigRepoBranch;
import org.intellij.sdk.codesync.files.UserFile;
import org.intellij.sdk.codesync.repoManagers.DeletedRepoManager;
import org.intellij.sdk.codesync.repoManagers.OriginalsRepoManager;
import org.intellij.sdk.codesync.repoManagers.ShadowRepoManager;
import org.intellij.sdk.codesync.utils.CommonUtils;
import org.intellij.sdk.codesync.utils.DiffUtils;
import org.intellij.sdk.codesync.utils.FileUtils;

import static org.intellij.sdk.codesync.Constants.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;


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
    public Set<String> renamedFiles = new HashSet<>();
    String repoPath, branchName;
    ConfigFile configFile;
    ConfigRepo configRepo;
    ConfigRepoBranch configRepoBranch;

    ShadowRepoManager shadowRepoManager;
    DeletedRepoManager deletedRepoManager;
    OriginalsRepoManager originalsRepoManager;

    Date repoModifiedAt = null;
    Map<String, Date> repoLastSyncedAtMap = new HashMap<>();

    String[] filePaths, relativeFilePaths;
    Map<String, Map<String, Object>> fileInfoMap = new HashMap<>();

    public PopulateBuffer(String repoPath, String branchName) {
        this.repoPath = repoPath;
        this.branchName = branchName;

        this.shadowRepoManager = new ShadowRepoManager(this.repoPath, this.branchName);
        this.deletedRepoManager = new DeletedRepoManager(this.repoPath, this.branchName);
        this.originalsRepoManager = new OriginalsRepoManager(this.repoPath, this.branchName);

        ConfigFile configFile;
        try {
            configFile = new ConfigFile(CONFIG_PATH);
        } catch (InvalidConfigFileError error) {
            CodeSyncLogger.logEvent(String.format(
                    "[INTELLIJ_PLUGIN][POPULATE_BUFFER] Config file error, %s.\n", error.getMessage()
            ));
            return ;
        }
        ConfigRepo configRepo = configFile.getRepo(repoPath);
        if (configRepo == null) {
            return;
        }

        ConfigRepoBranch configRepoBranch = configFile.getRepo(repoPath).getRepoBranch(branchName);
        if (configRepoBranch == null) {
            // Branch is not synced yet, we need to call init for this branch. That will be handled by other flows
            // We can simply ignore this.
            return ;
        }
        this.configFile = configFile;
        this.configRepo = configFile.getRepo(repoPath);
        this.configRepoBranch = configRepoBranch;

        this.filePaths = FileUtils.listFiles(repoPath);
        this.relativeFilePaths = Arrays.stream(this.filePaths)
                .map(filePath -> filePath.replace(this.repoPath, ""))
                .map(filePath -> filePath.replaceFirst("/", ""))
                .toArray(String[]::new);

        for (String relativeFilePath: this.relativeFilePaths) {
            Path filePath = Paths.get(repoPath, relativeFilePath);
            try {
                this.fileInfoMap.put(relativeFilePath, FileUtils.getFileInfo(filePath.toString()));
            } catch (FileInfoError error) {
                // Log the message and continue.
                CodeSyncLogger.logEvent(String.format(
                        "Error while getting the file info for %s, Error: %s", filePath, error.getMessage()
                ));
            }
        }
    }

    private static void populateBufferDaemon(final Timer timer) {
        timer.schedule(new TimerTask() {
            public void run() {
                try {
                    populateBuffer();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                populateBufferDaemon(timer);
            }
        }, DELAY_BETWEEN_BUFFER_TASKS);
    }

    public static void startPopulateBufferDaemon() {
        Timer timer = new Timer(true);
        populateBufferDaemon(timer);
    }

    public static void populateBuffer(){
        Map<String, String> reposToUpdate = detectBranchChange();
        populateBufferForMissedEvents(reposToUpdate);
    }


    public static Map<String, String> detectBranchChange() {
        Map<String, String> reposToUpdate = new HashMap<>();

        ConfigFile configFile;
        try {
            configFile = new ConfigFile(CONFIG_PATH);
        } catch (InvalidConfigFileError error) {
            CodeSyncLogger.logEvent(String.format(
                    "[INTELLIJ_PLUGIN][POPULATE_BUFFER] Config file error, %s.\n", error.getMessage()
            ));
            return reposToUpdate;
        }
        Map<String, ConfigRepo> configRepoMap = configFile.getRepos();
        UserFile userFile;

        try {
            userFile = new UserFile(USER_FILE_PATH);
        } catch (FileNotFoundException | InvalidYmlFileError error) {
            CodeSyncLogger.logEvent(String.format(
                    "[INTELLIJ_PLUGIN][POPULATE_BUFFER] User file error, %s.\n", error.getMessage()
            ));
            return reposToUpdate;
        }

        for (Map.Entry<String, ConfigRepo> configRepoEntry: configRepoMap.entrySet()) {
            String repoPath = configRepoEntry.getKey();
            ConfigRepo configRepo = configRepoEntry.getValue();

            if (configRepo.isDisconnected) {
                // No need to check updates for this repo.
                continue;
            }

            if (!configRepo.hasValidEmail()) {
                // Repo does not have a correct email address, skipping this as well.
                continue;
            }

            UserFile.User user = userFile.getUser(configRepo.email);
            if (user == null) {
                // Could not find the user with this email in user.yml file.
                continue;
            }

            if (user.getAccessToken() == null) {
                CodeSyncLogger.logEvent(String.format("Access token not found for repo: %s, %s`.", repoPath, configRepo.email));
                continue;
            }

            String branchName = Utils.GetGitBranch(repoPath);
            ShadowRepoManager shadowRepoManager = new ShadowRepoManager(repoPath, branchName);

            // Get the path of the shadow repo, one level above the branch name.
            // We do not want to include branch name in the path name that is why `getParent` is being called.
            Path shadowRepoDirectory = Paths.get(shadowRepoManager.getBaseRepoBranchDir()).getParent();

            if (!shadowRepoDirectory.toFile().exists() || !Paths.get(repoPath).toFile().exists()) {
                // TODO: Handle out of sync repo.
                continue;
            }
            OriginalsRepoManager originalsRepoManager = new OriginalsRepoManager(repoPath, branchName);
            if (!configRepo.containsBranch(branchName)) {
                Project project = CommonUtils.getCurrentProject();
                if (Paths.get(originalsRepoManager.getBaseRepoBranchDir()).toFile().exists()) {
                    String[] filePaths = FileUtils.listFiles(repoPath);

                    CodeSyncSetup.uploadRepoAsync(repoPath, project.getName(), filePaths, project);
                } else {
                    CodeSyncSetup.setupCodeSyncRepoAsync(project, false);
                }

                continue;
            }

            ConfigRepoBranch configRepoBranch = configRepo.getRepoBranch(branchName);

            if (!configRepoBranch.hasValidFiles()) {
                Project project = CommonUtils.getCurrentProject();

                String[] filePaths = FileUtils.listFiles(repoPath);
                CodeSyncSetup.uploadRepoAsync(repoPath, project.getName(), filePaths, project);

                // this repo can be checked in the next iteration.
                continue;
            }

            reposToUpdate.put(repoPath, branchName);
        }

        return reposToUpdate;
    }

    public static void populateBufferForMissedEvents(Map<String, String> readyRepos) {
        for (Map.Entry<String, String> repoEntry : readyRepos.entrySet()) {
            String repoPath = repoEntry.getKey();
            String branchName = repoEntry.getValue();
            Map<String, Object> diffsForFileUpdates = new HashMap<>();
            Map<String, Object> diffsForDeletedFiles = new HashMap<>();
            Map<String, Object> diffs = new HashMap<>();

            PopulateBuffer populateBuffer = new PopulateBuffer(repoPath, branchName);
            if (populateBuffer.configFile == null || populateBuffer.configRepoBranch == null || populateBuffer.configRepo == null) {
                // Skip it for now, it will be handled in future.
                CodeSyncLogger.logEvent(String.format(
                        "[INTELLIJ][NON_IDE_EVENTS] Could not populate for missed events, because config file for repo '%s' could not be opened.",
                        repoPath
                ));
            }

            if (!populateBuffer.configRepo.isDisconnected) {
                if (populateBuffer.isModifiedSinceLastSync()) {
                    diffsForFileUpdates = populateBuffer.getDiffsForFileUpdates();
                }
                diffsForDeletedFiles = populateBuffer.getDiffsOfDeletedFiles();

            }

            diffs.putAll(diffsForFileUpdates);
            diffs.putAll(diffsForDeletedFiles);

            // Finally add diffs to .diff directory
            populateBufferWithDiffs(diffs, populateBuffer);
        }
    }

    public static void populateBufferWithDiffs(Map<String, Object> diffs, PopulateBuffer populateBuffer) {
        for (Map.Entry<String, Object> diffEntry : diffs.entrySet()) {
            String relativeFilePath = diffEntry.getKey();
            Map<String, Object> diffData = (Map<String, Object>) diffEntry.getValue();
            String diff = (String) diffData.get("diff");
            if (diff == null) {
                diff = "";
            }

            Boolean isNewFile = CommonUtils.getBoolValue(diffData, "is_new_file", false);
            Boolean isDeleted = CommonUtils.getBoolValue(diffData, "is_deleted", false);
            Boolean isRename = CommonUtils.getBoolValue(diffData, "is_rename", false);

            if (diff.isEmpty() && !isNewFile && !isDeleted) {
                // Skipping empty file.
                continue;
            }

            if (diffData.containsKey("created_at")) {
                String createdAt = (String) diffData.get("created_at");
                DiffUtils.writeDiffToYml(
                        populateBuffer.repoPath, populateBuffer.branchName, relativeFilePath, diff,
                        isNewFile, isDeleted, isRename, false, createdAt
                );
            } else {
                DiffUtils.writeDiffToYml(
                        populateBuffer.repoPath, populateBuffer.branchName, relativeFilePath, diff,
                        isNewFile, isDeleted, isRename, false
                );
            }

        }
    }

    public boolean isModifiedSinceLastSync() {
        Optional<Date> maxModifiedTimeOptional = this.fileInfoMap.values().stream()
                .map(item -> CommonUtils.parseDate((String) item.get("modifiedTime")))
                .filter(Objects::nonNull)
                .max(Date::compareTo);

        Optional<Date> maxCreationTimeOptional = this.fileInfoMap.values().stream()
                .map(item -> CommonUtils.parseDate((String) item.get("creationTime")))
                .filter(Objects::nonNull)
                .max(Date::compareTo);

        Date maxModifiedTime = maxModifiedTimeOptional.orElse(null);
        Date maxCreationTime = maxCreationTimeOptional.orElse(null);

        if (maxModifiedTime != null && maxCreationTime!= null){
            this.repoModifiedAt = maxModifiedTime.compareTo(maxCreationTime) > 0 ? maxModifiedTime: maxCreationTime;
        } else{
            this.repoModifiedAt = maxModifiedTime != null ? maxModifiedTime: maxCreationTime;
        }

        if (this.repoLastSyncedAtMap.containsKey(this.repoPath)) {
            Date repoLastSyncedAt = this.repoLastSyncedAtMap.get(this.repoPath);
            return this.repoModifiedAt == null || this.repoModifiedAt.compareTo(repoLastSyncedAt) > 0;
        } else {
            return true;
        }
    }

    /*
        This will iterate through the files in the project repo and the see which files were updated and the changes
        were not captured by the IDE. It will then populate the buffer (i.e. create diff files) for those changes.
    */
    public Map<String, Object> getDiffsForFileUpdates() {
        Map<String, Object> diffs = new HashMap<>();
        Path repoBranchPath = Paths.get(repoPath, branchName);

        for (String relativeFilePath: this.relativeFilePaths) {
            Path filePath = Paths.get(this.repoPath, relativeFilePath);
            String previousFileContent = "", diff = "", currentFileContent;
            boolean isRename = false;
            boolean isBinary = FileUtils.isBinaryFile(filePath.toFile());
            File shadowFile = this.shadowRepoManager.getFilePath(relativeFilePath).toFile();

            // If file reference is present in the configFile, then we simply need to check if it was updated.
            // We only track changes to non-binary files, hence the check in here.
            if (this.configRepoBranch.hasFile(relativeFilePath) && !isBinary) {
                if (shadowFile.exists()) {
                    // If shadow file exists then it means existing file was updated and we simple need to
                    // add a diff file.
                    previousFileContent = FileUtils.readFileToString(shadowFile);
                } else {
                    Map<String, Object> fileInfo = this.fileInfoMap.get(relativeFilePath);
                    if (fileInfo != null && (Long) fileInfo.get("size") > FILE_SIZE_AS_COPY) {
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
            if (!this.configRepoBranch.hasFile(relativeFilePath) && !shadowFile.exists() && !FileUtils.isBinaryFile(filePath.toFile())) {
                Map<String, Object> renameResult = checkForRename(filePath.toString());
                String shadowFilePath = (String) renameResult.get("shadowFilePath");
                isRename = (Boolean) renameResult.get("isRename");

                if (isRename) {
                    String oldRelativePath = this.shadowRepoManager.getRelativeFilePath(shadowFilePath);
                    String oldProjectFilePath = Paths.get(repoBranchPath.toString(), oldRelativePath).toString();
                    String newProjectFilePath = Paths.get(repoBranchPath.toString(), relativeFilePath).toString();

                    if (!oldRelativePath.equals(relativeFilePath)) {
                        // Remove old file from shadow repo.
                        this.shadowRepoManager.deleteFile(oldRelativePath);

                        diff = DiffFactory.getFileRenameDiff(
                                oldProjectFilePath, newProjectFilePath, oldRelativePath, relativeFilePath
                        );
                        this.renamedFiles.add(oldRelativePath);
                    }
                }

            }

            boolean isNewFile = !this.configRepoBranch.hasFile(relativeFilePath) &&
                    !isRename &&
                    !this.originalsRepoManager.hasFile(relativeFilePath) &&
                    !this.shadowRepoManager.hasFile(relativeFilePath);

            if (isNewFile) {
                diff = "";
                this.originalsRepoManager.copyFiles(new String[]{filePath.toString()});
            }

            this.shadowRepoManager.copyFiles(new String[]{filePath.toString()});

            if (!diff.isEmpty() || isNewFile) {
                Map<String, Object> diffContentMap = new HashMap<>();
                Map<String, Object> fileInfo = this.fileInfoMap.get(relativeFilePath);
                Date date = CommonUtils.parseDate((String) fileInfo.get("modifiedTime"));

                diffContentMap.put("diff", diff);
                diffContentMap.put("is_rename", isRename);
                diffContentMap.put("is_new_file", isNewFile);
                diffContentMap.put("is_binary", isBinary);
                diffContentMap.put("created_at", CommonUtils.formatDate(date, DATE_TIME_FORMAT));

                diffs.put(relativeFilePath, diffContentMap);
            }
        }

        return diffs;
    }

    /*
    Check if the given file is a result of file-rename or not.

    returns true if this file is a result of a rename, false otherwise.
    */
    public Map<String, Object> checkForRename(String filePath) {
        int matchingFilesCount = 0;
        String matchingFilePath = "";
        Map<String, Object> renameResult = new HashMap<>();

        String fileContents = FileUtils.readFileToString(filePath);

        if (fileContents.isEmpty()) {
            renameResult.put("isRename", false);
            renameResult.put("shadowFilePath", matchingFilePath);

            return renameResult;
        }

        String[] shadowFilePaths = FileUtils.listFiles(this.shadowRepoManager.getBaseRepoBranchDir());
        // Filter out binary files.
        shadowFilePaths = Arrays.stream(shadowFilePaths)
                .filter(path -> !FileUtils.isBinaryFile(path))
                .toArray(String[]::new);

        for (String shadowFilePath: shadowFilePaths) {
            String relativeFilePath = this.shadowRepoManager.getRelativeFilePath(shadowFilePath);

            if (Paths.get(this.repoPath, relativeFilePath).toFile().exists()) {
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

    public Map<String, Object> getDiffsOfDeletedFiles(){
        Map<String, Object> diffs = new HashMap<>();

        Set<String> relativeFilePaths = Arrays.stream(this.relativeFilePaths)
                .collect(Collectors.toSet());

        for (String relativeFilePath : this.configRepoBranch.getFiles().keySet()) {
            // Check if we should ignore this file.
            if (
                    relativeFilePaths.contains(relativeFilePath) ||
                    this.renamedFiles.contains(relativeFilePath) ||
                    this.deletedRepoManager.hasFile(relativeFilePath) ||
                    !this.shadowRepoManager.hasFile(relativeFilePath)
            ) {
                continue;
            }
            Map<String, Object> diffContentMap = new HashMap<>();

            diffContentMap.put("is_deleted", true);
            diffContentMap.put("diff", null);  // Diff will be computed later while handling buffer.

            diffs.put(relativeFilePath, diffContentMap);
            this.deletedRepoManager.copyFiles(
                    new String[]{this.shadowRepoManager.getFilePath(relativeFilePath).toString()},
                    this.shadowRepoManager.getBaseRepoBranchDir()
            );
        }

        return diffs;
    }
}
