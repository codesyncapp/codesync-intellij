package org.intellij.sdk.codesync.database

import CodeSyncTestUtils
import org.intellij.sdk.codesync.exceptions.SQLiteDBConnectionError
import org.intellij.sdk.codesync.utils.Queries
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File
import java.nio.file.Paths
import kotlin.test.*


class DatabaseTest {

    @BeforeEach
    fun before() {
        //1. Create user table
        Database.executeUpdate(Queries.User.CREATE_TABLE)

        //2. Add dummy user
        Database.executeUpdate(Queries.User.insert("dummy@gmail.com","ASDFC", null, null, false));
    }

    @AfterEach
    fun after() {
        //1. Disconnect database
        SQLiteConnection.getInstance().disconnect()

        //2. Remove test db file
        var file = File(CodeSyncTestUtils.getTestDBFilePath())
        file.delete()
    }

    @Test
    fun validateSetupDbFilesAndTables(){
        SQLiteConnection.getInstance().connection.close()
        File(CodeSyncTestUtils.getTestDBFilePath()).delete()

        val userFilePath = Paths.get(CodeSyncTestUtils.getTestDataPath(), "userTest.yml").toAbsolutePath()
        val userFile = File(userFilePath.toString())
        userFile.createNewFile()

        var file = File(CodeSyncTestUtils.getTestDBFilePath())
        assertTrue(!file.exists())

        Database.getInstance().setupDbFilesAndTables(CodeSyncTestUtils.getTestDBFilePath())

        file = File(CodeSyncTestUtils.getTestDBFilePath())
        assertTrue(file.exists())

        var resultSet = Database.runQuery(Queries.User.TABLE_EXIST)
        assertNotNull(resultSet)
        assertEquals("user", resultSet[0].get("name"))

        userFile.delete()
    }

    @Test
    fun validateRunQuery(){
        var email = "sample@gmail.com"
        var access_token = "ACCESS TOKEN"
        Database.executeUpdate(Queries.User.insert(email, access_token, null, null, true));
        var resultSet = Database.runQuery(Queries.User.get_by_email(email))

        var row : HashMap<String, String> = resultSet.get(0)
        assertEquals(1, resultSet.size, "1 row exist.")
        assertEquals(email, row["EMAIL"])
        assertEquals(access_token, row["ACCESS_TOKEN"])
        assertNull(row["SECRET_KEY"])
        assertNull(row["ACCESS_KEY"])
        assertEquals(1, Integer.parseInt(row["IS_ACTIVE"]))
    }

    @Test
    fun validateReconnectingDB(){
        var email = "sample@gmail.com"
        var access_token = "ACCESS TOKEN"

        SQLiteConnection.getInstance().disconnect()

        Database.executeUpdate(Queries.User.insert(email, access_token, null, null, true));

        SQLiteConnection.getInstance().disconnect()

        var resultSet = Database.runQuery(Queries.User.get_by_email(email))

        var row : HashMap<String, String> = resultSet.get(0)
        assertEquals(1, resultSet.size, "1 row exist.")
        assertEquals(email, row["EMAIL"])
        assertEquals(access_token, row["ACCESS_TOKEN"])
        assertNull(row["SECRET_KEY"])
        assertNull(row["ACCESS_KEY"])
        assertEquals(1, Integer.parseInt(row["IS_ACTIVE"]))
    }

    @Test
    fun userTableCreation(){
        var resultSet = Database.runQuery(Queries.User.TABLE_EXIST)
        assertNotNull(resultSet)
        assertEquals("user", resultSet[0].get("name"))
    }

}
