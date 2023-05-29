package org.intellij.sdk.codesync.database

import org.intellij.sdk.codesync.models.UserAccount
import org.intellij.sdk.codesync.utils.Queries
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class UserTableTest {

    @BeforeEach
    fun before() {
        //2. Create directory
        var file = File(CodeSyncTestUtils.getTestDataPath())
        file.mkdir()

        //3. Connect to db created in step (2) above repo
        Database.initiate(CodeSyncTestUtils.getTestConnectionString())

        //4. Create user table
        Database.executeUpdate(Queries.User.CREATE_TABLE)

        //5. Add dummy user
        Database.executeUpdate(Queries.User.insert("dummy@gmail.com","ASDFC", null, null, false));
    }

    @AfterEach
    fun after() {
        //1. Disconnect database
        Database.disconnect()

        //2. Remove test db file
        var file = File(CodeSyncTestUtils.getTestDBFilePath())
        file.delete()
    }

    @Test
    fun validateInsertNewUser(){
        var email = "sample@gmail.com"

        var userAccount = UserAccount(email, "ACCESS_TOKEN")
        UserTable.insertNewUser(userAccount)

        var resultSet = Database.runQuery(Queries.User.get_by_email(email))
        var row : HashMap<String, String> = resultSet.get(0)
        assertEquals(email, row["EMAIL"])
        assertEquals("ACCESS_TOKEN", row["ACCESS_TOKEN"])
        assertNull(row["ACCESS_KEY"])
        assertNull(row["SECRET_KEY"])
    }

    @Test
    fun insertDuplicate(){
        var email = "Dummy@gmail.com"

        var userAccount = UserAccount(email, "ACCESS_TOKEN")
        AssertionError(UserTable.insertNewUser(userAccount))
    }

    @Test
    fun validateUpdateUser(){
        var email = "sample@gmail.com"

        var userAccount = UserAccount(email, "access token")
        UserTable.insertNewUser(userAccount)

        userAccount.setSecretKey("secret key")
        userAccount.setAccessKey("access key")
        println("Change update: " + UserTable.updateUser(userAccount))

        var resultSet = Database.runQuery(Queries.User.get_by_email(email))
        var row : HashMap<String, String> = resultSet.get(0)
        assertEquals(email, row["EMAIL"])
        assertEquals("access token", row["ACCESS_TOKEN"])
        assertEquals("access key", row["ACCESS_KEY"])
        assertEquals("secret key", row["SECRET_KEY"])
    }

    @Test
    fun validateGetByEmail(){

        var email = "sample@gmail.com"

        //Inserting a user
        var newUser = UserAccount(email, "ACCESS_TOKEN")
        UserTable.insertNewUser(newUser)

        //Getting inserted user.
        var userAccount = UserTable.getByEmail(email)
        assertNotNull(userAccount)
        assertEquals(email, userAccount.getUserEmail())

        //Getting user who does not exist!
        var notExistingUser = UserTable.getByEmail("nonExistant@gmail.com")
        assertNull(notExistingUser)
    }

    @Test
    fun validateGetActiveUser(){

    }

}
