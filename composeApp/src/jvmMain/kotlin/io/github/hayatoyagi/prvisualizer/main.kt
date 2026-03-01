package io.github.hayatoyagi.prvisualizer

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() {
    val appDisplayName = System.getProperty("app.display.name", "GitHub PRs Visualizer")

    // Set macOS-specific system properties for proper application name
    System.setProperty("apple.awt.application.name", appDisplayName)
    System.setProperty("com.apple.mrj.application.apple.menu.about.name", appDisplayName)
    
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = appDisplayName,
        ) {
            App()
        }
    }
}
