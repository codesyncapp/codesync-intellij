import com.intellij.openapi.application.PathManager
import com.intellij.openapi.util.io.FileUtil
import org.apache.commons.io.FileUtils
import org.intellij.sdk.codesync.codeSyncSetup.CodeSyncSetup.createSystemDirectories
import org.intellij.sdk.codesync.configuration.ConfigurationFactory
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.assertEquals
import kotlin.test.assertTrue

object CodeSyncTestUtils {
    //TODO Test files/paths should also be accessed from configuration files.
    private val DIRECTORY_PATH = Paths.get(System.getProperty("user.dir"), "test_data").toAbsolutePath()
    private val DATABASE_FILE = Paths.get(DIRECTORY_PATH.toString(), "test.db").toAbsolutePath()

    fun getTestDataPath(): String {
        return DIRECTORY_PATH.toString();
    }

    fun getTestFilePath(fileName: String): Path {
        return Paths.get(DIRECTORY_PATH.toString(), "files", fileName)
    }

    fun getTestDBFilePath(): String {
        return DATABASE_FILE.toString();
    }

    fun getTestConnectionString(): String {
        return "jdbc:sqlite:" + getTestDBFilePath()
    }

    fun getTempPath(): String {
        return FileUtil.toCanonicalPath("${PathManager.getTempPath()}/")
    }

    /*
    This makes sure the code sync directories for test are created and empty.
     */
    fun cleanupCodeSyncDirectory(codeSyncRoot: String) {
        // Make sure correct configuration is in review. We do not want to delete prod configs
        val configuration = ConfigurationFactory.getConfiguration()
        assertTrue { configuration.isTestMode }

        // First delete the directory if it exists.
        FileUtils.deleteDirectory(Paths.get(codeSyncRoot).toFile())
        createSystemDirectories()
    }
}
