package org.intellij.sdk.codesync.database

import org.intellij.sdk.codesync.Constants
import org.intellij.sdk.codesync.models.UserAccount
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.util.HashMap
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UserTableTest {

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
    fun validateInsertNewUser(){
        var userAccount = UserAccount("sample@gmail.com", "ACCESS_TOKEN")
        UserTable.insertNewUser(userAccount)
        var resultSet = Database.runQuery("SELECT * FROM user")
        var row : HashMap<String, String> = resultSet.get(0)
        assertEquals("sample@gmail.com", row["EMAIL"])
        assertEquals("ACCESS_TOKEN", row["ACCESS_TOKEN"])
        assertNull(row["ACCESS_KEY"])
        assertNull(row["SECRET_KEY"])
    }

    @Test
    fun validateUpdateUser(){
        var userAccount = UserAccount("sample@gmail.com", "access token")
        UserTable.insertNewUser(userAccount)

        userAccount.setSecretKey("secret key")
        userAccount.setAccessKey("access key")
        println("Change update: " + UserTable.updateUser(userAccount))

        var resultSet = Database.runQuery("SELECT * FROM user")
        var row : HashMap<String, String> = resultSet.get(0)
        assertEquals("sample@gmail.com", row["EMAIL"])
        assertEquals("access token", row["ACCESS_TOKEN"])
        assertEquals("access key", row["ACCESS_KEY"])
        assertEquals("secret key", row["SECRET_KEY"])
    }

    @Test
    fun validateGetByEmail(){
        var userAccount = UserTable.getByEmail("sample@gmail.com")
        assertNull(userAccount)
    }

    @Test
    fun validateGetActiveUser(){

    }

    @Test
    fun validateSetValues(){

    }

}