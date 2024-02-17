package org.intellij.sdk.codesync.database.migrations;

import org.intellij.sdk.codesync.database.enums.MigrationState;
import org.intellij.sdk.codesync.database.models.Repo;
import org.intellij.sdk.codesync.database.models.User;
import org.intellij.sdk.codesync.database.tables.MigrationsTable;
import org.intellij.sdk.codesync.database.tables.RepoTable;
import org.intellij.sdk.codesync.database.tables.UserTable;
import org.intellij.sdk.codesync.exceptions.InvalidConfigFileError;
import org.intellij.sdk.codesync.files.ConfigFile;
import org.intellij.sdk.codesync.files.ConfigRepo;

import java.nio.file.Paths;
import java.sql.SQLException;

import static org.intellij.sdk.codesync.Constants.CONFIG_PATH;

/*
    This class is used to migrate the database. This will include creating tables, adding columns, etc.
*/
public class MigrateRepo implements Migration {
    private static MigrateRepo instance;
    private RepoTable repoTable;
    private MigrationsTable migrationsTable;
    MigrationState migrationState = null;
    public static MigrateRepo getInstance() {
        if (instance == null) {
            instance = new MigrateRepo();
        }
        return instance;
    }

    private MigrateRepo() {
        this.repoTable = RepoTable.getInstance();
        this.migrationsTable = MigrationsTable.getInstance();
    }

    private void createRepoTable() throws SQLException {
        this.migrationsTable.createTable();
    }

    private MigrationState checkMigrationState() throws SQLException {
        if (!this.migrationsTable.exists()) {
            this.migrationsTable.createTable();
            return MigrationState.NOT_STARTED;
        }
        return this.migrationsTable.getMigrationState(this.repoTable.getTableName());
    }

    private User getOrCreateUser(String email) throws SQLException {
        User user = UserTable.getInstance().get(email);
        if (user == null) {
            user = UserTable.getInstance().insert(
                    new User(email, null, null, null, true)
            );
        }
        return user;
    }

    private void migrateData() throws InvalidConfigFileError, SQLException {
        ConfigFile configFile = new ConfigFile(CONFIG_PATH);
        for (ConfigRepo configRepo : configFile.getRepos().values()) {
            String repoPath = configRepo.repoPath;
            String repoName = Paths.get(repoPath).getFileName().toString();
            User user = getOrCreateUser(configRepo.email);
            Repo repo = new Repo(repoName, configRepo.repoPath, user.getId(), null);
        }
    }

    @Override
    public void migrate() {
        try {
            switch (checkMigrationState()) {
                case NOT_STARTED:
                case ERROR:
                    createRepoTable();
                    break;
                case IN_PROGRESS:
                case DONE:
                    break;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
