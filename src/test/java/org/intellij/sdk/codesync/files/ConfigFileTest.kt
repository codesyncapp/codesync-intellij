package org.intellij.sdk.codesync.files

import CodeSyncTestUtils.getTestDataPath
import org.intellij.sdk.codesync.utils.FileUtils
import org.junit.After
import org.junit.Before
import org.junit.jupiter.api.Test
import java.io.File
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Paths


class ConfigFileTest {

    /* This folder and the files created in it will be deleted after
         * tests are run, even in the event of failures or exceptions.
         */
    private val invalidSourceConfigFilePath = "${getTestDataPath()}/files/invalid-config.yml"
    private val validSourceConfigFilePath = "${getTestDataPath()}/files/valid-config.yml"
    private val invalidContent: String = FileUtils.readFileToString(invalidSourceConfigFilePath)
    private val validContent: String = FileUtils.readFileToString(validSourceConfigFilePath)
    @Before
    fun setUp() {
        // Create a temporary directory
        val tempDir = Files.createTempDirectory("codesync")

        // Set the temporary directory for your test
        System.setProperty("java.io.tmpdir", tempDir.toString())
    }

    @After
    fun tearDown() {
        // Clean up the temporary directory
        val tempDir = Paths.get(System.getProperty("java.io.tmpdir"))
        Files.deleteIfExists(tempDir)
    }

    private fun writeConfigFile(content: String){
        val tempDir = Paths.get(System.getProperty("java.io.tmpdir"))
        val configFile = File("${tempDir}/config.yml")

        if (!configFile.exists()) {
            configFile.createNewFile()
        }
        val writer = FileWriter(configFile)
        writer.write(content)
        writer.flush()
        writer.close()
    }

    @Test
    fun testUpdateConfigFile() {
        val tempDir = Paths.get(System.getProperty("java.io.tmpdir"))
        val configFilePath = "${tempDir}/config.yml"

        // Create an invalid config file.
        writeConfigFile(validContent)

        var configFile = ConfigFile(configFilePath);

        // Validate the config file is valid.
        assert(configFile.hasRepo("/Users/codesync/dev/test-repo"))

        val configRepo = configFile.getRepo("/Users/codesync/dev/test-repo")

        // Update the repo.
        configRepo.isDisconnected = true

        configFile.publishRepoUpdate(configRepo)

        // Read the config file again
        configFile = ConfigFile(configFilePath);
        // Validate the config file is valid.
        assert(configFile.hasRepo("/Users/codesync/dev/test-repo"))
        assert(configFile.getRepo("/Users/codesync/dev/test-repo").isDisconnected)
    }

    @Test
    fun testDeleteRepoInConfigFile() {
        val tempDir = Paths.get(System.getProperty("java.io.tmpdir"))
        val configFilePath = "${tempDir}/config.yml"

        // Create an invalid config file.
        writeConfigFile(validContent)

        var configFile = ConfigFile(configFilePath);

        // Validate the config file is valid.
        assert(configFile.hasRepo("/Users/codesync/dev/test-repo"))

        configFile.publishRepoRemoval("/Users/codesync/dev/test-repo")

        // Read the config file again
        configFile = ConfigFile(configFilePath);
        // Validate the config file is valid.
        assert(!configFile.hasRepo("/Users/codesync/dev/test-repo"))
    }

    @Test
    fun testUpdateBranchInConfigFile() {
        val tempDir = Paths.get(System.getProperty("java.io.tmpdir"))
        val configFilePath = "${tempDir}/config.yml"

        val repoName = "/Users/codesync/dev/test-repo"

        // Create an invalid config file.
        writeConfigFile(validContent)

        var configFile = ConfigFile(configFilePath);

        // Validate the config file is valid.
        assert(configFile.hasRepo(repoName))

        var configRepo = configFile.getRepo(repoName)

        // Validate branch exists
        assert(configRepo.containsBranch("test-branch"))
        var configRepoBranch = configRepo.getRepoBranch("test-branch")

        val files = configRepoBranch.getFiles()
        files["another-test-file.txt"] = 2
        configRepoBranch.updateFiles(files)

        configFile.publishBranchUpdate(configRepo, configRepoBranch)

        // Read the config file again
        configFile = ConfigFile(configFilePath);
        // Validate the config file is valid.
        assert(configFile.hasRepo(repoName))
        configRepo = configFile.getRepo(repoName)
        configRepoBranch = configRepo.getRepoBranch("test-branch")

        // Assert branch was updated with success.
        assert(configRepoBranch.getFileId("another-test-file.txt") == 2)
    }

    @Test
    fun testDeleteBranchInConfigFile() {
        val tempDir = Paths.get(System.getProperty("java.io.tmpdir"))
        val configFilePath = "${tempDir}/config.yml"

        val repoName = "/Users/codesync/dev/test-repo"

        // Create an invalid config file.
        writeConfigFile(validContent)

        var configFile = ConfigFile(configFilePath);

        // Validate the config file is valid.
        assert(configFile.hasRepo(repoName))

        var configRepo = configFile.getRepo(repoName)

        // Validate branch exists
        assert(configRepo.containsBranch("test-branch"))
        val configRepoBranch = configRepo.getRepoBranch("test-branch")

        val files = configRepoBranch.getFiles()
        files["another-test-file.txt"] = 2
        configRepoBranch.updateFiles(files)

        configFile.publishBranchRemoval(configRepo, "test-branch")

        // Read the config file again
        configFile = ConfigFile(configFilePath);
        // Validate the config file is valid.
        assert(configFile.hasRepo(repoName))
        configRepo = configFile.getRepo(repoName)
        assert(!configRepo.containsBranch("test-branch"))
    }
}
