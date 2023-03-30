package org.intellij.sdk.codesync.model

import org.intellij.sdk.codesync.Helper
import org.intellij.sdk.codesync.database.Database
import org.intellij.sdk.codesync.database.UserTable
import org.intellij.sdk.codesync.models.UserAccount
import org.intellij.sdk.codesync.utils.Queries
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class UserAccountTest {


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
    fun validatedGetUser(){
        var userAccount = UserAccount("alpha@gmail.com", "ASDF")
        UserTable.insertNewUser(userAccount);
        assertNotNull(userAccount.getUser("alpha@gmail.com"))
        assertNull(userAccount.getUser("beta@gmail.com"))
    }

    @Test
    fun validateGetAccessToken(){
        var userAccount1 = UserAccount("alpha@gmail.com", "alphaAccess")
        userAccount1.makeActive();
        UserTable.insertNewUser(userAccount1);

        var userAccount2 = UserAccount("beta@gmail.com", "betaAccess")
        UserTable.insertNewUser(userAccount2);

        assertEquals("alphaAccess", UserAccount.getAccessToken("alpha@gmail.com"))
        assertEquals("betaAccess", UserAccount.getAccessToken("beta@gmail.com"))
        assertNull(UserAccount.getAccessToken("gamma@gmail.com"))


    }

    @Test
    fun validateGetEmail(){
        assertNull(UserAccount.getEmail())
        var userAccount1 = UserAccount("alpha@gmail.com", "alphaAccess")
        userAccount1.makeActive();
        UserTable.insertNewUser(userAccount1);
        assertEquals("alpha@gmail.com", UserAccount.getEmail())
    }

    @Test
    fun validateGetAccessTokenByEmail(){
        assertNull(UserAccount.getAccessTokenByEmail())
        var userAccount1 = UserAccount("alpha@gmail.com", "alphaAccess")
        userAccount1.makeActive();
        UserTable.insertNewUser(userAccount1);
        assertEquals("alphaAccess", UserAccount.getAccessTokenByEmail())
    }

    @Test
    fun validateGetActiveAccessToken(){
        var userAccount = UserAccount();
        assertNull(userAccount.getActiveAccessToken())

        var userAccount1 = UserAccount("alpha@gmail.com", "alphaAccess")
        userAccount1.makeActive();
        UserTable.insertNewUser(userAccount1);
        assertEquals("alphaAccess", userAccount.getActiveAccessToken())
    }

    @Test
    fun validateSetActiveUser(){

        var userAccount = UserAccount()
        assertNull(userAccount.getActiveAccessToken())

        var userAccount1 = UserAccount("alpha@gmail.com", "alphaAccess")
        userAccount1.makeInActive()
        UserTable.updateUser(userAccount1)

        userAccount1.setActiveUser("alpha@gmail.com", "alphaAccess")
        assertEquals("alpha@gmail.com", userAccount.getActiveUser().getUserEmail())

        userAccount1.setActiveUser("beta@gmail.com", "betaAccess")
        assertEquals("beta@gmail.com", userAccount.getActiveUser().getUserEmail())

    }

}