package org.intellij.sdk.codesync.factories;

import org.json.simple.JSONObject;

/*
Factory class for getting the diffs of files for different events,
including file create event, file delete event, file rename event, file change event etc.
 */
public class DiffFactory {

    public static String getFileRenameDiff(String oldRelativePath, String relativeFilePath) {
        JSONObject diffObject = new JSONObject();
        diffObject.put("old_rel_path", oldRelativePath);
        diffObject.put("new_rel_path", relativeFilePath);

        return diffObject.toJSONString();
    }

    public static String getFileDeleteDiff() {
        JSONObject diffObject = new JSONObject();
        diffObject.put("is_deleted", true);
        diffObject.put("diff", null);  // Diff will be computed later while handling buffer.

        return diffObject.toJSONString();
    }

}
