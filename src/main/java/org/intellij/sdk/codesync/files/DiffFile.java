package org.intellij.sdk.codesync.files;

import java.io.File;
import java.util.Map;
import java.util.Date;


import org.intellij.sdk.codesync.ReadFileToString;
import org.intellij.sdk.codesync.Utils;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import org.yaml.snakeyaml.Yaml;


public class DiffFile {
    public String diff, branch, fileRelativePath, repoPath, newRelativePath, oldRelativePath, newPath, oldPath, newAbsolutePath, oldAbsolutePath;
    public Date createdAt;
    public Boolean isBinary, isDeleted, isNewFile, isRename, isDirRename;
    public File originalDiffFile;

    public DiffFile(@NotNull File originalDiffFile) {
        this.originalDiffFile = originalDiffFile;
        String contents = ReadFileToString.readLineByLineJava8(originalDiffFile.getPath());

        Yaml yaml = new Yaml();
        Map<String, Object> obj = yaml.load(contents);

        this.diff = (String) obj.get("diff");
        this.branch = (String) obj.get("branch");
        try {
            this.createdAt = (Date) obj.get("created_at");
        } catch (ClassCastException e) {
            this.createdAt = Utils.parseDate((String) obj.get("created_at"));
        }
        this.fileRelativePath = (String) obj.get("file_relative_path");

        this.repoPath = (String) obj.get("repo_path");

        this.isBinary = Utils.getBoolValue(obj, "is_binary", false);
        this.isDeleted = Utils.getBoolValue(obj, "is_deleted", false);
        this.isNewFile = Utils.getBoolValue(obj, "is_new_file", false);
        this.isRename = Utils.getBoolValue(obj, "is_rename", false);
        this.isDirRename = Utils.getBoolValue(obj, "is_dir_rename", false);

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
        // TODO: Add validation for `file-rename`, `directory-rename` and `diff-size`/
        return (
                this.repoPath != null &&
                this.branch != null &&
                this.fileRelativePath != null &&
                this.createdAt != null
        );
    }

    public Boolean delete() {
        return this.originalDiffFile.delete();
    }

    public void setDiff(String diff)  {
        this.diff = diff;
    }
}
