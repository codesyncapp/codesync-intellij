import com.intellij.openapi.application.PathManager
import com.intellij.openapi.util.io.FileUtil
import org.intellij.sdk.codesync.database.SQLiteConnection
import org.intellij.sdk.codesync.database.models.Repo
import org.intellij.sdk.codesync.database.models.RepoBranch
import org.intellij.sdk.codesync.database.models.RepoFile
import org.intellij.sdk.codesync.database.models.User
import org.intellij.sdk.codesync.database.tables.MigrationsTable
import java.nio.file.Path
import java.nio.file.Paths

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

    fun deleteTables() {
        val tables = arrayOf(
            User.getTable().tableName,
            Repo.getTable().tableName,
            RepoBranch.getTable().tableName,
            RepoFile.getTable().tableName,
            MigrationsTable.getInstance().tableName,
        )
        SQLiteConnection.getInstance().connection.createStatement().use { statement ->
            for (table in tables) {
                statement.execute("DROP TABLE IF EXISTS $table;")
            }
        }
    }
}
