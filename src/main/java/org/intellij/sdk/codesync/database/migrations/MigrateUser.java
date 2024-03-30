package org.intellij.sdk.codesync.database.migrations;

import com.google.common.io.CharStreams;
import org.intellij.sdk.codesync.CodeSyncLogger;
import org.intellij.sdk.codesync.database.enums.MigrationState;
import org.intellij.sdk.codesync.database.models.User;
import org.intellij.sdk.codesync.database.tables.MigrationsTable;
import org.intellij.sdk.codesync.database.tables.UserTable;
import org.intellij.sdk.codesync.exceptions.InvalidYmlFileError;
import org.intellij.sdk.codesync.utils.CommonUtils;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.*;
import java.sql.SQLException;
import java.util.Map;

import static org.intellij.sdk.codesync.Constants.USER_FILE_PATH;
public class MigrateUser implements Migration {

    private File userFile;
    private Map<String, Object> contentsMap;
    private static MigrateUser instance;
    private UserTable userTable;
    private MigrationsTable migrationsTable;

    public static MigrateUser getInstance() {
        if (instance == null) {
            instance = new MigrateUser();
        }
        return instance;
    }

    private MigrateUser(){
        this.userTable = UserTable.getInstance();
        this.migrationsTable = MigrationsTable.getInstance();
        this.userFile = new File(USER_FILE_PATH);
    }

    private void readYml() throws FileNotFoundException, InvalidYmlFileError {
        Yaml yaml = new Yaml();
        InputStream inputStream;
        inputStream = new FileInputStream(userFile);
        String text = null;

        try (Reader reader = new InputStreamReader(inputStream)) {
            text = CharStreams.toString(reader);
            this.contentsMap = yaml.load(text);
        } catch (IOException | YAMLException e) {
            throw new InvalidYmlFileError(e.getMessage());
        }
    }

    public void migrateData() {
        try {
            readYml();

            if (this.contentsMap == null) {
                return;
            }

            for (Map.Entry<String, Object> userEntry : this.contentsMap.entrySet()) {
                if (userEntry.getValue() != null) {
                    Map<String, Object> userCredentials = (Map<String, Object>) userEntry.getValue();
                    User user = new User(
                        userEntry.getKey(),
                        (String) userCredentials.getOrDefault("access_token", null),
                        (String) userCredentials.getOrDefault("access_key", null),
                        (String) userCredentials.getOrDefault("secret_key", null),
                        CommonUtils.getBoolValue(userCredentials, "is_active", false)
                    );
                    user.save();
                }
            }
        } catch (InvalidYmlFileError | FileNotFoundException | SQLException e) {
            try {
                setMigrationState(MigrationState.ERROR);
            } catch (SQLException ex) {
                CodeSyncLogger.critical(String.format(
                    "[DATABASE_MIGRATION] Error '%s' while setting migration state for error: %s",
                    ex.getMessage(),
                    e.getMessage()
                ));
            }
            CodeSyncLogger.critical("[DATABASE_MIGRATION] SQL error while migrating User table: " + e.getMessage());
        }
    }

    private void createUserTable() throws SQLException {
        this.userTable.createTable();
    }

    private MigrationState checkMigrationState() throws SQLException {
        if (!this.migrationsTable.exists()) {
            this.migrationsTable.createTable();
            return MigrationState.NOT_STARTED;
        }
        return this.migrationsTable.getMigrationState(this.userTable.getTableName());
    }

    private void setMigrationState(MigrationState migrationState) throws SQLException {
        this.migrationsTable.setMigrationState(this.userTable.getTableName(), migrationState);
    }

    @Override
    public void migrate() {
        try {
            switch (checkMigrationState()) {
                case NOT_STARTED:
                case ERROR:
                    setMigrationState(MigrationState.IN_PROGRESS);
                    createUserTable();
                    migrateData();
                    setMigrationState(MigrationState.DONE);
                    CodeSyncLogger.info("[DATABASE_MIGRATION] [DONE] User table migration complete.");
                    break;
                case IN_PROGRESS:
                case DONE:
                    break;
            }
        } catch (SQLException e) {
            try {
                setMigrationState(MigrationState.ERROR);
            } catch (SQLException ex) {
                CodeSyncLogger.critical(String.format(
                    "[DATABASE_MIGRATION] Error '%s' while setting migration state for error: %s",
                    ex.getMessage(),
                    e.getMessage()
                ));
            }
            CodeSyncLogger.critical("[DATABASE_MIGRATION] SQL error while migrating User table: " + e.getMessage());
        }
    }
}
