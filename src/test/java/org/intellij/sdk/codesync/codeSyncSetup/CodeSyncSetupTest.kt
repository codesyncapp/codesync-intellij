package org.intellij.sdk.codesync.codeSyncSetup

import org.intellij.sdk.codesync.Helper
import org.intellij.sdk.codesync.database.Database
import org.intellij.sdk.codesync.utils.Queries
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals

class CodeSyncSetupTest {

    @BeforeEach
    fun before() {
        //2. Create directory
        var file = File(Helper.DIRECTORY_PATH)
        file.mkdir()

        //3. Connect to db created in step (2) above repo
        Database.initiate(Helper.CONNECTION_STRING)

        //4. Create user table
        Database.executeUpdate(Queries.CREATE_USER_TABLE)

        //5. Add dummy user
        Database.executeUpdate(Queries.User.insert("Dummy@gmail.com","ASDFC", null, null, false));
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
    fun validateSaveIamUserTest(){
        var email = "sample@gmail.com"
        CodeSyncSetup.saveIamUser(email, "ACCESS", "SECRET")
        var resultSet = Database.runQuery(Queries.User.get_by_email(email))
        var row : HashMap<String, String> = resultSet.get(0)
        assertEquals(email, row["EMAIL"])
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