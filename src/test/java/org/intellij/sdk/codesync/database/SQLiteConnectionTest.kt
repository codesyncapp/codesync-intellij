package org.intellij.sdk.codesync.database

import CodeSyncTestUtils
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SQLiteConnectionTest {

    @AfterEach
    fun AfterEach(){
        //1. Disconnect database
        SQLiteConnection.getInstance().disconnect()

        //2. Remove test db file
        var file = File(CodeSyncTestUtils.getTestDBFilePath())
        file.delete()
    }

    @Test
    fun validateSQLiteConnection(){

        //Getting connection instance first time.
        var connection = SQLiteConnection.getInstance().connection

        //Checking if connection is open.
        assertTrue(!connection.isClosed)

        //Closing existing connection.
        SQLiteConnection.getInstance().disconnect()

        //Checking if connection was closed.
        assertTrue(connection.isClosed)

        //Getting new instance and seeing if it is open.
        assertFalse(SQLiteConnection.getInstance().connection.isClosed)

        //Closing new instance.
        SQLiteConnection.getInstance().disconnect()
    }


}
