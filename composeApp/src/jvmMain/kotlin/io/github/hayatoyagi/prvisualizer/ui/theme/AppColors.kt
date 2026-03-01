@file:Suppress("MagicNumber")

package io.github.hayatoyagi.prvisualizer.ui.theme

import androidx.compose.ui.graphics.Color
import io.github.hayatoyagi.prvisualizer.PullRequest

/**
 * Determines a color for an author based on their username.
 *
 * @param author The author's username
 * @return A color from the author palette based on a hash of the author name
 */
fun authorColor(author: String): Color = AppColors.authorPalette[author.hashCode().ushr(1) % AppColors.authorPalette.size]

/**
 * Determines the color for a pull request.
 *
 * @param pr The pull request to get the color for
 * @param colorMap Map of PR IDs to assigned colors
 * @return The color for the PR from the map, or a fallback color based on author
 */
fun prColor(
    pr: PullRequest,
    colorMap: Map<String, Color>,
): Color = colorMap[pr.id] ?: authorColor("${pr.author}:${pr.number}")

object AppColors {
    // Backgrounds
    val backgroundMain = Color(0xFF101820)
    val backgroundHeader = Color(0xFF0F2832)
    val backgroundCanvas = Color(0xFF0B1117)
    val backgroundPane = Color(0xFF18212B)
    val backgroundPaneList = Color(0xFF13202B)

    // Explorer highlights
    val explorerSelectionFocused = Color(0xFF2A455B)
    val explorerSelectionFile = Color(0xFF2B3A4A)
    val explorerNodeDir = Color(0xFFFFD37A)
    val explorerNodeFile = Color(0xFF96B2C8)
    val explorerAncestorText = Color(0xFFFFE4A5)

    // PR list items
    val prItemNormal = Color(0xFF203041)
    val prItemDraft = Color(0xFF253748)
    val prListDivider = Color(0xFF2C3D4E)

    // Text roles
    val textPrimary = Color(0xFFDCEAF5)
    val textSecondary = Color(0xFF9EC4DD)
    val textWarning = Color(0xFFFFC107)
    val textDeviceCode = Color(0xFFFFE082)
    val textError = Color(0xFFFF8A80)
    val textPaneTitle = Color(0xFFE8F1F8)
    val textBody = Color(0xFFD7E4EE)
    val textBodyMuted = Color(0xFFD1DEEB)
    val textMeta = Color(0xFFB5C8D8)
    val textHint = Color(0xFF8FA8BC)
    val textPrItem = Color(0xFFEAF2F8)
    val textCanvasLabel = Color(0xFFDAE8F3)
    val textTooltip = Color(0xFFC5D8E7)
    val textTooltipMultiPr = explorerNodeDir
    val textRepoOption = Color(0xFFF3FBFF)

    // Treemap fills
    val treemapAddition = Color(0xFF3CA65F)
    val treemapModification = Color(0xFFD2A43F)
    val treemapDeletion = Color(0xFFCB4A44)
    val treemapNeutralDir = Color(0xFF264155)
    val treemapNeutralFile = Color(0xFF1C2A36)
    val treemapFallbackBorderDir = Color(0xFF2F4A5F)
    val treemapFallbackBorderFile = Color(0xFF2A3D4E)
    val treemapActivePrDot = Color(0xFFFFB800)
    val treemapConflictStripe = Color(0xAAFFE082)

    // Tooltip chrome
    val tooltipBackground = Color(0xFF12212F)
    val tooltipBorder = Color(0xFF3E5A72)

    // 24-color author palette (used in authorColor() and prColor())
    val authorPalette: List<Color> = listOf(
        Color(0xFF4FC3F7),
        Color(0xFF81C784),
        Color(0xFFFF8A65),
        Color(0xFFBA68C8),
        Color(0xFFFFD54F),
        Color(0xFF90A4AE),
        Color(0xFF64B5F6),
        Color(0xFFA5D6A7),
        Color(0xFFFFB74D),
        Color(0xFFE57373),
        Color(0xFF4DB6AC),
        Color(0xFFF06292),
        Color(0xFF7986CB),
        Color(0xFFFFA726),
        Color(0xFF26A69A),
        Color(0xFFDCE775),
        Color(0xFF9575CD),
        Color(0xFFFF7043),
        Color(0xFF29B6F6),
        Color(0xFF8D6E63),
        Color(0xFF9CCC65),
        Color(0xFFFFCA28),
        Color(0xFF66BB6A),
        Color(0xFF42A5F5),
    )
}
