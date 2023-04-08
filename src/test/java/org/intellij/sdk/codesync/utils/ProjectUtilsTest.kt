package org.intellij.sdk.codesync.utils

import CodeSyncTestUtils
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.util.*


class ProjectUtilsTest {

    /*
    Validate the behavior of `getAllContentRoots` method from ProjectUtils.
    */
    @Test
    fun validateGetAllContentRootsWithSingleRoot() {
        val project: Project = Mockito.mock(Project::class.java)
        val projectRootManager: ProjectRootManager = Mockito.mock(ProjectRootManager::class.java)
        val testDataPath = CodeSyncTestUtils.getTestDataPath()
        val mockFiles = arrayOf(
            // Three repos, with one parent and 2 nested.
            Mockito.mock(VirtualFile::class.java).apply { Mockito.`when`(getCanonicalPath()).thenReturn("$testDataPath/test-repo") },
            Mockito.mock(VirtualFile::class.java).apply { Mockito.`when`(getCanonicalPath()).thenReturn("$testDataPath/test-repo/nested-repo") },
            Mockito.mock(VirtualFile::class.java).apply { Mockito.`when`(getCanonicalPath()).thenReturn("$testDataPath/test-repo/another-nested-repo") }
        )

        // configure the mock objects
        Mockito.`when`(projectRootManager.contentRoots).thenReturn(mockFiles)
        Mockito.`when`(ProjectRootManager.getInstance(project)).thenReturn(projectRootManager)

        val contentRoots = ProjectUtils.getAllContentRoots(project);
        assert(contentRoots.size == 1)
        assert(contentRoots[0].canonicalPath == "$testDataPath/test-repo")
    }

    @Test
    fun validateGetAllContentRootsWithMultipleRoots() {
        val project: Project = Mockito.mock(Project::class.java)
        val projectRootManager: ProjectRootManager = Mockito.mock(ProjectRootManager::class.java)
        val testDataPath = CodeSyncTestUtils.getTestDataPath()
        val mockFiles = arrayOf(
            // Three repos, with one parent and 2 nested.
            Mockito.mock(VirtualFile::class.java).apply { Mockito.`when`(getCanonicalPath()).thenReturn("$testDataPath/test-repo") },
            Mockito.mock(VirtualFile::class.java).apply { Mockito.`when`(getCanonicalPath()).thenReturn("$testDataPath/test-repo/nested-repo") },
            Mockito.mock(VirtualFile::class.java).apply { Mockito.`when`(getCanonicalPath()).thenReturn("$testDataPath/test-repo/another-nested-repo") },

            // 2 repos, with one parent and 1 nested.
            Mockito.mock(VirtualFile::class.java).apply { Mockito.`when`(getCanonicalPath()).thenReturn("$testDataPath/another-repo") },
            Mockito.mock(VirtualFile::class.java).apply { Mockito.`when`(getCanonicalPath()).thenReturn("$testDataPath/another-repo/nested-repo") },

            // Single repo.
            Mockito.mock(VirtualFile::class.java).apply { Mockito.`when`(getCanonicalPath()).thenReturn("$testDataPath/third-repo/different-path") },
        )

        // configure the mock objects
        Mockito.`when`(projectRootManager.contentRoots).thenReturn(mockFiles)
        Mockito.`when`(ProjectRootManager.getInstance(project)).thenReturn(projectRootManager)

        val contentRoots = ProjectUtils.getAllContentRoots(project);

        // Validate there are 3 repos.
        assert(contentRoots.size == 3)

        val expectedRepos = arrayOf(
            "$testDataPath/test-repo",
            "$testDataPath/another-repo",
            "$testDataPath/third-repo/different-path",
        )
        val result = Arrays.stream(contentRoots).distinct().map { contentRoot: VirtualFile? -> contentRoot?.canonicalPath }.toArray();
        assert(result.contentEquals(expectedRepos))
    }

