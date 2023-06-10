package org.intellij.sdk.codesync.codeSyncSetup

import org.intellij.sdk.codesync.database.Database
import org.intellij.sdk.codesync.database.SQLiteConnection
import org.intellij.sdk.codesync.utils.Queries
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals

class CodeSyncSetupTest {

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
        SQLiteConnection.getInstance().connection.close()

        //2. Remove test db file
        var file = File(CodeSyncTestUtils.getTestDBFilePath())
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
