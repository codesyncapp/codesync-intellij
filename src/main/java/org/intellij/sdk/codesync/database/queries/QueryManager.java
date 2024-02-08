package org.intellij.sdk.codesync.database.queries;

/*
    Base class containing queries common to all tables in the database.
*/
public abstract class QueryManager {
    private static QueryManager instance;

    QueryManager() {}

    public abstract String getCreateTableQuery();

    public static QueryManager getInstance() {
        if (instance == null) {
            instance = new QueryManager();
        }
        return instance;
    }

    public String getTableExistsQuery(String tableName){
        return String.format("SELECT name FROM sqlite_master WHERE type='table' AND name='%s'", tableName);
    }
}
