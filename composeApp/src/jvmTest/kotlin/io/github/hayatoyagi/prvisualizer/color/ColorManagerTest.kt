package io.github.hayatoyagi.prvisualizer.color

import io.github.hayatoyagi.prvisualizer.ColorState
import io.github.hayatoyagi.prvisualizer.PullRequest
import io.github.hayatoyagi.prvisualizer.ui.theme.AppColors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertNotNull

class ColorManagerTest {
    @Test
    fun `ensurePrColors should assign colors to new PRs`() {
        var capturedState: ColorState? = null
        val manager = ColorManager(
            colorState = ColorState(),
            onStateChanged = { capturedState = it },
        )

        val prs = listOf(
            PullRequest("pr1", 1, "Title 1", "author1", false, "url1", emptyList()),
            PullRequest("pr2", 2, "Title 2", "author2", false, "url2", emptyList()),
        )

        manager.ensurePrColors(prs)

        assertNotNull(capturedState)
        assertEquals(2, capturedState?.prColorMap?.size)
        assertNotNull(capturedState?.prColorMap?.get("pr1"))
        assertNotNull(capturedState?.prColorMap?.get("pr2"))
    }

    @Test
    fun `ensurePrColors should not reassign existing colors`() {
        var capturedState: ColorState? = null
        val manager = ColorManager(
            colorState = ColorState(),
            onStateChanged = { capturedState = it },
        )

        val pr1 = PullRequest("pr1", 1, "Title 1", "author1", false, "url1", emptyList())

        manager.ensurePrColors(listOf(pr1))
        val color1 = capturedState?.prColorMap?.get("pr1")

        // Update the manager with the new state
        capturedState?.let { manager.updateState(it) }

        // Call again with same PR
        manager.ensurePrColors(listOf(pr1))
        assertEquals(color1, capturedState?.prColorMap?.get("pr1"))
    }

    @Test
    fun `shufflePrColors should reassign all colors`() {
        var capturedState: ColorState? = null
        val manager = ColorManager(
            colorState = ColorState(),
            onStateChanged = { capturedState = it },
        )

        val prs = listOf(
            PullRequest("pr1", 1, "Title 1", "author1", false, "url1", emptyList()),
            PullRequest("pr2", 2, "Title 2", "author2", false, "url2", emptyList()),
        )

        manager.ensurePrColors(prs)
        capturedState?.let { manager.updateState(it) }

        manager.shufflePrColors(prs)

        assertNotNull(capturedState)
        assertEquals(2, capturedState?.prColorMap?.size)
        assertNotNull(capturedState?.prColorMap?.get("pr1"))
        assertNotNull(capturedState?.prColorMap?.get("pr2"))
    }

    @Test
    fun `cyclePrColor should cycle through palette`() {
        var capturedState: ColorState? = null
        val manager = ColorManager(
            colorState = ColorState(),
            onStateChanged = { capturedState = it },
        )

        val pr = PullRequest("pr1", 1, "Title", "author", false, "url", emptyList())

        manager.ensurePrColors(listOf(pr))
        capturedState?.let { manager.updateState(it) }

        val initialColor = capturedState?.prColorMap?.get("pr1")
        val initialIndex = AppColors.authorPalette.indexOf(initialColor)

        manager.cyclePrColor("pr1")

        val newColor = capturedState?.prColorMap?.get("pr1")
        val expectedIndex = (initialIndex + 1) % AppColors.authorPalette.size
        assertEquals(AppColors.authorPalette[expectedIndex], newColor)
    }

    @Test
    fun `cyclePrColor should handle PR without assigned color`() {
        var capturedState: ColorState? = null
        val manager = ColorManager(
            colorState = ColorState(),
            onStateChanged = { capturedState = it },
        )

        manager.cyclePrColor("pr1")

        // Should assign the first color in the palette
        assertEquals(AppColors.authorPalette[0], capturedState?.prColorMap?.get("pr1"))
    }

    @Test
    fun `updateState should update internal state`() {
        var capturedState: ColorState? = null
        val manager = ColorManager(
            colorState = ColorState(),
            onStateChanged = { capturedState = it },
        )

        val newState = ColorState(prColorMap = mapOf("pr1" to AppColors.authorPalette[0]))
        manager.updateState(newState)

        // Verify state was updated by calling ensurePrColors
        val pr1 = PullRequest("pr1", 1, "Title", "author", false, "url", emptyList())
        manager.ensurePrColors(listOf(pr1))

        // No-op path should not trigger callback because state is already populated.
        assertNull(capturedState)

        // Verifies internal state was updated by requiring the next color for a new PR.
        val pr2 = PullRequest("pr2", 2, "Title 2", "author2", false, "url2", emptyList())
        manager.ensurePrColors(listOf(pr1, pr2))

        assertNotNull(capturedState)
        assertEquals(2, capturedState?.prColorMap?.size)
        assertEquals(AppColors.authorPalette[0], capturedState?.prColorMap?.get("pr1"))
    }
}
