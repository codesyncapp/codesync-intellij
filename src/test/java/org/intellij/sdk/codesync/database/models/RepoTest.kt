package org.intellij.sdk.codesync.database.models

import org.intellij.sdk.codesync.codeSyncSetup.CodeSyncSetup
import org.intellij.sdk.codesync.database.migrations.MigrationManager
import org.intellij.sdk.codesync.database.tables.RepoTable
import org.intellij.sdk.codesync.enums.RepoState
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


class RepoTest {
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

        // Make sure id attribute is populated after saving the repo.
        assert(repo.id != null)

        // Make sure the repo is saved in the database.
        val repoFromDb = RepoTable.getInstance().get(repo.path)
        assert(repoFromDb != null)
        assert(repoFromDb.id == repo.id)
        assert(repoFromDb.name == repo.name)
        assert(repoFromDb.path == repo.path)
        assert(repoFromDb.userId == repo.userId)
        assert(repoFromDb.state == repo.state)
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

        // Make sure id attribute is populated after saving the repo.
        assert(repo.id != null)

        val idBeforeSave = repo.id

        // Update the repo
        repo.state = RepoState.DISCONNECTED
        repo.save()

        // Make sure the repo is saved in the database.
        val repoFromDb = RepoTable.getInstance().get(repo.path)
        assert(repoFromDb != null)
        assert(repo.id == idBeforeSave)
        assert(repoFromDb.id == idBeforeSave)
        assert(repoFromDb.state == repo.state)
    }

    /*
    Validate the existing repo is updated when a new repo with the same path is saved.
     */
    @Test
    fun validateSaveWithExistingPath() {
        // Save a user
        val user = User(
            "test@codesync.com", "access-token", "access-key", "secrete-key", true
        )
        user.save()
        // Save a repo
        val repo = Repo(1, "test-repo", "/Users/codesync/dev/test-repo", user.id, RepoState.SYNCED)
        repo.save()

        // Make sure id attribute is populated after saving the repo.
        assert(repo.id != null)

        // Try to save a repo with the same path but different user or state.
        val repo2 = Repo(2, "test-repo-update", "/Users/codesync/dev/test-repo", user.id, RepoState.DISCONNECTED)
        repo2.save()

        // Make sure the repo is saved in the database. But user is only able to change state or user id.
        val repoFromDb = RepoTable.getInstance().get(repo.path)
        assert(repoFromDb != null)
        assert(repoFromDb.id == repo.id)
        assert(repoFromDb.state == RepoState.DISCONNECTED)
        assert(repoFromDb.userId == user.id)

        // Make sure name or server id is not updated.
        assert(repoFromDb.name != "test-repo-update")
        assert(repoFromDb.serverRepoId != 2)
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