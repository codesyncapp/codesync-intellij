package org.intellij.sdk.codesync.database.migrations;

import org.intellij.sdk.codesync.CodeSyncLogger;
import org.intellij.sdk.codesync.state.StateUtils;

import java.util.Timer;

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
        Timer timer = new Timer(true);
        CodeSyncLogger.debug("[DATABASE_MIGRATION] Running migrations async");
        timer.schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                CodeSyncLogger.debug("[DATABASE_MIGRATION] Running migrations");
                MigrationManager.getInstance().runMigrations();
                CodeSyncLogger.debug("[DATABASE_MIGRATION] Migrations complete");
                StateUtils.reloadState(StateUtils.getGlobalState().project);
            }
        }, 0);
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
