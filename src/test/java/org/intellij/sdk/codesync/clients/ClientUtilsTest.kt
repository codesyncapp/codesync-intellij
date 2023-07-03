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
import org.json.simple.JSONObject
import org.junit.jupiter.api.Test
import org.mockito.Answers
import org.mockito.Mockito.*
import kotlin.test.assertEquals

class ClientUtilsTest {

    @Test
    fun sendPost(){
        
        val httpClientBuilder : HttpClientBuilder = mock(HttpClientBuilder::class.java)
        val httpClient : CloseableHttpClient = mock(CloseableHttpClient::class.java)
        val httpResponse: CloseableHttpResponse = getHttpResponse()
        val expectedJSONResponse = "{\"user\":{\"access_token\":\"Unit Test Mode\",\"name\":\"Unit Test Name\",\"id\":\"Unit Test ID\",\"email\":\"Unit Test Email\"}}"

        mockStatic(ClientUtils::class.java, Answers.CALLS_REAL_METHODS).use {

            mocked -> mocked.`when`<Any>{ClientUtils.getHttpClientBuilder()}.thenReturn(httpClientBuilder)
            `when`(httpClientBuilder.build()).thenReturn(httpClient)
            `when`(httpClient.execute(any())).thenReturn(httpResponse)

            val API = String.format("%s/users?&source=%s&v=%s", "https://api.example.com/v1", "intellij", "unknown")
            val actualResponse = ClientUtils.sendPost(API, "ACCESS_TOKEN")

            assertEquals(expectedJSONResponse, actualResponse.jsonResponse.toString())
            assertEquals(200, actualResponse.statusCode)
        }

        // Don't forget to close the response when done
        httpResponse.close()
    }

    private fun getHttpResponse(): CloseableHttpResponse {
        val userJSON = JSONObject()
        userJSON.put("access_token", "Unit Test Mode")
        userJSON.put("name", "Unit Test Name")
        userJSON.put("id", "Unit Test ID")
        userJSON.put("email", "Unit Test Email")
        val responseJSON = JSONObject()
        responseJSON.put("user", userJSON)

        val protocolVersion = ProtocolVersion("HTTP", 1, 1)
        val statusLine: StatusLine = BasicStatusLine(protocolVersion, 200, "OK")

        val httpEntity: HttpEntity = StringEntity(responseJSON.toString())

        val httpResponse: CloseableHttpResponse = object : BasicHttpResponse(statusLine), CloseableHttpResponse {
            override fun close() {}
        }.apply {
            entity = httpEntity
        }

        return httpResponse
    }

}

