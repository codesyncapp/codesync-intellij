package org.intellij.sdk.codesync.database.models

import org.intellij.sdk.codesync.codeSyncSetup.CodeSyncSetup
import org.intellij.sdk.codesync.database.migrations.MigrationManager
import org.intellij.sdk.codesync.database.tables.RepoBranchTable
import org.intellij.sdk.codesync.enums.RepoState
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


class RepoBranchTest {
    @BeforeEach
    fun before() {}

    @AfterEach
    fun after() {}

    /*
    Make sure user can create a repo using save.
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

        // Make sure the repo is saved in the database.
        assert(repoBranch.id != null)


        // Make sure the repo is saved in the database.
        val repoBranchFromDb = RepoBranchTable.getInstance().get(repoBranch.name, repo.id)
        assert(repoBranchFromDb != null)
        assert(repoBranchFromDb.id == repoBranch.id)
        assert(repoBranchFromDb.name == repoBranch.name)
        assert(repoBranchFromDb.repoId == repoBranch.repoId)
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

        val idBeforeSave = repoBranch.id
        // Update the repo
        repoBranch.repoId = 2
        repoBranch.name = "develop"
        repoBranch.save()

        // Make sure the repo is saved in the database.
        val repoBranchFromDb = RepoBranchTable.getInstance().get("develop", 2)
        assert(repoBranchFromDb != null)
        assert(repoBranchFromDb.id == idBeforeSave)
        assert(repoBranch.id == idBeforeSave)
        assert(repoBranch.name == "develop")
        assert(repoBranch.repoId == 2)
    }

    /*
    Validate the existing repo is updated when a new repo with the same path is saved.
     */
    @Test
    fun validateSaveWithExistingNameAndRepoID() {
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

        val repoBranch2 = RepoBranch("master", repo.id)
        repoBranch2.save()

        assert(repoBranch.id == repoBranch2.id)
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun setup(): Unit {
            CodeSyncSetup.createSystemDirectories()
            // Create the tables in the database. There is no data in the config file so empty tables will be created.
            MigrationManager.getInstance().runMigrations()
        }
    }

}