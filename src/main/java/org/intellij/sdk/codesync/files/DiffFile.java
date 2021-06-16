package org.intellij.sdk.codesync.files;

import java.io.File;
import java.util.Map;
import java.util.Date;


import org.intellij.sdk.codesync.ReadFileToString;
import org.intellij.sdk.codesync.Utils;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.Yaml;


public class DiffFile {
    public String diff, branch, fileRelativePath, repoPath, newRelativePath, oldRelativePath, newPath, oldPath;
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
        this.newRelativePath = (String) obj.get("new_rel_path");
        this.oldRelativePath = (String) obj.get("old_rel_path");
        this.repoPath = (String) obj.get("repo_path");
        this.newPath = (String) obj.get("new_path");
        this.oldPath = (String) obj.get("old_path");

        isBinary = (Boolean) obj.getOrDefault("is_binary", false);
        this.isBinary = isBinary != null ? isBinary: false;

        isDeleted = (Boolean) obj.getOrDefault("is_deleted", false);
        this.isDeleted = isDeleted != null ? isDeleted: false;

        isNewFile = (Boolean) obj.getOrDefault("is_new_file", false);
        this.isNewFile = isNewFile != null ? isNewFile: false;

        isRename = (Boolean) obj.getOrDefault("is_rename", false);
        this.isRename = isRename != null ? isRename: false;

        isDirRename = (Boolean) obj.getOrDefault("is_dir_rename", false);
        this.isDirRename = isDirRename != null ? isDirRename: false;
    }

    public Boolean isValid () {
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
