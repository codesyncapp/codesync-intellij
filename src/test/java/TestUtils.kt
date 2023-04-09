import com.intellij.openapi.application.PathManager
import com.intellij.openapi.util.io.FileUtil
import java.nio.file.Paths

object CodeSyncTestUtils {
    private val DIRECTORY_PATH = Paths.get(System.getProperty("user.dir"), "test_data").toAbsolutePath()

    fun getTestDataPath(): String {
        return DIRECTORY_PATH.toString();
    }

    fun getTempPath(): String {
        return FileUtil.toCanonicalPath("${PathManager.getTempPath()}/")
    }
}
