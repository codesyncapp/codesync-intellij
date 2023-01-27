package org.intellij.sdk.codesync.utils

import org.intellij.sdk.codesync.Constants
import org.junit.jupiter.api.Test
import java.util.*


class CommonUtilsTest {
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
