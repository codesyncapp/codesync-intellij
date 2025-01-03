package org.intellij.sdk.codesync.files;

import java.io.File;
import java.util.Map;
import java.util.Date;


import org.intellij.sdk.codesync.CodeSyncLogger;
import org.intellij.sdk.codesync.utils.CommonUtils;
import org.intellij.sdk.codesync.utils.CodeSyncDateUtils;
import org.intellij.sdk.codesync.utils.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import static org.intellij.sdk.codesync.Constants.DIFF_SIZE_LIMIT;


public class DiffFile {
    public String diff, branch, fileRelativePath, repoPath, newRelativePath, oldRelativePath,
        newPath, oldPath, newAbsolutePath, oldAbsolutePath, commitHash;
    public Date createdAt, addedAt; // `addedAt` means the date file sync started. Only useful in case of new file diff.
    public Boolean isBinary, isDeleted, isNewFile, isRename, isDirRename;
    public File originalDiffFile;

    public String contents;

    public DiffFile(@NotNull File originalDiffFile) {
        this.originalDiffFile = originalDiffFile;
        this.contents = FileUtils.readFileToString(originalDiffFile);
        Map<String, Object> obj;
        Yaml yaml = new Yaml();
        try {
            obj = yaml.load(this.contents);
        } catch (YAMLException error) {
            CodeSyncLogger.debug(String.format(
                "Invalid diff file '%s'. File contents: %s", this.originalDiffFile.getPath(), this.contents
            ));
            return;
        }

        if (obj == null) {
            CodeSyncLogger.debug(String.format("Invalid diff file. YAML returned null after loading contents. File Contents: %s", this.contents));
            return;
        }

        this.diff = (String) obj.get("diff");
        this.branch = (String) obj.get("branch");
        try {
            this.createdAt = (Date) obj.get("created_at");
        } catch (ClassCastException e) {
            this.createdAt = CodeSyncDateUtils.parseDate((String) obj.get("created_at"));
        }
        this.fileRelativePath = (String) obj.get("file_relative_path");

        this.repoPath = (String) obj.get("repo_path");
        this.commitHash = (String) obj.get("commit_hash");

        this.isBinary = CommonUtils.getBoolValue(obj, "is_binary", false);
        this.isDeleted = CommonUtils.getBoolValue(obj, "is_deleted", false);
        this.isNewFile = CommonUtils.getBoolValue(obj, "is_new_file", false);
        this.isRename = CommonUtils.getBoolValue(obj, "is_rename", false);
        this.isDirRename = CommonUtils.getBoolValue(obj, "is_dir_rename", false);

        if (this.isNewFile) {
            this.addedAt =  CodeSyncDateUtils.parseDate((String) obj.get("added_at"));
        }

        if (this.isDirRename) {
            try {
                JSONObject diffJSON = (JSONObject) JSONValue.parseWithException(this.diff);
                this.newPath = (String) diffJSON.get("new_path");
                this.oldPath = (String) diffJSON.get("old_path");
            } catch (ParseException e) {
                // Not sure what else we can do in case of invalid diff file.
                this.delete();
                e.printStackTrace();
            }
        }
        if (this.isRename) {
            try {
                JSONObject diffJSON = (JSONObject) JSONValue.parseWithException(this.diff);
                this.newRelativePath = (String) diffJSON.get("new_rel_path");
                this.oldRelativePath = (String) diffJSON.get("old_rel_path");
                this.newAbsolutePath = (String) diffJSON.get("new_rel_path");
                this.oldAbsolutePath = (String) diffJSON.get("old_rel_path");
            } catch (ParseException e) {
                // Not sure what else we can do in case of invalid diff file.
                this.delete();
                e.printStackTrace();
            }
        }
    }

    public Boolean isValid () {
        // Validate diff file has non-null values for all the required fields.
        if (
                this.repoPath == null ||
                this.branch == null ||
                this.fileRelativePath == null ||
                this.createdAt == null
        ) {
            // mark diff file as invalid if any of the required fields above has a null value.
            return false;
        }

        // Make sure diff is of acceptable size.
        if (this.diff != null && this.diff.length() > DIFF_SIZE_LIMIT) {
            return false;
        }

        // Validation logic for only rename or directory renmae diff files.
        if (this.isRename || this.isDirRename) {
            if (this.isRename && this.isDirRename) {
                return false;
            }

            // Make sure all fields required for file rename have non-null values.
            if (this.isRename) {
                if (this.newRelativePath == null || this.oldRelativePath == null) {
                    // Mark diff as invalid if either of the required values in rename file operation have null values.
                    return false;
                }
            }
            // Make sure all fields required for directory rename have non-null values.
            if (this.isDirRename) {
                if (this.newPath == null || this.oldPath == null) {
                    // Mark diff as invalid if either of the required values in rename directory operation have null values.
                    return false;
                }
            }
        }

        return true;

    }

    public boolean delete() {
        return this.originalDiffFile.delete();
    }

    public static boolean delete(String diffFilePath) {
        if (diffFilePath != null){
            return new File(diffFilePath).delete();
        }
        return false;
    }

    public void setDiff(String diff)  {
        this.diff = diff;
    }

    /*
    Helper method to check if diff content is empty or null.
     */
    public boolean isEmptyDiff() {
        return this.diff == null || this.diff.length() == 0;
    }
}
