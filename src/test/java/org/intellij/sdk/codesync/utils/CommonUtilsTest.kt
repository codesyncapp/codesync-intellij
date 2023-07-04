package org.intellij.sdk.codesync.utils

import com.intellij.openapi.application.ApplicationInfo
import org.intellij.sdk.codesync.Constants
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Answers
import org.mockito.Mockito.*
import java.util.*


class CommonUtilsTest {

    @Test
    fun testGetOS(){
        //Here we are unit testing a static method that has call of another static method of same class.
        //And that static call need to be mocked.
        //It is something which was not possible in earlier version of mockito. Version 4.6.1 supports it now.
        mockStatic(CommonUtils::class.java, Answers.CALLS_REAL_METHODS).use{ mocked ->
            mocked.`when`<Any>{ CommonUtils.isWindows() }.thenReturn(false)
            mocked.`when`<Any>{ CommonUtils.isMac() }.thenReturn(false)
            mocked.`when`<Any> { CommonUtils.isSolaris() }.thenReturn(false)

            assertEquals("aix", CommonUtils.getOS())
        }

        mockStatic(CommonUtils::class.java, Answers.CALLS_REAL_METHODS).use{ mocked ->
            mocked.`when`<Any>{ CommonUtils.isWindows() }.thenReturn(true)
            mocked.`when`<Any>{ CommonUtils.isMac() }.thenReturn(false)
            mocked.`when`<Any> { CommonUtils.isSolaris() }.thenReturn(false)

            assertEquals("windows", CommonUtils.getOS())
        }

        mockStatic(CommonUtils::class.java, Answers.CALLS_REAL_METHODS).use{ mocked ->
            mocked.`when`<Any>{ CommonUtils.isWindows() }.thenReturn(false)
            mocked.`when`<Any>{ CommonUtils.isMac() }.thenReturn(true)
            mocked.`when`<Any> { CommonUtils.isSolaris() }.thenReturn(false)

            assertEquals("mac", CommonUtils.getOS())
        }

        mockStatic(CommonUtils::class.java, Answers.CALLS_REAL_METHODS).use{ mocked ->
            mocked.`when`<Any>{ CommonUtils.isWindows() }.thenReturn(false)
            mocked.`when`<Any>{ CommonUtils.isMac() }.thenReturn(false)
            mocked.`when`<Any> { CommonUtils.isSolaris() }.thenReturn(true)

            assertEquals("sunos", CommonUtils.getOS())
        }
    }

    @Test
    fun testGetIDEBuildDate(){
        val dummyBuildDate = Calendar.getInstance()
        val applicationInfo : ApplicationInfo = mock(ApplicationInfo::class.java)
        mockStatic(ApplicationInfo::class.java).use{mocked ->

            //Here we are mocking static method of ApplicationInfo class
            mocked.`when`<Any>{ ApplicationInfo.getInstance() }.thenReturn(applicationInfo)

            //Here we are mocking non-static method of same class using the mocked object.
            `when`(applicationInfo.buildDate).thenReturn(dummyBuildDate)

            assertEquals(dummyBuildDate.get(Calendar.YEAR), CommonUtils.getIDEBuildDate().get(Calendar.YEAR))
            assertEquals(dummyBuildDate.get(Calendar.MONTH), CommonUtils.getIDEBuildDate().get(Calendar.MONTH))
            assertEquals(dummyBuildDate.get(Calendar.DATE), CommonUtils.getIDEBuildDate().get(Calendar.DATE))
        }
    }

    @Test
    fun validateParseDate() {
        var date: Date? = CodeSyncDateUtils.parseDate(
            "2023-01-15 07:40:02.682 UTC",
            Constants.DATE_TIME_FORMAT
        )
        assert(date != null)

        date = CodeSyncDateUtils.parseDate(
            "invalid-date",
            Constants.DATE_TIME_FORMAT
        )
        assert(date == null)
    }
}
