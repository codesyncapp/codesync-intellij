package org.intellij.sdk.codesync;

import com.intellij.openapi.project.Project;
import org.apache.commons.text.similarity.*;
import org.intellij.sdk.codesync.clients.CodeSyncClient;
import org.intellij.sdk.codesync.codeSyncSetup.CodeSyncSetup;
import org.intellij.sdk.codesync.exceptions.FileInfoError;
import org.intellij.sdk.codesync.exceptions.InvalidConfigFileError;
import org.intellij.sdk.codesync.factories.DiffFactory;
import org.intellij.sdk.codesync.files.ConfigFile;
import org.intellij.sdk.codesync.files.ConfigRepo;
import org.intellij.sdk.codesync.files.ConfigRepoBranch;
import org.intellij.sdk.codesync.models.UserAccount;
import org.intellij.sdk.codesync.repoManagers.DeletedRepoManager;
import org.intellij.sdk.codesync.repoManagers.OriginalsRepoManager;
import org.intellij.sdk.codesync.repoManagers.ShadowRepoManager;
import org.intellij.sdk.codesync.utils.*;

import static org.intellij.sdk.codesync.Constants.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
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
        the config file and is not a file rename (We will detect this in step 2) then file must be newly created.
    2. file rename events
        If we find a new file whose content match with some file in the shadow repo,
        then file is probably a file rename of that shadow file.
    3. file change events
        This is the simplest to handle, if the project file and shadow file do not have the same
        content then it means file was updated.
    4. file delete events
        If a file is not in the project repo but is present in the shadow repo and the config file then it was deleted.
