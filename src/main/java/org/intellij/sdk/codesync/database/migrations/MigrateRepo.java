package org.intellij.sdk.codesync.database.migrations;

import org.intellij.sdk.codesync.CodeSyncLogger;
import org.intellij.sdk.codesync.database.enums.MigrationState;
import org.intellij.sdk.codesync.database.models.Repo;
import org.intellij.sdk.codesync.database.models.RepoBranch;
import org.intellij.sdk.codesync.database.models.RepoFile;
import org.intellij.sdk.codesync.database.models.User;
import org.intellij.sdk.codesync.database.tables.*;
import org.intellij.sdk.codesync.enums.RepoState;
import org.intellij.sdk.codesync.exceptions.InvalidConfigFileError;
import org.intellij.sdk.codesync.exceptions.database.UserNotFound;
import org.intellij.sdk.codesync.files.ConfigFile;
import org.intellij.sdk.codesync.files.ConfigRepo;
import org.intellij.sdk.codesync.files.ConfigRepoBranch;
import org.intellij.sdk.codesync.utils.CommonUtils;

import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.intellij.sdk.codesync.Constants.CONFIG_PATH;

/*
    This class is used to migrate the database. This will include creating tables, adding columns, etc.
*/
public class MigrateRepo implements Migration {
    private static MigrateRepo instance;
    private RepoTable repoTable;
    private RepoBranchTable repoBranchTable;
    private RepoFileTable repoFileTable;
    private MigrationsTable migrationsTable;

    private Set<String> reposBeingMigrated = new HashSet<>();
    public static MigrateRepo getInstance() {
        if (instance == null) {
            instance = new MigrateRepo();
        }
        return instance;
    }

    // Get a list of repos that are being migrated. Useful to ignore operations on these while migration is in progress.
    public Set<String> getReposBeingMigrated() {
        return reposBeingMigrated;
    }

    private MigrateRepo() {
        this.repoTable = RepoTable.getInstance();
        this.migrationsTable = MigrationsTable.getInstance();
        this.repoBranchTable = RepoBranchTable.getInstance();
        this.repoFileTable = RepoFileTable.getInstance();
    }

    private void createTables() throws SQLException {
        this.repoTable.createTable();
        this.repoBranchTable.createTable();
        this.repoFileTable.createTable();
    }

    private MigrationState checkMigrationState() throws SQLException {
        if (!this.migrationsTable.exists()) {
            this.migrationsTable.createTable();
            return MigrationState.NOT_STARTED;
        }
        return this.migrationsTable.getMigrationState(this.repoTable.getTableName());
    }

    private void setMigrationState(MigrationState migrationState) throws SQLException {
        this.migrationsTable.setMigrationState(this.repoTable.getTableName(), migrationState);
    }

    private User getOrCreateUser(String email) throws SQLException {
        try {
            return UserTable.getInstance().get(email);
        } catch (UserNotFound e) {
            User user  = new User(email, null, null, null, true);
            user.save();
            return user;
        }
    }

    private RepoState getState(ConfigRepo configRepo) {
        if (configRepo.isInSync && !configRepo.isDisconnected && !configRepo.isDeleted) {
            return RepoState.SYNCED;
        } else if (configRepo.isDisconnected) {
            return RepoState.DISCONNECTED;
        } else if (configRepo.isDeleted) {
            return RepoState.DELETED;
        } else {
            return RepoState.NOT_SYNCED;
        }
    }

    /*
        Insert repos into the database. Also return the inserted repos with their ids.
    */
    private ArrayList<Repo> insertRepos(Map<String, ConfigRepo> repoMap) throws SQLException {
        ArrayList<Repo> reposToInsert = new ArrayList<>();
        Set<String> reposToIgnore = new HashSet<>();

        // Get existing repos in the database.
        ArrayList<String> repoPaths = new ArrayList<>(repoMap.keySet());
        ArrayList<Repo> existingRepos = Repo.getTable().findAll(repoPaths);
        for (Repo repo: existingRepos) {
            // Ignore repos that are present in the database.
            reposToIgnore.add(repo.getPath());
        }
        // Insert repo data, using bulk insert query.
        for (ConfigRepo configRepo : repoMap.values()) {
            String repoPath = configRepo.repoPath;
            if (reposToIgnore.contains(repoPath)) {
                continue;
            }
            // Add repo to the list of repos being migrated.
            reposBeingMigrated.add(repoPath);

            String repoName = Paths.get(repoPath).getFileName().toString();
            User user = getOrCreateUser(configRepo.email);
            Repo repo = new Repo(configRepo.id, repoName, configRepo.repoPath, user.getId(), getState(configRepo));
            reposToInsert.add(repo);
        }
        existingRepos.addAll(
            Repo.getTable().bulkInsert(reposToInsert)
        );
        return existingRepos;
    }

