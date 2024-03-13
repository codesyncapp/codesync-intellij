package org.intellij.sdk.codesync.database.migrations;

import com.google.common.io.CharStreams;
import com.intellij.openapi.application.ApplicationManager;
import org.intellij.sdk.codesync.CodeSyncLogger;
import org.intellij.sdk.codesync.database.enums.MigrationState;
import org.intellij.sdk.codesync.database.tables.MigrationsTable;
import org.intellij.sdk.codesync.database.tables.UserTable;
import org.intellij.sdk.codesync.exceptions.InvalidYmlFileError;
import org.intellij.sdk.codesync.exceptions.SQLiteDBConnectionError;
import org.intellij.sdk.codesync.exceptions.SQLiteDataError;
import org.intellij.sdk.codesync.database.models.UserAccount;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static org.intellij.sdk.codesync.Constants.USER_FILE_PATH;
public class MigrateUser implements Migration {

    private File userFile;
    private Map<String, Object> contentsMap;
    private Map<String, UserAccount> users;
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

        if(ApplicationManager.getApplication() == null){
            Path testDirPath = Paths.get(System.getProperty("user.dir"), "test_data").toAbsolutePath();
            Path userTestFile = Paths.get(testDirPath.toString(), "userTest.yml").toAbsolutePath();
            this.userFile = new File(userTestFile.toString());
        }else{
            this.userFile = new File(USER_FILE_PATH);
        }

        this.users = new HashMap<>();
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

    private void loadYmlContent () throws InvalidYmlFileError {
        if (this.contentsMap == null) {
            return;
        }
        try {
            for (Map.Entry<String, Object> userEntry : this.contentsMap.entrySet()) {
                if (userEntry.getValue() != null) {
                    Map<String, Object> userCredentials = (Map<String, Object>) userEntry.getValue();
                    this.users.put(userEntry.getKey(), new UserAccount(userEntry.getKey(), userCredentials));
                }
            }
        } catch (ClassCastException e){
            throw new InvalidYmlFileError(
                    String.format("User yml file \"%s\" is not valid. Error: %s", this.userFile.getPath(), e.getMessage())
            );
        }
    }

    public void migrateData(){
        try {
            readYml();
            loadYmlContent();
            for(String key : users.keySet()){
                UserTable.insertNewUser(users.get(key));
            }
        } catch (InvalidYmlFileError e) {
            CodeSyncLogger.critical("[INTELLIJ_MIGRATE_TO_SQLITE] Invalid yml file error while migrating user to SQLite: " + e.getMessage());
        } catch (FileNotFoundException e) {
            CodeSyncLogger.critical("[INTELLIJ_MIGRATE_TO_SQLITE] File not found error while migrating user to SQLite: " + e.getMessage());
        } catch (SQLiteDBConnectionError e) {
            CodeSyncLogger.critical("[INTELLIJ_MIGRATE_TO_SQLITE] SQL error while migrating user to SQLite: " + e.getMessage());
        } catch (SQLiteDataError e) {
            CodeSyncLogger.critical("[INTELLIJ_MIGRATE_TO_SQLITE] Database connection error while migrating user to SQLite: " + e.getMessage());
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
