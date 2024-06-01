package org.intellij.sdk.codesync.database

import org.intellij.sdk.codesync.codeSyncSetup.CodeSyncSetup
import org.intellij.sdk.codesync.database.migrations.MigrationManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class SQLiteConnectionTest {
    @AfterEach
    fun afterEach(){
        //1. Disconnect database
        SQLiteConnection.getInstance().disconnect()
    }

    @Test
    fun validateSQLiteConnection(){
        // Getting connection instance first time.
        val connection = SQLiteConnection.getInstance().connection

        // Checking if connection is open.
        Assertions.assertTrue(!connection.isClosed)

        // Closing existing connection.
        SQLiteConnection.getInstance().disconnect()

        // Checking if connection was closed.
        Assertions.assertTrue(connection.isClosed)

        // Getting new instance and seeing if it is open.
        Assertions.assertFalse(SQLiteConnection.getInstance().connection.isClosed)

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
