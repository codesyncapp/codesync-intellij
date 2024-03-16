package org.intellij.sdk.codesync.database.migration

import CodeSyncTestUtils.getTestFilePath
import CodeSyncTestUtils.setupCodeSyncDirectory
import org.intellij.sdk.codesync.Constants
import org.intellij.sdk.codesync.database.enums.MigrationState
import org.intellij.sdk.codesync.database.migrations.MigrationManager
import org.intellij.sdk.codesync.database.tables.MigrationsTable
import org.intellij.sdk.codesync.database.tables.RepoBranchTable
import org.intellij.sdk.codesync.database.tables.RepoFileTable
import org.intellij.sdk.codesync.database.tables.RepoTable
import org.intellij.sdk.codesync.database.tables.UserTable
import org.intellij.sdk.codesync.enums.RepoState
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Paths
import kotlin.test.assertEquals

/*
This will test the complete end-to-end flow of migration manager.
*/
class MigrationManagerTest {

    @BeforeEach
    fun before() {
        // Make sure the test directory is empty.
        setupCodeSyncDirectory(Constants.CODESYNC_ROOT)
    }

    @AfterEach
    fun after(){

    }

    /*
    This will test the complete end-to-end flow of migration manager.
    */
    @Test
    fun validateMigration() {
        // copy data to config.yml file
        org.apache.commons.io.FileUtils.copyFile(
            getTestFilePath("config-for-migration.yml").toFile(),
            Paths.get(Constants.CONFIG_PATH).toFile()
        )
        org.apache.commons.io.FileUtils.copyFile(
            getTestFilePath("user-for-migration.yml").toFile(),
            Paths.get(Constants.USER_FILE_PATH).toFile()
        )

        // validate the tables do not exist.
        assert(!MigrationsTable.getInstance().exists())
        assert(!RepoTable.getInstance().exists())
        assert(!RepoBranchTable.getInstance().exists())
        assert(!RepoFileTable.getInstance().exists())
        assert(!UserTable.getInstance().exists())

        // Run the migration.
        MigrationManager.getInstance().runMigrations()

        // Validate the data now.

        // validate the tables are present.
        assert(MigrationsTable.getInstance().exists())
        assert(RepoTable.getInstance().exists())
        assert(RepoBranchTable.getInstance().exists())
        assert(RepoFileTable.getInstance().exists())
        assert(UserTable.getInstance().exists())

        // Make sure there are 2 entries in the migration table.
        assertEquals(
            MigrationsTable.getInstance().getMigrationState(UserTable.getInstance().tableName),
            MigrationState.DONE
        )
        assertEquals(
            MigrationsTable.getInstance().getMigrationState(RepoTable.getInstance().tableName),
            MigrationState.DONE
        )

        val expectedRepos = listOf(
            "/Users/codesync/dev/test-repo-1",
            "/Users/codesync/dev/test-repo-2",
            "/Users/codesync/dev/test-repo-3",
            "/Users/codesync/dev/test-repo-4",
        )

        // Validate the data in the repo table.
        for (repo in expectedRepos) {
            val repoData = RepoTable.getInstance().get(repo)
            assert(repoData != null)
            assertEquals(repoData.path, repo)
        }

        val expectedUsers = listOf(
            "test@codesync.com",
            "test2@codesync.com",
        )

        // Validate the data in user table.
        for (user in expectedUsers) {
            val userData = UserTable.getInstance().get(user)
            assert(userData != null)
            assertEquals(userData.email, user)
        }

        // validate data for the first user
        val user1 = UserTable.getInstance().get(expectedUsers[0])
        assert(user1 != null)
        assertEquals(user1.email, expectedUsers[0])
        assertEquals(user1.isActive, true)
        assertEquals(user1.accessKey, "test-access-key")
        assertEquals(user1.accessToken, "test-access-token")
        assertEquals(user1.secretKey, "test-secret-key")

        // validate data for the second user
        val user2= UserTable.getInstance().get(expectedUsers[1])
        assert(user2 != null)
        assertEquals(user2.email, expectedUsers[1])
        assertEquals(user2.isActive, false)
        assertEquals(user2.accessKey, "test-2-access-key")
        assertEquals(user2.accessToken, "test-2-access-token")
        assertEquals(user2.secretKey, "test-2-secret-key")

        // Validate the first repo and related data.
        val repo1 = RepoTable.getInstance().get(expectedRepos[0])
        assert(repo1 != null)
        assertEquals(repo1.path, expectedRepos[0])
        assertEquals(repo1.name, "test-repo-1")
        assertEquals(repo1.serverRepoId, 11)
        assertEquals(repo1.state, RepoState.SYNCED)

        // Validate the second repo and related data.
        val repo2 = RepoTable.getInstance().get(expectedRepos[1])
        assert(repo2 != null)
        assertEquals(repo2.path, expectedRepos[1])
        assertEquals(repo2.name, "test-repo-2")
        assertEquals(repo2.serverRepoId, 22)
        assertEquals(repo2.state, RepoState.DISCONNECTED)

        // Validate the second repo and related data.
        val repo3 = RepoTable.getInstance().get(expectedRepos[2])
        assert(repo3 != null)
        assertEquals(repo3.path, expectedRepos[2])
        assertEquals(repo3.name, "test-repo-3")
        assertEquals(repo3.serverRepoId, 33)
        assertEquals(repo3.state, RepoState.DISCONNECTED)

        // Validate the second repo and related data.
        val repo4 = RepoTable.getInstance().get(expectedRepos[3])
        assert(repo4 != null)
        assertEquals(repo4.path, expectedRepos[3])
        assertEquals(repo4.name, "test-repo-4")
        assertEquals(repo4.serverRepoId, 44)
        assertEquals(repo4.state, RepoState.DELETED)

    }
}
