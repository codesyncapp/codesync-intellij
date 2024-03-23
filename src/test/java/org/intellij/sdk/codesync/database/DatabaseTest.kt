package org.intellij.sdk.codesync.database

import org.intellij.sdk.codesync.codeSyncSetup.CodeSyncSetup
import org.intellij.sdk.codesync.database.migrations.MigrationManager
import org.intellij.sdk.codesync.database.models.User
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.*


class DatabaseTest {

    @BeforeEach
    fun before() {}

    @AfterEach
    fun after() {}

    @Test
    fun validateReconnectingDB() {
        // Save a user
        val user = User(
            "test@codesync.com", "access-token", "access-key", "secrete-key", true
        )
        user.save()

        // Make sure id attribute is populated after saving the user.
        assertNotNull(user.id)

        // Close the database connection.
        SQLiteConnection.getInstance().disconnect()

        user.accessToken = "new-access"
        user.save()

        SQLiteConnection.getInstance().disconnect()

        val userFromDB = User.getTable().get(user.email)
        assert(userFromDB != null)
        assert(userFromDB.id == user.id)
        assert(userFromDB.email == user.email)
        assert(userFromDB.accessToken == user.accessToken)
        assert(userFromDB.accessKey == user.accessKey)
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
