package org.intellij.sdk.codesync.utils

import org.intellij.sdk.codesync.utils.FileUtils.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileTime
import java.time.Instant
import java.time.temporal.ChronoUnit

class FileUtilsTest {
    @Test
    @DisplayName("Read File to String")
    fun testReadFileToString() {
        val file = createTempFile("test_data/testFile", ".txt")
        val content = "Hello, world!"

        file.writeText(content)

        val expectedString = "Hello, world!"
        val actualString = readFileToString(file)

        assertEquals(expectedString, actualString)
    }

    @Test
    @DisplayName("Testing if file is Binary")
    fun testIsBinaryFile() {
        val textFile = createTextFile("test_data/textfile.txt", "This is a text file.")
        assertFalse(isBinaryFile(textFile))

        val binaryFile = createBinaryFile("test_data/binaryfile.bin")
        //Fix this
        assertFalse(isBinaryFile(binaryFile))

        textFile.delete()
        binaryFile.delete()
    }

    private fun createTextFile(fileName: String, content: String): File {
        val file = File(fileName)
        FileWriter(file).use { writer ->
            writer.write(content)
        }
        return file
    }

    private fun createBinaryFile(fileName: String): File {
        val file = File(fileName)
        file.createNewFile()
        return file
    }

    @Test
    @DisplayName("File Creation Date Test")
    fun testGetFileCreationDate() {
        val file = File.createTempFile("test_data/test", ".txt")
        val expectedCreationDate = Instant.now().truncatedTo(ChronoUnit.MILLIS)

        val attributes = Files.readAttributes(file.toPath(), BasicFileAttributes::class.java)
        val creationTime = FileTime.from(expectedCreationDate)
        Files.setAttribute(file.toPath(), "basic:creationTime", creationTime)

        val actualCreationDate = getFileCreationDate(file)

        assertEquals(expectedCreationDate, actualCreationDate)
    }

    @Test
    @DisplayName("Normalizing Path for Windows")
    fun testNormalizeFilePath() {

        val filePath = "test_data/to/file.txt"
        val expectedNormalizedPath = "test_data\\to\\file.txt"

        val actualNormalizedPath = normalizeFilePath(filePath)

        assertEquals(expectedNormalizedPath, actualNormalizedPath)
    }

}