package org.intellij.sdk.codesync.database.migration

import CodeSyncTestUtils.deleteTables
import CodeSyncTestUtils.getTestFilePath
import org.intellij.sdk.codesync.Constants
import org.intellij.sdk.codesync.codeSyncSetup.CodeSyncSetup
import org.intellij.sdk.codesync.database.enums.MigrationState
import org.intellij.sdk.codesync.database.migrations.MigrationManager
import org.intellij.sdk.codesync.database.tables.MigrationsTable
import org.intellij.sdk.codesync.database.tables.RepoBranchTable
import org.intellij.sdk.codesync.database.tables.RepoFileTable
import org.intellij.sdk.codesync.database.tables.RepoTable
import org.intellij.sdk.codesync.database.tables.UserTable
import org.intellij.sdk.codesync.enums.RepoState
import org.junit.jupiter.api.*
import java.nio.file.Paths
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/*
This will test the complete end-to-end flow of migration manager.
*/
class MigrationManagerTest {

    @BeforeEach
    fun before() {}

    @AfterEach
    fun after() {}

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

        // validate the tables are not present.
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
        assertEquals(repo1!!.user.email, "test@codesync.com")

        // Validate the branches for the first repo.
        val repo1Branches = repo1.branches.sortedWith(compareBy { it.name })
        assertEquals(repo1Branches.size, 2)
        assertEquals(repo1Branches[0].name, "test-branch-1")
        assertEquals(repo1Branches[1].name, "test-branch-2")

        // Assert files inside the branch
        val repo1branch1Files = repo1Branches[0].files.sortedWith(compareBy { it.path })
        val repo1branch2Files = repo1Branches[1].files.sortedWith(compareBy { it.path })
        assertEquals(repo1branch1Files.size, 6)
        assertEquals(repo1branch2Files.size, 6)

        assertEquals(repo1branch1Files.map { file -> file.serverFileId }, listOf(11, 12, 13, 14, 15, 16))
        assertEquals(repo1branch1Files.map { file -> file.path }, listOf("test-1.txt", "test-2.txt", "test-3.txt", "test-4.txt", "test-5.txt", "test-6.txt"))
        assertEquals(repo1branch2Files.map { file -> file.serverFileId }, listOf(21, 22, 23, 24, 25, 26))
        assertEquals(repo1branch2Files.map { file -> file.path }, listOf("test-1.txt", "test-2.txt", "test-3.txt", "test-4.txt", "test-5.txt", "test-6.txt"))

        // Make sure same file is not used in different branches
        assertNotEquals(
            repo1branch1Files.map { file -> file.id },
            repo1branch2Files.map { file -> file.id },
        )

        // Validate the second repo and related data.
        val repo2 = RepoTable.getInstance().get(expectedRepos[1])
        assert(repo2 != null)
        assertEquals(repo2.path, expectedRepos[1])
        assertEquals(repo2.name, "test-repo-2")
        assertEquals(repo2.serverRepoId, 22)
        assertEquals(repo2.state, RepoState.DISCONNECTED)
        assertEquals(repo2!!.user.email, "test@codesync.com")

        // Validate the branches for the second repo.
        val repo2Branches = repo2.branches.sortedWith(compareBy { it.name })
        assertEquals(repo2Branches.size, 3)
        assertEquals(repo2Branches[0].name, "test-branch-1")
        assertEquals(repo2Branches[1].name, "test-branch-2")
        assertEquals(repo2Branches[2].name, "test-branch-3")

        // Assert files inside the branch
        val repo2branch1Files = repo2Branches[0].files.sortedWith(compareBy { it.path })
        val repo2branch2Files = repo2Branches[1].files.sortedWith(compareBy { it.path })
        val repo2branch3Files = repo2Branches[2].files.sortedWith(compareBy { it.path })
        assertEquals(repo2branch1Files.size, 6)
        assertEquals(repo2branch2Files.size, 6)
        assertEquals(repo2branch3Files.size, 6)

