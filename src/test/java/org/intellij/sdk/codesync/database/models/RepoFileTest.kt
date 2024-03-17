package org.intellij.sdk.codesync.database.models

import CodeSyncTestUtils.setupCodeSyncDirectory
import org.intellij.sdk.codesync.Constants
import org.intellij.sdk.codesync.database.migrations.MigrationManager
import org.intellij.sdk.codesync.database.tables.RepoBranchTable
import org.intellij.sdk.codesync.database.tables.RepoFileTable
import org.intellij.sdk.codesync.enums.RepoState
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


class RepoFileTest {
    @BeforeEach
    fun before() {}

    @AfterEach
    fun after() {}

    /*
    Make sure user can create a repo file using save.
     */
    @Test
    fun validateCreate() {
        // Save a user
        val user = User(
            "test@codesync.com", "access-token", "access-key", "secrete-key", true
        )
        user.save()
        // Save a repo
        val repo = Repo(1, "test-repo", "/Users/codesync/dev/test-repo", user.id, RepoState.SYNCED)
        repo.save()
        assert(repo.id != null)

        val repoBranch = RepoBranch("master", repo.id)
        repoBranch.save()
        assert(repoBranch.id != null)

        val repoFile = RepoFile("test-file", repoBranch.id, 123)
        repoFile.save()

        // assert the repo file is saved in the database.
        assert(repoFile.id != null)

        // Make sure the repo is saved in the database.
        val repoBranchFileFromDb = RepoFileTable.getInstance().get("test-file", repoBranch.id)
        assert(repoBranchFileFromDb != null)
        assert(repoBranchFileFromDb.id == repoFile.id)
        assert(repoBranchFileFromDb.path == repoFile.path)
        assert(repoBranchFileFromDb.repoBranchId == repoBranch.id)
        assert(repoBranchFileFromDb.serverFileId == 123)
    }

    @Test
    fun validateUpdate() {
        // Save a user
        val user = User(
            "test@codesync.com", "access-token", "access-key", "secrete-key", true
        )
        user.save()
        // Save a repo
        val repo = Repo(1, "test-repo", "/Users/codesync/dev/test-repo", user.id, RepoState.SYNCED)
        repo.save()
        assert(repo.id != null)

        val repoBranch = RepoBranch("master", repo.id)
        repoBranch.save()
        assert(repoBranch.id != null)

        val repoFile = RepoFile("test-file", repoBranch.id, 123)
        repoFile.save()
        assert(repoFile.id != null)

        val idBeforeSave = repoFile.id

        repoFile.serverFileId = 124
        repoFile.save()

        // Make sure the repo is saved in the database.
        val repoBranchFileFromDb = RepoFileTable.getInstance().get("test-file", repoBranch.id)
        assert(repoBranchFileFromDb != null)
        assert(repoBranchFileFromDb.id == idBeforeSave)
        assert(repoFile.id == idBeforeSave)
        assert(repoBranchFileFromDb.path == repoFile.path)
        assert(repoBranchFileFromDb.repoBranchId == repoBranch.id)
        assert(repoBranchFileFromDb.serverFileId == 124)
    }

    /*
    Validate the existing repo file is updated when a new repo file with the same path is saved.
     */
    @Test
    fun validateSaveWithExistingPathAndBranchId() {
        // Save a user
        val user = User(
            "test@codesync.com", "access-token", "access-key", "secrete-key", true
        )
        user.save()
        // Save a repo
        val repo = Repo(1, "test-repo", "/Users/codesync/dev/test-repo", user.id, RepoState.SYNCED)
        repo.save()
        assert(repo.id != null)

        val repoBranch = RepoBranch("master", repo.id)
        repoBranch.save()
        assert(repoBranch.id != null)

        val repoFile = RepoFile("test-file", repoBranch.id, 123)
        repoFile.save()
        assert(repoFile.id != null)

        val repoFile2 = RepoFile("test-file", repoBranch.id, 130)
        repoFile2.save()
        assert(repoFile2.id != null)
        assert(repoFile.id == repoFile2.id)


        // Make sure the repo is saved in the database.
        val repoBranchFileFromDb = RepoFileTable.getInstance().get("test-file", repoBranch.id)
        assert(repoBranchFileFromDb != null)
        assert(repoBranchFileFromDb.id == repoFile.id)
        assert(repoBranchFileFromDb.path == repoFile.path)
        assert(repoBranchFileFromDb.repoBranchId == repoBranch.id)
        assert(repoBranchFileFromDb.serverFileId == 130)
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun setup(): Unit {
            // Make sure the test directory is empty.
            setupCodeSyncDirectory(Constants.CODESYNC_ROOT)

            // Create the tables in the database. There is no data in the config file so empty tables will be created.
            MigrationManager.getInstance().runMigrations()
        }
    }

}