package org.intellij.sdk.codesync.utils.clients

import org.intellij.sdk.codesync.Constants
import org.intellij.sdk.codesync.clients.ClientUtils
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ClientUtilsTest {

    @BeforeEach
    fun before(){

    }

    @AfterEach
    fun after(){

    }

    @Test
    fun sendPost(){
        val API_USERS = String.format("%s/users?&source=%s&v=%s", "https://api.codesync.com/v1", "intellij", "unknown")
        ClientUtils.sendPost(API_USERS, "ACCESS_TOKEN")
    }

}