package io.github.hayatoyagi.prvisualizer.ui.shared

import io.github.hayatoyagi.prvisualizer.FileNode
import io.github.hayatoyagi.prvisualizer.PrFileChange
import io.github.hayatoyagi.prvisualizer.PullRequest
import kotlin.test.Test
import kotlin.test.assertTrue

class VisualizerSupportTest {
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
        val conflictedDirectoryPaths = computeConflictedDirectoryPaths(fileOverlayByPath)

        assertTrue(fileOverlayByPath.isEmpty())
        assertTrue(conflictedDirectoryPaths.isEmpty())
    }
}