*/
public class PopulateBuffer {
    public Set<String> renamedFiles = new HashSet<>();
    public static final Set<String> reposBeingSynced = new HashSet<>();
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
            CodeSyncLogger.critical(String.format(
                "[POPULATE_BUFFER] Config file error, %s.\n", error.getMessage()
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
                .map(filePath -> filePath.replaceFirst(Pattern.quote(String.valueOf(File.separatorChar)), ""))
                .toArray(String[]::new);

        for (String relativeFilePath: this.relativeFilePaths) {
            Path filePath = Paths.get(repoPath, relativeFilePath);
            try {
                this.fileInfoMap.put(relativeFilePath, FileUtils.getFileInfo(filePath.toString()));
            } catch (FileInfoError error) {
                // Log the message and continue.
                CodeSyncLogger.error(String.format(
                    "Error while getting the file info for %s, Error: %s", filePath, error.getMessage()
                ));
            }
        }
    }

    private static void populateBufferDaemon(final Timer timer, Project project) {
        timer.schedule(new TimerTask() {
            public void run() {
                try {
                    populateBuffer(project);
                } catch (Exception e) {
                    CodeSyncLogger.error(String.format("populateBuffer exited with error: %s", e.getMessage()));
                }

                populateBufferDaemon(timer, project);
            }
        }, DELAY_BETWEEN_BUFFER_TASKS);
    }

    public static void startPopulateBufferDaemon(Project project) {
        Timer timer = new Timer(true);
        populateBufferDaemon(timer, project);
    }

    public static void populateBuffer(Project project) {
        boolean canRunDaemon = ProjectUtils.canRunDaemon(
            LockFileType.POPULATE_BUFFER_LOCK,
            POPULATE_BUFFER_DAEMON_LOCK_KEY,
            project.getName()
        );

        if (!canRunDaemon) {
            return;
        }
        Map<String, String> reposToUpdate = detectBranchChange();
        populateBufferForMissedEvents(reposToUpdate);
    }

    public static Map<String, String> detectBranchChange() {
        Map<String, String> reposToUpdate = new HashMap<>();
        CodeSyncClient codeSyncClient = new CodeSyncClient();
        if (!codeSyncClient.isServerUp()) {
            return reposToUpdate;
        }

        ConfigFile configFile;
        try {
            configFile = new ConfigFile(CONFIG_PATH);
        } catch (InvalidConfigFileError error) {
            CodeSyncLogger.critical(String.format(
                "[POPULATE_BUFFER] Config file error, %s.\n", error.getMessage()
            ));
            return reposToUpdate;
        }
        Map<String, ConfigRepo> configRepoMap = configFile.getRepos();
        UserAccount userAccount;
        userAccount = new UserAccount();

        for (Map.Entry<String, ConfigRepo> configRepoEntry: configRepoMap.entrySet()) {
            String repoPath = configRepoEntry.getKey();
            String repoName = Paths.get(repoPath).getFileName().toString();
            ConfigRepo configRepo = configRepoEntry.getValue();

            if (configRepo.isDisconnected) {
                // No need to check updates for this repo.
                continue;
            }

            if (!configRepo.hasValidEmail()) {
                // Repo does not have a correct email address, skipping this as well.
                continue;
            }

            userAccount = userAccount.getUser(configRepo.email);
            if (userAccount == null) {
                // Could not find the user with this email in user.yml file.
                continue;
            }

            if (userAccount.getAccessToken() == null) {
                CodeSyncLogger.error(String.format("Access token not found for repo: %s, %s`.", repoPath, configRepo.email));
                continue;
            }
            // If repo path does not exist anymore, skip it
            File _repoPath = new File(repoPath);
            if (!_repoPath.exists()) continue;

            String branchName = Utils.GetGitBranch(repoPath);
            ShadowRepoManager shadowRepoManager = new ShadowRepoManager(repoPath, branchName);

            // Get the path of the shadow repo, one level above the branch name.
            // We do not want to include branch name in the path name that is why `getParent` is being called.
            Path shadowRepoDirectory = Paths.get(shadowRepoManager.getBaseRepoBranchDir()).getParent();

            if (!shadowRepoDirectory.toFile().exists() || !Paths.get(repoPath).toFile().exists()) {
                // TODO: Handle out of sync repo.
                continue;
            }
            String repoAndBranchName = String.format("%s-%s", repoPath, branchName);
            OriginalsRepoManager originalsRepoManager = new OriginalsRepoManager(repoPath, branchName);

            if (reposBeingSynced.contains(repoAndBranchName)) {
                // branch is already being synced.
                continue;
            }

            if (!configRepo.containsBranch(branchName)) {
                Project project = CommonUtils.getCurrentProject(repoPath);
                if (Paths.get(originalsRepoManager.getBaseRepoBranchDir()).toFile().exists()) {
                    String[] filePaths = FileUtils.listFiles(repoPath);

                    CodeSyncSetup.uploadRepoAsync(repoPath, repoName, filePaths, project, true);
                } else {
                    CodeSyncSetup.setupCodeSyncRepoAsync(project, repoPath, repoName, true, true);
                }

                reposBeingSynced.add(repoAndBranchName);
                continue;
            }

            ConfigRepoBranch configRepoBranch = configRepo.getRepoBranch(branchName);

            if (!configRepoBranch.hasValidFiles()) {
                Project project = CommonUtils.getCurrentProject(repoPath);

                String[] filePaths = FileUtils.listFiles(repoPath);
                CodeSyncSetup.uploadRepoAsync(repoPath, repoName, filePaths, project, true);

                reposBeingSynced.add(repoAndBranchName);
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
                // Skip it for now, it will be handled in the future.
                CodeSyncLogger.critical(String.format(
                    "[NON_IDE_EVENTS] Could not populate for missed events, because config file for repo '%s' could not be opened.",
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
                .map(item -> CodeSyncDateUtils.parseDate((String) item.get("modifiedTime")))
                .filter(Objects::nonNull)
                .max(Date::compareTo);

        Optional<Date> maxCreationTimeOptional = this.fileInfoMap.values().stream()
                .map(item -> CodeSyncDateUtils.parseDate((String) item.get("creationTime")))
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

        for (String relativeFilePath: this.relativeFilePaths) {
            Path filePath = Paths.get(this.repoPath, relativeFilePath);
            String previousFileContent = "", diff = "", currentFileContent;
            boolean isRename = false;
            boolean isBinary = FileUtils.isBinaryFile(filePath.toFile());
            File shadowFile = this.shadowRepoManager.getFilePath(relativeFilePath).toFile();

            // If file reference is present in the configFile, then we simply need to check if it was updated.
            // We only track changes to non-binary files, hence the check in here.
            if (this.configRepoBranch.hasFile(relativeFilePath) && !isBinary) {
                Map<String, Object> fileInfo = this.fileInfoMap.get(relativeFilePath);

                if (shadowFile.exists()) {
                    // If shadow file exists then it means existing file was updated and we simple need to
                    // add a diff file.
                    // We need to check if shadow file was updated after file was written to disk.
                    // Because daemon should only pick changes once thi condition satisfies

                    try {
                        Map<String, Object> shadowFileInfo = FileUtils.getFileInfo(shadowFile.getPath());
                        Date fileModifiedTime = CodeSyncDateUtils.parseDate((String) fileInfo.get("modifiedTime"));
                        Date shadowFileModifiedTime = CodeSyncDateUtils.parseDate((String) shadowFileInfo.get("modifiedTime"));

                        // If shadow file was modified after the file was written to disk, then, skip the change.
                        if (
                                shadowFileModifiedTime != null &&
                                fileModifiedTime != null &&
                                shadowFileModifiedTime.after(fileModifiedTime)
                        ) {
                            continue;
                        }
                    } catch (FileInfoError error) {
                        // Log the message and continue.
                        CodeSyncLogger.error(String.format(
                                "Error while getting the file info for shadow file %s, Error: %s",
                                shadowFile.getPath(),
                                error.getMessage()
                        ));
                        continue;
                    }
                    previousFileContent = FileUtils.readFileToString(shadowFile);
                } else {
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

                    if (!oldRelativePath.equals(relativeFilePath)) {
                        // Remove old file from shadow repo.
                        this.shadowRepoManager.deleteFile(oldRelativePath);

                        diff = DiffFactory.getFileRenameDiff(oldRelativePath, relativeFilePath);
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
                Date createdAt = CodeSyncDateUtils.parseDate((String) fileInfo.get("creationTime"));

                diffContentMap.put("diff", diff);
                diffContentMap.put("is_rename", isRename);
                diffContentMap.put("is_new_file", isNewFile);
                diffContentMap.put("is_binary", isBinary);
                diffContentMap.put("created_at", CodeSyncDateUtils.formatDate(createdAt, DATE_TIME_FORMAT));

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
