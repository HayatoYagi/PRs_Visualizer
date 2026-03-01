package io.github.hayatoyagi.prvisualizer

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "GitHubPRsVisualizer",
        icon = painterResource("icon.png"),
    ) {
        App()
    }
}
