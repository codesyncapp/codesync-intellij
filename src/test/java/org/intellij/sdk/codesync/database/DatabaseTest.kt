package org.intellij.sdk.codesync.database

import org.intellij.sdk.codesync.Constants.CREATE_USER_TABLE_QUERY
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull


class DatabaseTest {

    var directory_path = ""
    var database_file = ""

    @BeforeEach
    fun before() {
        //1. Generate random base repo path
        directory_path = System.getProperty("user.dir") + "\\src\\test\\java\\test_data"
        database_file = "\\test.db"
        //2. Create directory
        var file = File(directory_path)
        file.mkdir()

        //3. Connect to db created in step (2) above repo
        var connectionString = "jdbc:sqlite:" + directory_path + database_file
        Database.initiate(connectionString)

        //4. Create user table
        Database.executeUpdate(CREATE_USER_TABLE_QUERY)
    }

    @AfterEach
    fun after() {
        //1. Disconnect database
        Database.disconnect()

        //2. Remove base repo
        var file = File(directory_path + database_file)
        file.delete()
        file = File(directory_path)
        file.delete()
    }

    @Test
    fun validateRunQuery(){
        var query = "SELECT * FROM user"
        var resultSet = Database.runQuery(query)

        assertNotNull(resultSet, "Table exists.")
        assertEquals(0, resultSet.size, "Zero rows.")

        var insertQuery = "INSERT INTO user VALUES('Ahmed', 'ASDFC', null, null, 1)"
        Database.executeUpdate(insertQuery)
        resultSet = Database.runQuery(query)

        var row : HashMap<String, String> = resultSet.get(0)
        assertEquals(1, resultSet.size, "1 row exist.")
        assertEquals("Ahmed", row["EMAIL"])
        assertEquals("ASDFC", row["ACCESS_TOKEN"])
        assertNull(row["SECRET_KEY"])
        assertNull(row["ACCESS_KEY"])
        assertEquals(1, Integer.parseInt(row["IS_ACTIVE"]))
    }

}