    private ArrayList<RepoBranch> insertBranches(Repo repo, Map<String, ConfigRepoBranch> branchMap) throws SQLException {
        ArrayList<RepoBranch> branchesToInsert = new ArrayList<>();
        Set<String> repoBranchesToIgnore = new HashSet<>();
        ArrayList<RepoBranch> existingBranches = RepoBranch.getTable().findAll(repo.getId(), new ArrayList<>(branchMap.keySet()));

        for (RepoBranch repoBranch: existingBranches) {
            repoBranchesToIgnore.add(repoBranch.getName());
        }

        for (ConfigRepoBranch configRepoBranch : branchMap.values()) {
            if (repoBranchesToIgnore.contains(configRepoBranch.branchName)) {
                // Ignore branches that are already present in the database.
                continue;
            }
            RepoBranch repoBranch = new RepoBranch(configRepoBranch.branchName, repo.getId());
            branchesToInsert.add(repoBranch);
        }

        existingBranches.addAll(
            RepoBranch.getTable().bulkInsert(branchesToInsert)
        );
        return existingBranches;
    }

    private void insertFiles(RepoBranch repoBranch, Map<String, Integer> files) throws SQLException {
        ArrayList<RepoFile> repoFiles = new ArrayList<>();
        Set<String> filesToIgnore = new HashSet<>();

        for (RepoFile repoFile: RepoFile.getTable().findAll(repoBranch.getId())) {
            filesToIgnore.add(repoFile.getPath());
        }

        for (Map.Entry<String, Integer> fileEntry : files.entrySet()) {
            if (filesToIgnore.contains(fileEntry.getKey())) {
                // Ignore files that are already present in the database.
                continue;
            }
            repoFiles.add(
                new RepoFile(fileEntry.getKey(), repoBranch.getId(), fileEntry.getValue())
            );
        }

        RepoFile.getTable().bulkInsert(repoFiles);
    }

    private void migrateData() throws InvalidConfigFileError, SQLException {
        ConfigFile configFile = new ConfigFile(CONFIG_PATH);

        // Insert Repos.
        CodeSyncLogger.info("[DATABASE_MIGRATION] Inserting Repos into the database.");
        ArrayList<Repo> repos = this.insertRepos(configFile.getRepos());
        CodeSyncLogger.info(String.format("[DATABASE_MIGRATION] Inserted %d Repos into the database.", repos.size()));

        for (Repo repo: repos) {
            CodeSyncLogger.info(String.format("[DATABASE_MIGRATION] Inserting branches for Repo: %s", repo.getPath()));
            ConfigRepo configRepo = configFile.getRepos().get(repo.getPath());
            ArrayList<RepoBranch> repoBranches = this.insertBranches(repo, configRepo.getRepoBranches());
            CodeSyncLogger.info(String.format("[DATABASE_MIGRATION] Inserted %d branches for Repo: %s", repoBranches.size(), repo.getPath()));
            for (RepoBranch repoBranch: repoBranches) {
                CodeSyncLogger.info(String.format("[DATABASE_MIGRATION] Inserting files for branch: %s", repoBranch.getName()));
                this.insertFiles(repoBranch, configRepo.getRepoBranches().get(repoBranch.getName()).getFiles());
                CodeSyncLogger.info(String.format("[DATABASE_MIGRATION] Inserted files for branch: %s", repoBranch.getName()));
            }
        }
    }

    @Override
    public void migrate() {
        try {
            switch (checkMigrationState()) {
                case NOT_STARTED:
                case ERROR:
                    setMigrationState(MigrationState.IN_PROGRESS);
                    createTables();
                    migrateData();
                    setMigrationState(MigrationState.DONE);
                    CodeSyncLogger.info("[DATABASE_MIGRATION] [DONE] Repo table migration complete.");
                    break;
                case IN_PROGRESS:
                case DONE:
                    break;
            }
        } catch (SQLException | InvalidConfigFileError e) {
            try {
                setMigrationState(MigrationState.ERROR);
            } catch (SQLException ex) {
                CodeSyncLogger.critical(String.format(
                    "[DATABASE_MIGRATION] Error '%s' while setting migration state for error: %s",
                    ex.getMessage(),
                    e.getMessage()
                ));
            }
            CodeSyncLogger.critical(
                String.format(
                    "[DATABASE_MIGRATION] SQL error while migrating Repo table: %s",
                    CommonUtils.getStackTrace(e)
                )
            );
        }

        // Clear repos being migrated set.
        reposBeingMigrated.clear();
    }

    public void populateReposBeingSynced() {
        try {
            ConfigFile configFile = new ConfigFile(CONFIG_PATH);
            for (ConfigRepo configRepo: configFile.getRepos().values()) {
                reposBeingMigrated.add(configRepo.repoPath);
            }
        } catch (InvalidConfigFileError e) {
            CodeSyncLogger.error("[DATABASE_MIGRATION] Error while populating repos being synced: " + e.getMessage());
        }
    }
}
