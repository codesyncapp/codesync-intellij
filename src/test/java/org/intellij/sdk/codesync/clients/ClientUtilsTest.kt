package org.intellij.sdk.codesync.clients

import org.intellij.sdk.codesync.Constants
import org.intellij.sdk.codesync.clients.ClientUtils
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ClientUtilsTest {

    @Test
    fun sendPost(){
        //TODO Add mocking
        val correctAPI = String.format("%s/users?&source=%s&v=%s", "https://api.codesync.com/v1", "intellij", "unknown")
        assertEquals(401, ClientUtils.sendPost(correctAPI, "ACCESS_TOKEN").statusCode)
    }

}
