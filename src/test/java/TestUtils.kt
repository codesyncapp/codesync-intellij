import com.intellij.openapi.application.PathManager
import com.intellij.openapi.util.io.FileUtil

object CodeSyncTestUtils {
    fun getTestDataPath(): String {
        return FileUtil.toCanonicalPath("${PathManager.getTempPath()}/")
    }
}