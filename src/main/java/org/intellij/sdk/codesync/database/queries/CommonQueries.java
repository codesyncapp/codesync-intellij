package org.intellij.sdk.codesync.database.queries;

public class CommonQueries {
    public String getTableExistsQuery(String tableName){
        return String.format("SELECT name FROM sqlite_master WHERE type='table' AND name='%s'", tableName);
    }
}
