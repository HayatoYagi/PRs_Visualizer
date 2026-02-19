package io.github.hayatoyagi.prvisualizer

import io.github.hayatoyagi.prvisualizer.github.GitHubApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GitHubApiTest {

    @Test
    fun testPathExtraction() {
        // Test normal paths
        val normalPath = "src/main/kotlin/Main.kt"
        val normalParent = normalPath.substringBeforeLast('/', missingDelimiterValue = "")
        val normalName = normalPath.substringAfterLast('/')
        
        assertEquals("src/main/kotlin", normalParent)
        assertEquals("Main.kt", normalName)
        
        // Test hidden directory paths
        val githubPath = ".github/workflows/build.yml"
        val githubParent = githubPath.substringBeforeLast('/', missingDelimiterValue = "")
        val githubName = githubPath.substringAfterLast('/')
        
        assertEquals(".github/workflows", githubParent)
        assertEquals("build.yml", githubName)
        
        // Test directory path with dot
        val dotDirPath = ".github/workflows"
        val dotDirParent = dotDirPath.substringBeforeLast('/', missingDelimiterValue = "")
        val dotDirName = dotDirPath.substringAfterLast('/')
        
        assertEquals(".github", dotDirParent)
        assertEquals("workflows", dotDirName)
        
        // Test root level hidden directory
        val rootHiddenDir = ".github"
        val rootHiddenParent = rootHiddenDir.substringBeforeLast('/', missingDelimiterValue = "")
        val rootHiddenName = rootHiddenDir.substringAfterLast('/')
        
        assertEquals("", rootHiddenParent)
        assertEquals(".github", rootHiddenName)
    }
    
    @Test
    fun testPathExtractionForHiddenDirectories() {
        // This test verifies path extraction logic for hidden files and directories
        // Note: To test buildTree itself, it would need to be made internal or public
        
        // Simulate file paths from a real repository
        val testPaths = listOf(
            ".github/workflows/build.yml",
            ".gitignore",
            "README.md",
            "src/main.kt"
        )
        
        // Verify path extraction logic for each file
        testPaths.forEach { path ->
            val parentPath = path.substringBeforeLast('/', missingDelimiterValue = "")
            
            // Files with directory separator should have a parent path
            if (path.contains('/')) {
                assertTrue(parentPath.isNotEmpty(),
                    "Parent path should not be empty for $path with /, got: '$parentPath'")
            }
        }
    }
}