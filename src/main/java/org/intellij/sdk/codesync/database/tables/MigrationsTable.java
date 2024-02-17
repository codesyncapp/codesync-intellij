package org.intellij.sdk.codesync.database.tables;

import org.intellij.sdk.codesync.database.Database;
import org.intellij.sdk.codesync.database.enums.MigrationState;
import org.intellij.sdk.codesync.database.queries.MigrationsQueries;

import java.sql.ResultSet;
import java.sql.SQLException;

/*
    This class is used to interact with the Migrations table in the database.
*/
public class MigrationsTable extends DBTable {
    private final String tableName = "migrations";
    private final MigrationsQueries migrationsQueries;

    private static MigrationsTable instance;

    private MigrationsTable() {
        this.migrationsQueries = new MigrationsQueries(tableName);
    }

    public static MigrationsTable getInstance() {
        if (instance == null) {
            instance = new MigrationsTable();
        }
        return instance;
    }

    @Override
    public String getTableName() {
        return tableName;
    }

    @Override
    protected String getCreateTableQuery() {
        return this.migrationsQueries.getCreateTableQuery();
    }

    public MigrationState getMigrationState(String tableName) throws SQLException {
        ResultSet resultSet = Database.getInstance().query(this.migrationsQueries.getFetchMigrationQuery(tableName));
        if (resultSet.next()) {
            return MigrationState.valueOf(resultSet.getString("state"));
        }
        return MigrationState.NOT_STARTED;
    }

}
