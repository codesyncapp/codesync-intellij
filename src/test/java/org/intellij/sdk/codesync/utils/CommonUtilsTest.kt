package org.intellij.sdk.codesync.utils

import org.intellij.sdk.codesync.Constants
import org.intellij.sdk.codesync.utils.CommonUtils.computeDiff
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import java.util.*
import kotlin.test.assertTrue


class CommonUtilsTest {

    @Test
    fun testIsWindows() {
//        System.setProperty("os.name", "Windows 10")
//
//        val commonUtils = mock(CommonUtils::class.java)
//
//        `when`(CommonUtils.isWindows()).then {
//            val osName = System.getProperty("os.name").toLowerCase()
//            osName.contains("win")
//        }
//
//        val result = CommonUtils.isWindows()
//
//        assertTrue(result)
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



    @Test
    fun testComputeDiff() {
//        val original = "Hello, world!"
//        val change = "Hello, Ahmed."
//
//        val expectedDiff = "@@ -4,10 +4,10 @@\n lo, \n-world!\n+Ahmed."
//        val actualDiff = computeDiff(original, change)
//        print(actualDiff)
//        assertEquals(actualDiff.equals(expectedDiff))
    }

}