        assertEquals(repo2branch1Files.map { file -> file.serverFileId }, listOf(211, 212, 213, 214, 215, 216))
        assertEquals(repo2branch1Files.map { file -> file.path }, listOf("test-1.txt", "test-2.txt", "test-3.txt", "test-4.txt", "test-5.txt", "test-6.txt"))
        assertEquals(repo2branch2Files.map { file -> file.serverFileId }, listOf(221, 222, 223, 224, 225, 226))
        assertEquals(repo2branch2Files.map { file -> file.path }, listOf("test-1.txt", "test-2.txt", "test-3.txt", "test-4.txt", "test-5.txt", "test-6.txt"))
        assertEquals(repo2branch3Files.map { file -> file.serverFileId }, listOf(321, 322, 323, 324, 325, 326))
        assertEquals(repo2branch3Files.map { file -> file.path }, listOf("test-1.txt", "test-2.txt", "test-3.txt", "test-4.txt", "test-5.txt", "test-6.txt"))

        // Make sure same file is not used in different branches
        assertNotEquals(
            repo2branch1Files.map { file -> file.id },
            repo2branch2Files.map { file -> file.id },
        )
        assertNotEquals(
            repo2branch2Files.map { file -> file.id },
            repo2branch3Files.map { file -> file.id },
        )
        assertNotEquals(
            repo2branch1Files.map { file -> file.id },
            repo2branch3Files.map { file -> file.id },
        )

        // Validate the second repo and related data.
        val repo3 = RepoTable.getInstance().get(expectedRepos[2])
        assert(repo3 != null)
        assertEquals(repo3.path, expectedRepos[2])
        assertEquals(repo3.name, "test-repo-3")
        assertEquals(repo3.serverRepoId, 33)
        assertEquals(repo3.state, RepoState.DISCONNECTED)
        assertEquals(repo3!!.user.email, "test3@codesync.com")

        // Validate the branches for the first repo.
        val repo3Branches = repo3.branches.sortedWith(compareBy { it.name })
        assertEquals(repo3Branches.size, 2)
        assertEquals(repo3Branches[0].name, "test-branch-31")
        assertEquals(repo3Branches[1].name, "test-branch-32")

        // Assert files inside the branch
        val repo3branch1Files = repo3Branches[0].files.sortedWith(compareBy { it.path })
        val repo3branch2Files = repo3Branches[1].files.sortedWith(compareBy { it.path })
        assertEquals(repo3branch1Files.size, 3)
        assertEquals(repo3branch2Files.size, 2)

        assertEquals(repo3branch1Files.map { file -> file.serverFileId }, listOf(311, 312, 313))
        assertEquals(repo3branch1Files.map { file -> file.path }, listOf("test-repo-3-1.txt", "test-repo-3-2.txt", "test-repo-3-3.txt"))
        assertEquals(repo3branch2Files.map { file -> file.serverFileId }, listOf(321, 322))
        assertEquals(repo3branch2Files.map { file -> file.path }, listOf("test-repo-3-1.txt", "test-repo-3-2.txt"))

        // Make sure same file is not used in different branches
        assertNotEquals(
            repo3branch1Files.map { file -> file.id },
            repo3branch2Files.map { file -> file.id },
        )

        // Validate the second repo and related data.
        val repo4 = RepoTable.getInstance().get(expectedRepos[3])
        assert(repo4 != null)
        assertEquals(repo4.path, expectedRepos[3])
        assertEquals(repo4.name, "test-repo-4")
        assertEquals(repo4.serverRepoId, 44)
        assertEquals(repo4.state, RepoState.DELETED)
        assertEquals(repo4!!.user.email, "test4@codesync.com")

        // Validate the branches for the first repo.
        val repo4Branches = repo4.branches.sortedWith(compareBy { it.name })
        assertEquals(repo4Branches.size, 1)
        assertEquals(repo4Branches[0].name, "test-branch-44")

        // Assert files inside the branch
        val repo4branch1Files = repo4Branches[0].files.sortedWith(compareBy { it.path })
        assertEquals(repo4branch1Files.size, 6)

        assertEquals(repo4branch1Files.map { file -> file.serverFileId }, listOf(411, 412, 413, 414, 415, 416))
        assertEquals(repo4branch1Files.map { file -> file.path }, listOf("test-repo-4-1.txt", "test-repo-4-2.txt", "test-repo-4-3.txt", "test-repo-4-4.txt", "test-repo-4-5.txt", "test-repo-4-6.txt"))
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun setup(): Unit {
            CodeSyncSetup.createSystemDirectories()
            deleteTables()
        }

        @JvmStatic
        @AfterAll
        fun cleanup(): Unit {
            deleteTables()
        }
    }
}
