package org.intellij.sdk.codesync.database.migrations;

import java.io.File;

/*
Manager for handling migrations.
 */
public class MigrationManager {
    private static MigrationManager instance;

    private MigrationManager() {
    }

    public static MigrationManager getInstance() {
        if (instance == null) {
            instance = new MigrationManager();
        }
        return instance;
    }

    private void createDBFiles(String  databasePath) {
        File file = new File(databasePath);
        if(file.exists()){
            return;
        }

    }

    public void runMigrations(String  databasePath) {
//        createDBFiles(databasePath);
        MigrateUser.getInstance().migrate();
        MigrateRepo.getInstance().migrate();
    }
}
