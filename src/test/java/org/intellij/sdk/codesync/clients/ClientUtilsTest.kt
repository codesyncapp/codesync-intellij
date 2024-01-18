package org.intellij.sdk.codesync.clients

import org.apache.http.HttpEntity
import org.apache.http.ProtocolVersion
import org.apache.http.StatusLine
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.message.BasicHttpResponse
import org.apache.http.message.BasicStatusLine
import org.intellij.sdk.codesync.exceptions.response.StatusCodeError
import org.json.simple.JSONObject
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Answers
import org.mockito.Mockito.*
import kotlin.test.assertEquals

class ClientUtilsTest {

    @Test
    fun testSendPost(){
        
        val httpClientBuilder : HttpClientBuilder = mock(HttpClientBuilder::class.java)
        val httpClient : CloseableHttpClient = mock(CloseableHttpClient::class.java)

        val successfullyHttpResponse: CloseableHttpResponse = getHttpResponse(200)
        val failedHttpResponse: CloseableHttpResponse = getHttpResponse(401)

        val successfullyExpectedJSONResponse = "{\"user\":{\"access_token\":\"Unit Test Mode\",\"name\":\"Unit Test Name\",\"id\":\"Unit Test ID\",\"email\":\"Unit Test Email\"}}"

        mockStatic(ClientUtils::class.java, Answers.CALLS_REAL_METHODS).use { mocked -> mocked.

            //Mocking for multiple calls of same methods.
            `when`<Any>{ClientUtils.getHttpClientBuilder()}.thenReturn(httpClientBuilder).thenReturn(httpClientBuilder)
            `when`(httpClientBuilder.build()).thenReturn(httpClient).thenReturn(httpClient)
            `when`(httpClient.execute(any())).thenReturn(successfullyHttpResponse).thenReturn(failedHttpResponse)

            val API = String.format("%s/users?&source=%s&v=%s", "https://api.example.com/v1", "intellij", "unknown")

            //First call resulting in successfully request.
            var actualResponse = ClientUtils.sendPost(API, "ACCESS_TOKEN")
            assertEquals(successfullyExpectedJSONResponse, actualResponse.jsonResponse.toString())
            assertEquals(200, actualResponse.statusCode)

            //Second call resulting in failed request.
            assertThrows<StatusCodeError> {
                actualResponse = ClientUtils.sendPost(API, "ACCESS_TOKEN")

            }

        }

        // Don't forget to close the response when done
        successfullyHttpResponse.close()
        failedHttpResponse.close()
    }

    private fun getHttpResponse(statusCode : Int): CloseableHttpResponse {

        val userJSON = JSONObject()
        val responseJSON = JSONObject()

        if(statusCode == 200){
            userJSON.put("access_token", "Unit Test Mode")
            userJSON.put("name", "Unit Test Name")
            userJSON.put("id", "Unit Test ID")
            userJSON.put("email", "Unit Test Email")
            responseJSON.put("user", userJSON)
        }else if(statusCode == 401){
            userJSON.put("message", "Token verification failed")
            responseJSON.put("error", userJSON)
        }


        val protocolVersion = ProtocolVersion("HTTP", 1, 1)
        val statusLine: StatusLine = BasicStatusLine(protocolVersion, statusCode, "OK")

        val httpEntity: HttpEntity = StringEntity(responseJSON.toString())

        val httpResponse: CloseableHttpResponse = object : BasicHttpResponse(statusLine), CloseableHttpResponse {
            override fun close() {}
        }.apply {
            entity = httpEntity
        }

        return httpResponse
    }

}

