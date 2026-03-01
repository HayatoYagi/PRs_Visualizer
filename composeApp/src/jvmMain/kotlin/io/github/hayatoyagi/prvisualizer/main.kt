package io.github.hayatoyagi.prvisualizer

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() {
    // Set macOS-specific system properties for proper application name
    System.setProperty("apple.awt.application.name", "GitHub PRs Visualizer")
    System.setProperty("com.apple.mrj.application.apple.menu.about.name", "GitHub PRs Visualizer")
    
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "GitHub PRs Visualizer",
        ) {
            App()
        }
    }
}
