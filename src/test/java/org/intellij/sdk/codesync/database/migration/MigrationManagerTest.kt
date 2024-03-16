package org.intellij.sdk.codesync.database.migration

import CodeSyncTestUtils.setupCodeSyncDirectory
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/*
This will test the complete end-to-end flow of migration manager.
*/
class MigrationManagerTest {

    @BeforeEach
    fun before() {
        // Make sure the test directory is empty.
        setupCodeSyncDirectory()
    }

    @AfterEach
    fun after(){

    }

    @Test
    fun validateLoadYmlContent(){

    }

}
