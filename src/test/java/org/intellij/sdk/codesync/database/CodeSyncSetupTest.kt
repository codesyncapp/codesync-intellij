package org.intellij.sdk.codesync.database

import org.intellij.sdk.codesync.codeSyncSetup.CodeSyncSetup
import org.intellij.sdk.codesync.database.migrations.MigrationManager
import org.intellij.sdk.codesync.database.models.User
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class CodeSyncSetupTest {
    @Test
    fun validateSaveIamUserTest(){
        val email = "sample@gmail.com"
        CodeSyncSetup.saveIamUser(email, "ACCESS", "SECRET")

        val userFromDB = User.getTable().get(email)
        Assertions.assertTrue { userFromDB.isActive }
        Assertions.assertTrue { userFromDB.accessKey == "ACCESS" }
        Assertions.assertTrue { userFromDB.secretKey == "SECRET" }
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