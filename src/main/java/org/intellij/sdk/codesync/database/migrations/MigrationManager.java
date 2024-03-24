package org.intellij.sdk.codesync.database.migrations;

/*
Manager for handling migrations.
 */
public class MigrationManager {
    private static MigrationManager instance;

    private MigrationManager() {}

    public static MigrationManager getInstance() {
        if (instance == null) {
            instance = new MigrationManager();
        }
        return instance;
    }

    public void runMigrations() {
        MigrateUser.getInstance().migrate();
        MigrateRepo.getInstance().migrate();
    }
}
