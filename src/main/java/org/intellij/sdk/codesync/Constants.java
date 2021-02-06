package org.intellij.sdk.codesync;

public final class Constants {

    private Constants() {
        // restrict instantiation
    }

    public static String CODESYNC_ROOT = "/usr/local/bin/.codesync";
    public static String DIFFS_REPO = String.format("%s/.diffs", CODESYNC_ROOT);
    public static String ORIGINALS_REPO = String.format("%s/.originals", CODESYNC_ROOT);
    public static String CONFIG_PATH = String.format("%s/config.yml", CODESYNC_ROOT);

    public static String DEFAULT_BRANCH = "default";
    public static String GIT_REPO = ".git";
    public static String CURRENT_GIT_BRANCH_COMMAND = "git rev-parse --abbrev-ref HEAD";

    public static String MAGIC_STRING = "IntellijIdeaRulezzz";
    public static String FILE_CREATE_EVENT = "VfsEvent[create file";
    public static String FILE_DELETE_EVENT = "VfsEvent[deleted: file";
    public static String FILE_RENAME_EVENT = "VfsEvent[property(name) changed";

}