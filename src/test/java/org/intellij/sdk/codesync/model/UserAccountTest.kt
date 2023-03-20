package org.intellij.sdk.codesync.model

import org.intellij.sdk.codesync.models.UserAccount
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertNull

class UserAccountTest {

    @Test
    fun validatedGetUser(){
        var userAccount = UserAccount()
        assertNull(userAccount.getUser("gulahmed2@gaml.com"))
    }

    @Test
    fun validateGetAccessToken(){

    }

    @Test
    fun validateGetEmail(){

    }

    @Test
    fun validateGetAccessTokenByEmail(){

    }

    @Test
    fun validateGetActiveAccessToken(){

    }

    @Test
    fun validateSetActiveUser(){

    }

}