package org.intellij.sdk.codesync.database

import CodeSyncTestUtils
import com.intellij.openapi.application.ApplicationInfo
import org.intellij.sdk.codesync.Constants.CONNECTION_STRING
import org.intellij.sdk.codesync.configuration.ConfigurationFactory
import org.intellij.sdk.codesync.configuration.TestConfiguration
import org.intellij.sdk.codesync.utils.ProjectUtils
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import java.io.File
import java.nio.file.Paths
import java.sql.DriverManager
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SQLiteConnectionTest {
    @Test
    fun getTestConnectionString(){

        val applicationInfo : ApplicationInfo = mock(ApplicationInfo::class.java)

        mockStatic(ConfigurationFactory::class.java).use { configurationFactoryMock ->
            configurationFactoryMock.`when`<Any>{ ConfigurationFactory.getConfiguration() }
                .thenReturn(TestConfiguration.getInstance())

                mockStatic(ApplicationInfo::class.java).use {applicationInfoMocked ->
                    applicationInfoMocked.`when`<Any>{ ApplicationInfo.getInstance() }
                        .thenReturn(applicationInfo)

                        `when`(applicationInfo.versionName).thenReturn("Intellij")
                        mockStatic(ProjectUtils::class.java).use { projectUtilsMocked ->
                            projectUtilsMocked.`when` <Any> { ProjectUtils.getPluginVersion() }.thenReturn("1.0.0")
                            println("Here: " + CONNECTION_STRING)
                        }
                }
        }
    }

    @AfterEach
    fun AfterEach(){
        //1. Disconnect database
        SQLiteConnection.getInstance().disconnect()

        //2. Remove test db file
        var file = File(CodeSyncTestUtils.getTestDBFilePath())
        file.delete()
    }

    @Test
    fun validateSQLiteConnection(){

        //Getting connection instance first time.
        var connection = SQLiteConnection.getInstance().connection

        //Checking if connection is open.
        assertTrue(!connection.isClosed)

        //Closing existing connection.
        SQLiteConnection.getInstance().disconnect()

        //Checking if connection was closed.
        assertTrue(connection.isClosed)

        //Getting new instance and seeing if it is open.
        assertFalse(SQLiteConnection.getInstance().connection.isClosed)

        //Closing new instance.
        SQLiteConnection.getInstance().disconnect()
    }


}
