package org.intellij.sdk.codesync.database.tables;

import org.intellij.sdk.codesync.database.SQLiteConnection;
import org.intellij.sdk.codesync.database.enums.MigrationState;
import org.intellij.sdk.codesync.database.queries.MigrationsQueries;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/*
    This class is used to interact with the Migrations table in the database.
*/
public class MigrationsTable extends DBTable {
    private final String tableName = "migrations";

    // This is the identifier for the migration. It is used to identify the migration.
    // This will be incremented manually each time we are adding a new migration.
    private final String identifier = "1.0.0-feb-2024";
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
        try (Statement statement = SQLiteConnection.getInstance().getConnection().createStatement()) {
            ResultSet resultSet = statement.executeQuery(this.migrationsQueries.getFetchMigrationQuery(tableName, identifier));
            if (resultSet.isBeforeFirst()) {
                return MigrationState.fromString(resultSet.getString("state"));
            }
        }
        return MigrationState.NOT_STARTED;
    }

    public void setMigrationState(String tableName, MigrationState state) throws SQLException {
        try (Statement statement = SQLiteConnection.getInstance().getConnection().createStatement()) {
            ResultSet resultSet = statement.executeQuery(this.migrationsQueries.getFetchMigrationQuery(tableName, identifier));
            if (!resultSet.isBeforeFirst()) {
                statement.executeUpdate(this.migrationsQueries.getInsertMigrationQuery(tableName, identifier, state.toString()));
            } else {
                statement.executeUpdate(this.migrationsQueries.getUpdateMigrationQuery(tableName, identifier, state.toString()));
            }
        }
    }
}
