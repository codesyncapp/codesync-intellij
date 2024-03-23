package org.intellij.sdk.codesync.database

import org.intellij.sdk.codesync.codeSyncSetup.CodeSyncSetup
import org.intellij.sdk.codesync.database.migrations.MigrationManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SQLiteConnectionTest {
    @AfterEach
    fun AfterEach(){
        //1. Disconnect database
        SQLiteConnection.getInstance().disconnect()
    }

    @Test
    fun validateSQLiteConnection(){
        // Getting connection instance first time.
        val connection = SQLiteConnection.getInstance().connection

        // Checking if connection is open.
        assertTrue(!connection.isClosed)

        // Closing existing connection.
        SQLiteConnection.getInstance().disconnect()

        // Checking if connection was closed.
        assertTrue(connection.isClosed)

        // Getting new instance and seeing if it is open.
        assertFalse(SQLiteConnection.getInstance().connection.isClosed)

        // Closing new instance.
        SQLiteConnection.getInstance().disconnect()
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
