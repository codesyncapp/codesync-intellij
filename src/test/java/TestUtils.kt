import com.intellij.openapi.application.PathManager
import com.intellij.openapi.util.io.FileUtil
import java.nio.file.Paths

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
}
