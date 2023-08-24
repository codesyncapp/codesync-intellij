package org.intellij.sdk.codesync

import CodeSyncTestUtils
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UtilsTest {

    @Test
    fun isIndividualFileOpenTest() {

        val directory = Paths.get(CodeSyncTestUtils.getTestDataPath(), "TestDirectory").toAbsolutePath();
        val file = Paths.get(CodeSyncTestUtils.getTestDataPath(), "TestFile.java").toAbsolutePath();
        val directoryNamedLikeFile =
            Paths.get(CodeSyncTestUtils.getTestDataPath(), "DirLikeFile.java").toAbsolutePath();
        val fileNamedLikeDirectory = Paths.get(CodeSyncTestUtils.getTestDataPath(), "FileLikeDir").toAbsolutePath();

        try {
            Files.createDirectory(directory)
            Files.createFile(file)
            Files.createDirectory(directoryNamedLikeFile)
            Files.createFile(fileNamedLikeDirectory)
        } catch (e: Exception) {
            println(e.message)
        }

        assertFalse(Utils.isIndividualFileOpen(directory.toString()))
        assertTrue(Utils.isIndividualFileOpen(file.toString()))
        assertFalse(Utils.isIndividualFileOpen(directoryNamedLikeFile.toString()))
        assertTrue(Utils.isIndividualFileOpen(fileNamedLikeDirectory.toString()))

        try {
            Files.delete(directory)
            Files.delete(file)
            Files.delete(directoryNamedLikeFile)
            Files.delete(fileNamedLikeDirectory)
        } catch (e: Exception) {
            println(e.message)
        }

    }

}

