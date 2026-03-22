package io.github.hayatoyagi.prvisualizer.state

import io.github.hayatoyagi.prvisualizer.ChangeType
import io.github.hayatoyagi.prvisualizer.FileNode
import io.github.hayatoyagi.prvisualizer.PrFileChange
import io.github.hayatoyagi.prvisualizer.PullRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OverlayStateTest {
    @Test
    fun `computeFileOverlayByPath ignores paths not present in visible files`() {
        val visibleFiles = listOf(
            FileNode.File(
                path = "src/visible.kt",
                name = "visible.kt",
                extension = "kt",
                totalLines = 10,
                hasActivePr = true,
                weight = 10.0,
            ),
        )
        val prs = listOf(
            PullRequest(
                id = "pr-1",
                number = 1,
                title = "first",
                author = "alice",
                isDraft = false,
                url = "https://example.com/1",
                files = listOf(PrFileChange(path = "src/ghost.kt", additions = 1, deletions = 0)),
            ),
            PullRequest(
                id = "pr-2",
                number = 2,
                title = "second",
                author = "bob",
                isDraft = false,
                url = "https://example.com/2",
                files = listOf(PrFileChange(path = "src/ghost.kt", additions = 2, deletions = 0)),
            ),
        )

        val fileOverlayByPath = computeFileOverlayByPath(
            visiblePrs = prs,
            visibleFiles = visibleFiles,
        )
        val conflictedDirectoryPaths = computeConflictedDirs(fileOverlayByPath)

        assertTrue(fileOverlayByPath.isEmpty())
        assertTrue(conflictedDirectoryPaths.isEmpty())
    }

    @Test
    fun `computeFileOverlayByPath includes newly added file present in visible files`() {
        val addedFilePath = "src/NewFeature.kt"
        val visibleFiles = listOf(
            FileNode.File(
                path = addedFilePath,
                name = "NewFeature.kt",
                extension = "kt",
                totalLines = 30,
                hasActivePr = true,
                weight = 30.0,
            ),
        )
        val prs = listOf(
            PullRequest(
                id = "pr-1",
                number = 1,
                title = "Add NewFeature",
                author = "alice",
                isDraft = false,
                url = "https://example.com/1",
                files = listOf(PrFileChange(path = addedFilePath, additions = 30, deletions = 0)),
            ),
        )

        val fileOverlayByPath = computeFileOverlayByPath(
            visiblePrs = prs,
            visibleFiles = visibleFiles,
        )

        val overlay = fileOverlayByPath[addedFilePath]
        assertNotNull(overlay)
        assertEquals(ChangeType.Addition, overlay.dominantType)
        assertEquals(1, overlay.prs.size)
        assertEquals("pr-1", overlay.prs.first().id)
    }
}
