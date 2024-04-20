package org.intellij.sdk.codesync.database.migrations;

import org.intellij.sdk.codesync.state.StateUtils;

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

    public void runMigrationsAsync() {
        Thread thread = new Thread(() -> {
            MigrationManager.getInstance().runMigrations();
            StateUtils.reloadState(StateUtils.getGlobalState().project);
        });
        thread.start();
    }

    /*
    Populate the cache with the repos being synced.
     */
    public void populateCache() {
        MigrateRepo.getInstance().populateReposBeingSynced();
    }

    public void runMigrations() {
        MigrateUser.getInstance().migrate();
        MigrateRepo.getInstance().migrate();
    }
}
