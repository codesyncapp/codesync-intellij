package org.intellij.sdk.codesync.clients

import com.intellij.openapi.application.ApplicationInfo
import org.intellij.sdk.codesync.configuration.Configuration
import org.intellij.sdk.codesync.database.models.Repo
import org.intellij.sdk.codesync.exceptions.InvalidUsage
import org.intellij.sdk.codesync.exceptions.response.StatusCodeError
import org.intellij.sdk.codesync.files.DiffFile
import org.intellij.sdk.codesync.utils.CodeSyncDateUtils
import org.intellij.sdk.codesync.utils.FileUtils
import org.intellij.sdk.codesync.utils.ProjectUtils
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.*
import java.io.File
import java.util.*
import kotlin.collections.HashMap

class CodeSyncClientTest {

    @Test
    fun uploadFileTest() {

        //This test case is just testing one scenario when file syncignore is sent for upload.
        val configurationMock = mock(Configuration::class.java)
        val hostMock = ""

        val originalsFileMock = mock(File::class.java)
        val absolutePathMock = ""

        val pluginVersionMock = ""

        val applicationInfoMock = mock(ApplicationInfo::class.java)
        val versionNameMock = ""

        val fileInfoMock: MutableMap<String, Any> = HashMap()
        fileInfoMock["isBinary"] = false
        fileInfoMock["size"] = 100L

        val formattedDateMock = ""

        val jsonResponseMock = mock(JSONResponse::class.java)
        val statusCodeErrorMock = StatusCodeError(400, "")

        val accessTokenMock = ""
        val repoMock = mock(Repo::class.java)
        val diffFileMock = mock(DiffFile::class.java)

        `when`(configurationMock.getCodeSyncHost()).thenReturn(hostMock)
        `when`(originalsFileMock.absolutePath).thenReturn(absolutePathMock)

        mockStatic(ProjectUtils::class.java).use { projectUtilsMocked ->
            projectUtilsMocked.`when`<Any> { ProjectUtils.getPluginVersion() }.thenReturn(pluginVersionMock)

            mockStatic(ApplicationInfo::class.java).use { applicationInfo ->
                applicationInfo.`when`<Any> { ApplicationInfo.getInstance() }.thenReturn(applicationInfoMock)
                `when`(applicationInfoMock.versionName).thenReturn(versionNameMock)

                mockStatic(FileUtils::class.java).use { fileUtilsMocked ->
                    fileUtilsMocked.`when`<Any> { FileUtils.getFileInfo(anyString()) }.thenReturn(fileInfoMock)

                    mockStatic(CodeSyncDateUtils::class.java).use { codeSyncDateUtilsMocked ->
                        codeSyncDateUtilsMocked.`when`<Any> { CodeSyncDateUtils.formatDate(Date()) }
                            .thenReturn(formattedDateMock)

                        mockStatic(ClientUtils::class.java).use { clientUtilsMocked ->
                            clientUtilsMocked.`when`<Any> { ClientUtils.sendPost(anyString(), any(), anyString()) }
                                .thenThrow(statusCodeErrorMock)

                            assertThrows<InvalidUsage> {
                                val codeSyncClient = CodeSyncClient()
                                codeSyncClient.uploadFile(
                                    accessTokenMock,
                                    repoMock,
                                    diffFileMock,
                                    originalsFileMock
                                )
                            }

                        }

                    }

                }

            }

        }

    }

}
