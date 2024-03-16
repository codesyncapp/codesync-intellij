import com.intellij.openapi.application.PathManager
import com.intellij.openapi.util.io.FileUtil
import java.nio.file.Paths
import org.intellij.sdk.codesync.Constants.*
import org.intellij.sdk.codesync.codeSyncSetup.CodeSyncSetup.createSystemDirectories

object CodeSyncTestUtils {
    //TODO Test files/paths should also be accessed from configuration files.
    private val DIRECTORY_PATH = Paths.get(System.getProperty("user.dir"), "test_data").toAbsolutePath()
    private val DATABASE_FILE = Paths.get(DIRECTORY_PATH.toString(), "test.db").toAbsolutePath()

    fun getTestDataPath(): String {
        return DIRECTORY_PATH.toString();
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
    fun setupCodeSyncDirectory() {
        // First delete the directory if it exists.
        FileUtil.delete(Paths.get(CODESYNC_ROOT))
        createSystemDirectories()
    }
}
