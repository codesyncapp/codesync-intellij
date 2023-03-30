package org.intellij.sdk.codesync.database

import org.intellij.sdk.codesync.Helper
import org.intellij.sdk.codesync.utils.Queries
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull


class DatabaseTest {

    @BeforeEach
    fun before() {
        //2. Create directory
        var file = File(Helper.DIRECTORY_PATH)
        file.mkdir()

        //3. Connect to db created in step (2) above repo
        Database.initiate(Helper.CONNECTION_STRING)

        //4. Create user table
        Database.executeUpdate(Queries.User.CREATE_TABLE)

        //5. Add dummy user
        Database.executeUpdate(Queries.User.insert("dummy@gmail.com","ASDFC", null, null, false));
    }

    @AfterEach
    fun after() {
        //1. Disconnect database
        Database.disconnect()

        //2. Remove base repo
        var file = File(Helper.DIRECTORY_PATH + Helper.DATABASE_FILE)
        file.delete()
        file = File(Helper.DIRECTORY_PATH)
        file.delete()
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
    fun userTableCreation(){
        var resultSet = Database.runQuery(Queries.User.TABLE_EXIST)
        assertNotNull(resultSet)
        assertEquals("user", resultSet[0].get("name"))
    }

}