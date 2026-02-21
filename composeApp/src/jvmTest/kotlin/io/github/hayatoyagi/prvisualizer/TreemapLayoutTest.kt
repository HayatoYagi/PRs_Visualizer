package io.github.hayatoyagi.prvisualizer

import androidx.compose.ui.geometry.Rect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TreemapLayoutTest {

    @Test
    fun `computeTreemap should handle empty directory`() {
        val root = FileNode.Directory(
            path = "",
            name = "root",
            children = emptyList(),
            weight = 0.0
        )
        val bounds = Rect(0f, 0f, 100f, 100f)
        
        val nodes = computeTreemap(root, bounds)
        
        // Should only have the root node
        assertEquals(1, nodes.size)
        assertEquals("", nodes[0].path)
        assertTrue(nodes[0].isDirectory)
    }

    @Test
    fun `computeTreemap should handle single file`() {
        val file = FileNode.File(
            path = "file.txt",
            name = "file.txt",
            extension = "txt",
            totalLines = 100,
            hasActivePr = false,
            weight = 100.0
        )
        val root = FileNode.Directory(
            path = "",
            name = "root",
            children = listOf(file),
            weight = 100.0
        )
        val bounds = Rect(0f, 0f, 100f, 100f)
        
        val nodes = computeTreemap(root, bounds)
        
        // Should have root and file
        assertEquals(2, nodes.size)
        assertEquals("", nodes[0].path)
        assertEquals("file.txt", nodes[1].path)
        assertEquals(100, nodes[1].totalLines)
    }

    @Test
    fun `computeTreemap should create more square-like rectangles`() {
        // Create files with equal weights
        val files = (1..4).map { i ->
            FileNode.File(
                path = "file$i.txt",
                name = "file$i.txt",
                extension = "txt",
                totalLines = 100,
                hasActivePr = false,
                weight = 100.0
            )
        }
        val root = FileNode.Directory(
            path = "",
            name = "root",
            children = files,
            weight = 400.0
        )
        val bounds = Rect(0f, 0f, 100f, 100f)
        
        val nodes = computeTreemap(root, bounds)
        
        // Should have root + 4 files
        assertEquals(5, nodes.size)
        
        // Calculate average aspect ratio for files (should be closer to 1.0)
        val fileNodes = nodes.filter { !it.isDirectory }
        val aspectRatios = fileNodes.map { node ->
            val w = node.rect.width
            val h = node.rect.height
            if (w > h) w / h else h / w
        }
        
        // All aspect ratios should be reasonable (not extremely elongated)
        aspectRatios.forEach { ratio ->
            assertTrue(ratio < 4.0, "Aspect ratio $ratio is too high")
        }
    }

    @Test
    fun `computeTreemap should handle nested directories`() {
        val file1 = FileNode.File(
            path = "dir1/file1.txt",
            name = "file1.txt",
            extension = "txt",
            totalLines = 100,
            hasActivePr = false,
            weight = 100.0
        )
        val dir1 = FileNode.Directory(
            path = "dir1",
            name = "dir1",
            children = listOf(file1),
            weight = 100.0
        )
        val root = FileNode.Directory(
            path = "",
            name = "root",
            children = listOf(dir1),
            weight = 100.0
        )
        val bounds = Rect(0f, 0f, 100f, 100f)
        
        val nodes = computeTreemap(root, bounds)
        
        // Should have root + dir1 + file1
        assertEquals(3, nodes.size)
        assertEquals("", nodes[0].path)
        assertEquals("dir1", nodes[1].path)
        assertEquals("dir1/file1.txt", nodes[2].path)
    }

    @Test
    fun `computeTreemap should handle varying file sizes`() {
        // Create files with different weights (10, 20, 30, 40)
        val files = listOf(
            FileNode.File(path = "file1.txt", name = "file1.txt", extension = "txt", totalLines = 10, hasActivePr = false, weight = 10.0),
            FileNode.File(path = "file2.txt", name = "file2.txt", extension = "txt", totalLines = 20, hasActivePr = false, weight = 20.0),
            FileNode.File(path = "file3.txt", name = "file3.txt", extension = "txt", totalLines = 30, hasActivePr = false, weight = 30.0),
            FileNode.File(path = "file4.txt", name = "file4.txt", extension = "txt", totalLines = 40, hasActivePr = false, weight = 40.0),
        )
        val root = FileNode.Directory(
            path = "",
            name = "root",
            children = files,
            weight = 100.0
        )
        val bounds = Rect(0f, 0f, 100f, 100f)
        
        val nodes = computeTreemap(root, bounds)
        
        // Should have root + 4 files
        assertEquals(5, nodes.size)
        
        // Verify that larger files get more space
        val fileNodes = nodes.filter { !it.isDirectory }.sortedBy { it.totalLines }
        for (i in 0 until fileNodes.size - 1) {
            val area1 = fileNodes[i].rect.width * fileNodes[i].rect.height
            val area2 = fileNodes[i + 1].rect.width * fileNodes[i + 1].rect.height
            assertTrue(area1 < area2, "Larger files should have more area")
        }
    }

    @Test
    fun `computeTreemap should handle zero-weight files without crashing`() {
        val files = listOf(
            FileNode.File(path = "a.txt", name = "a.txt", extension = "txt", totalLines = 0, hasActivePr = false, weight = 0.0),
            FileNode.File(path = "b.txt", name = "b.txt", extension = "txt", totalLines = 0, hasActivePr = false, weight = 0.0),
        )
        val root = FileNode.Directory(
            path = "",
            name = "root",
            children = files,
            weight = 0.0,
        )
        val bounds = Rect(0f, 0f, 100f, 100f)

        val nodes = computeTreemap(root, bounds)

        nodes.forEach { node ->
            assertTrue(node.rect.width >= 0f, "Width should be non-negative for ${node.path}")
            assertTrue(node.rect.height >= 0f, "Height should be non-negative for ${node.path}")
        }
    }

    @Test
    fun `computeTreemap should avoid NaN rectangles for mixed positive and zero weights`() {
        val files = listOf(
            FileNode.File(path = "normal.txt", name = "normal.txt", extension = "txt", totalLines = 10, hasActivePr = false, weight = 10.0),
            FileNode.File(path = "empty1.txt", name = "empty1.txt", extension = "txt", totalLines = 0, hasActivePr = false, weight = 0.0),
            FileNode.File(path = "empty2.txt", name = "empty2.txt", extension = "txt", totalLines = 0, hasActivePr = false, weight = 0.0),
        )
        val root = FileNode.Directory(
            path = "",
            name = "root",
            children = files,
            weight = 10.0,
        )
        val bounds = Rect(0f, 0f, 100f, 100f)

        val nodes = computeTreemap(root, bounds)

        assertTrue(nodes.any { it.path == "normal.txt" }, "Positive-weight file should be laid out")
        nodes.forEach { node ->
            assertTrue(node.rect.left.isFinite(), "Left should be finite for ${node.path}")
            assertTrue(node.rect.top.isFinite(), "Top should be finite for ${node.path}")
            assertTrue(node.rect.right.isFinite(), "Right should be finite for ${node.path}")
            assertTrue(node.rect.bottom.isFinite(), "Bottom should be finite for ${node.path}")
        }
    }

    @Test
    fun `computeTreemap should not create negative-size rectangles`() {
        val files = (1..10).map { i ->
            FileNode.File(
                path = "file$i.txt",
                name = "file$i.txt",
                extension = "txt",
                totalLines = i * 10,
                hasActivePr = false,
                weight = (i * 10).toDouble()
            )
        }
        val root = FileNode.Directory(
            path = "",
            name = "root",
            children = files,
            weight = files.sumOf { it.weight }
        )
        val bounds = Rect(0f, 0f, 100f, 100f)
        
        val nodes = computeTreemap(root, bounds)
        
        // All rectangles should have positive width and height
        nodes.forEach { node ->
            assertTrue(node.rect.width >= 0f, "Width should be non-negative for ${node.path}")
            assertTrue(node.rect.height >= 0f, "Height should be non-negative for ${node.path}")
        }
    }
}
