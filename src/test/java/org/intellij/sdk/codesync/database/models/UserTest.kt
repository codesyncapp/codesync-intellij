package org.intellij.sdk.codesync.database.models

import org.intellij.sdk.codesync.codeSyncSetup.CodeSyncSetup
import org.intellij.sdk.codesync.database.migrations.MigrationManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


class UserTest {
    @BeforeEach
    fun before() {}

    @AfterEach
    fun after() {}

    /*
    Make sure user can create a user instance using save.
     */
    @Test
    fun validateCreate() {
        // Save a user
        val user = User(
            "test@codesync.com", "access-token", "access-key", "secrete-key", true
        )
        user.save()
        // Make sure id attribute is populated after saving the user.
        assert(user.id != null)

        // Make sure the user is saved in the database.
        val userFromDb = User.getTable().get(user.email)
        assert(userFromDb != null)
        assert(userFromDb.id == user.id)
        assert(userFromDb.email == user.email)
        assert(userFromDb.accessToken == user.accessToken)
        assert(userFromDb.accessKey == user.accessKey)
        assert(userFromDb.secretKey == user.secretKey)
        assert(userFromDb.isActive == user.isActive)
    }

    @Test
    fun validateUpdate() {
        // Save a user
        val user = User(
            "test@codesync.com", "access-token", null, null, true
        )
        user.save()
        // Make sure id attribute is populated after saving the user.
        assert(user.id != null)

        val idBeforeSave = user.id

        user.accessKey = "access-key"
        user.secretKey = "secret-key"
        user.save()

        // Make sure the user is saved in the database.
        val userFromDB = User.getTable().get(user.id)
        assert(userFromDB != null)
        assert(user.id == idBeforeSave)
        assert(userFromDB.id == idBeforeSave)
        assert(userFromDB.accessKey == user.accessKey)
        assert(userFromDB.secretKey == user.secretKey)
    }

    /*
    Validate the existing user is updated when a new user with the same email is saved.
     */
    @Test
    fun validateSaveWithExistingPath() {
        // Save a user
        val user = User(
            "test@codesync.com", "access-token", "access-key", "secrete-key", true
        )
        user.save()

        // Make sure id attribute is populated after saving the user.
        assert(user.id != null)

        // Try to save a user with the same email but different access token.
        val user2 = User(
            "test@codesync.com", "access-token-updated", "access-key", "secrete-key", true
        )
        user2.save()

        assert(user2.id != null)
        assert(user2.id == user.id)

        // Make sure the user is saved in the database.
        val userFromDB = User.getTable().get(user.email)
        assert(userFromDB != null)
        assert(userFromDB.id == user.id)
        assert(userFromDB.accessToken == "access-token-updated")
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