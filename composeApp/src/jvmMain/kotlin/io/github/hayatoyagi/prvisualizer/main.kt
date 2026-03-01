package io.github.hayatoyagi.prvisualizer

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

/**
 * Main entry point for the application.
 */
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "GitHubPRsVisualizer",
    ) {
        App()
    }
}
