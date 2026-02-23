package io.github.hayatoyagi.prvisualizer.ui.explorer.badge

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class ExplorerBadgeSize(
    val badgeDp: Dp,
    val fontSp: TextUnit,
    val conflictFontSp: TextUnit,
) {
    Legend(14.dp, 9.sp, 10.sp),
    Row(16.dp, 11.sp, 11.sp),
}
