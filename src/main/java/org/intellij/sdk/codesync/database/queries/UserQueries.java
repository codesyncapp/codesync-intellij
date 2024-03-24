package org.intellij.sdk.codesync.database.queries;

public class UserQueries extends CommonQueries {
    String tableName;

    public UserQueries(String tableName) {
        this.tableName = tableName;
    }

    public String getCreateTableQuery() {
        return String.format("CREATE TABLE IF NOT EXISTS %s (", this.tableName) +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, email TEXT, access_token TEXT, access_key TEXT, secret_key TEXT, is_active BOOLEAN" +
            ")";
    }

    /*
        This method returns the query to insert a new user into the database.
        email must not be null. Others can be null.
     */
    public String getInsertQuery(String email, String accessToken, String accessKey, String secretKey, boolean isActive) {
        return String.format(
            "INSERT INTO %s (email, access_token, access_key, secret_key, is_active) VALUES (%s, %s, %s, %s, %s)",
                this.tableName,
                String.format("'%s'", email),
                accessToken == null ? "NULL" : String.format("'%s'", accessToken),
                accessKey == null ? "NULL" : String.format("'%s'", accessKey),
                secretKey == null ? "NULL" : String.format("'%s'", secretKey),
                isActive ? "1" : "0"
        );
    }

    public String getSelectQuery(String email) {
        return String.format("SELECT * FROM %s WHERE email = '%s';", this.tableName, email);
    }

    public String getSelectQuery(Integer userId) {
        return String.format("SELECT * FROM %s WHERE id = '%s';", this.tableName, userId);
    }

    public String getSelectActiveQuery() {
        return String.format("SELECT * FROM %s WHERE is_active = 1;", this.tableName);
    }

    public String getUpdateQuery(Integer id, String email, String accessToken, String accessKey, String secretKey, boolean isActive) {
        return String.format(
            "UPDATE %s SET email = %s, access_token = %s, access_key = %s, secret_key = %s, is_active = %s WHERE id = %s",
            this.tableName,
            String.format("'%s'", email),
            accessToken == null ? "NULL" : String.format("'%s'", accessToken),
            accessKey == null ? "NULL" : String.format("'%s'", accessKey),
            secretKey == null ? "NULL" : String.format("'%s'", secretKey),
            isActive ? "1" : "0",
            id
        );
    }

    /*
    Get the query to mark the all users other than the given as in-active.
    This is needed because we want to make sure that only one user is active at a time.
    */
    public String getMarkInActiveQuery(Integer id) {
        return String.format("UPDATE %s SET is_active = 0 WHERE id != %s", this.tableName, id);
    }

    /*
    Get the query to mark all users as in-active.
     */
    public String getMarkAllInActiveQuery() {
        return String.format("UPDATE %s SET is_active = 0", this.tableName);
    }

}