    @Test
    fun validateGetAllContentRootsWithMultipleRootsNoChild() {
        val project: Project = Mockito.mock(Project::class.java)
        val projectRootManager: ProjectRootManager = Mockito.mock(ProjectRootManager::class.java)
        val testDataPath = CodeSyncTestUtils.getTestDataPath()
        val mockFiles = arrayOf(
            // 5 repos no child
            Mockito.mock(VirtualFile::class.java).apply { Mockito.`when`(getCanonicalPath()).thenReturn("$testDataPath/test-repo") },
            Mockito.mock(VirtualFile::class.java).apply { Mockito.`when`(getCanonicalPath()).thenReturn("$testDataPath/another-repo") },
            Mockito.mock(VirtualFile::class.java).apply { Mockito.`when`(getCanonicalPath()).thenReturn("$testDataPath/third-repo/different-path") },
            Mockito.mock(VirtualFile::class.java).apply { Mockito.`when`(getCanonicalPath()).thenReturn("$testDataPath/my-path/fourth-repo") },
            Mockito.mock(VirtualFile::class.java).apply { Mockito.`when`(getCanonicalPath()).thenReturn("$testDataPath/my-path/fifth-repo") },
        )

        // configure the mock objects
        Mockito.`when`(projectRootManager.contentRoots).thenReturn(mockFiles)
        Mockito.`when`(ProjectRootManager.getInstance(project)).thenReturn(projectRootManager)

        val contentRoots = ProjectUtils.getAllContentRoots(project);

        // Validate there are 5 repos.
        assert(contentRoots.size == 5)

        val expectedRepos = arrayOf(
            "$testDataPath/test-repo",
            "$testDataPath/another-repo",
            "$testDataPath/third-repo/different-path",
            "$testDataPath/my-path/fourth-repo",
            "$testDataPath/my-path/fifth-repo",
        )
        val result = Arrays.stream(contentRoots).distinct().map { contentRoot: VirtualFile? -> contentRoot?.canonicalPath }.toArray();
        assert(result.contentEquals(expectedRepos))
    }

    @Test
    fun validateIsChild() {
        val testDataPath = CodeSyncTestUtils.getTestDataPath()

        // Assert returns true for child path.
        assert(
            ProjectUtils.isChild(
                "$testDataPath/test-repo/nested-repo",
                "$testDataPath/test-repo"
            )
        )
        // Assert returns true for child path.
        assert(
            ProjectUtils.isChild(
                "$testDataPath/test-repo/another-nested-repo",
                "$testDataPath/test-repo"
            )
        )

        // Assert returns false if first path is not a child of the second.
        assert(
            !ProjectUtils.isChild(
                "$testDataPath/first-repo",
                "$testDataPath/second-repo"
            )
        )

        // Assert returns false if first path is not a child of the second.
        assert(
            !ProjectUtils.isChild(
                "$testDataPath/first-path",
                "$testDataPath/first-path/nested-repo"
            )
        )
         // Assert returns false if first path is the same as the second.
        assert(
            !ProjectUtils.isChild(
                "$testDataPath/first-path",
                "$testDataPath/first-path"
            )
        )
        // Assert returns trailing slash does not cause a problem.
        assert(
            !ProjectUtils.isChild(
                "$testDataPath/first-path/",
                "$testDataPath/first-path"
            )
        )

        assert(
            ProjectUtils.isChild(
                "$testDataPath/test-repo/nested-repo/",
                "$testDataPath/test-repo/"
            )
        )
    }

    @Test
    fun validateIsParent() {
        val testDataPath = CodeSyncTestUtils.getTestDataPath()

        // Assert returns true for parent path.
        assert(
            ProjectUtils.isParent(
                "$testDataPath/test-repo",
                "$testDataPath/test-repo/nested-repo",
            )
        )
        // Assert returns true for parent path.
        assert(
            ProjectUtils.isParent(
                "$testDataPath/test-repo",
                "$testDataPath/test-repo/another-nested-repo",
            )
        )

        // Assert returns true if first path is the same as the second.
        assert(
            ProjectUtils.isParent(
                "$testDataPath/first-path",
                "$testDataPath/first-path"
            )
        )

        // Assert returns trailing slash does not cause a problem.
        assert(
            ProjectUtils.isParent(
                "$testDataPath/first-path",
                "$testDataPath/first-path/"
            )
        )

        assert(
            !ProjectUtils.isParent(
                "$testDataPath/test-repo/nested-repo/",
                "$testDataPath/test-repo/"
            )
        )

        // Assert returns false if first path is not a parent of the second.
        assert(
            !ProjectUtils.isParent(
                "$testDataPath/first-repo",
                "$testDataPath/second-repo"
            )
        )

        // Assert returns false if first path is not a parent of the second.
        assert(
            !ProjectUtils.isParent(
                "$testDataPath/first-path/nested-repo",
                "$testDataPath/first-path",
            )
        )
    }

}
