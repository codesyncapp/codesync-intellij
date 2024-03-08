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
                accessToken == null ? "NULL" : String.format("'%s'", accessKey),
                accessKey == null ? "NULL" : String.format("'%s'", accessKey),
                secretKey == null ? "NULL" : String.format("'%s'", secretKey),
                isActive ? "1" : "0"
        );
    }

    public String getSelectQuery(String email) {
        return String.format("SELECT * FROM %s WHERE email = '%s';", this.tableName, email);
    }

    public String getUpdateQuery() {
        return String.format("UPDATE %s SET email = ?, access_token = ?, access_key = ?, secret_key = ?, is_active = ? WHERE id = ?", this.tableName);
    }

    public String getDeleteQuery() {
        return String.format("DELETE FROM %s WHERE id = ?", this.tableName);
    }
}
