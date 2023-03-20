package org.intellij.sdk.codesync.codeSyncSetup

import org.intellij.sdk.codesync.Constants
import org.intellij.sdk.codesync.database.Database
import org.jsoup.helper.Validate
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.util.HashMap
import kotlin.test.assertEquals

class CodeSyncSetupTest {

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
        Database.executeUpdate(Constants.CREATE_USER_TABLE_QUERY)
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
    fun validateSaveIamUserTest(){
        CodeSyncSetup.saveIamUser("sample@gmail.com", "ACCESS", "SECRET")
        var resultSet = Database.runQuery("SELECT * FROM user")
        var row : HashMap<String, String> = resultSet.get(0)
        assertEquals("sample@gmail.com", row["EMAIL"])
        assertEquals("ACCESS", row["ACCESS_KEY"])
        assertEquals("SECRET", row["SECRET_KEY"])
    }

    @Test
    fun validateDisconnectRepo(){

    }

    @Test
    fun validateCheckUserAccess(){

    }

    @Test
    fun validateUploadRepo(){

    }

}