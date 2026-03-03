package io.github.hayatoyagi.prvisualizer

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.github.hayatoyagi.prvisualizer.generated.resources.Res
import io.github.hayatoyagi.prvisualizer.generated.resources.icon
import org.jetbrains.compose.resources.painterResource
import java.awt.Taskbar
import javax.imageio.ImageIO
import javax.swing.JOptionPane

fun main() {
    try {
        val appDisplayName = System.getProperty("app.display.name", "PRs Visualizer for GitHub")
        // Ensure macOS menu/About uses the configured display name even when JVM args are not applied.
        System.setProperty("apple.awt.application.name", appDisplayName)
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", appDisplayName)

        setDockAndTaskbarIcon()

        application {
            Window(
                onCloseRequest = ::exitApplication,
                title = appDisplayName,
                icon = painterResource(Res.drawable.icon),
            ) {
                App()
            }
        }
    } catch (e: Throwable) {
        // Show error dialog on startup failure (especially important for Windows where console is not visible)
        e.printStackTrace()
        JOptionPane.showMessageDialog(
            null,
            "Failed to start application:\n${e.message}",
            "Startup Error",
            JOptionPane.ERROR_MESSAGE,
        )
        System.exit(1)
    }
}

private fun setDockAndTaskbarIcon() {
    if (!Taskbar.isTaskbarSupported()) return
    val taskbar = Taskbar.getTaskbar()
    if (!taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) return

    runCatching {
        // Use resource as stream instead of URI to avoid Windows path issues.
        // Note: We use Java ClassLoader resource loading here (not Compose resources)
        // because we need a java.awt.Image, not a Compose Painter.
        // Try common resource paths where Compose resources might be packaged.
        val resourcePaths = listOf(
            "/composeResources/drawable/icon.png",
            "/drawable/icon.png",
            "drawable/icon.png",
        )

        for (path in resourcePaths) {
            val iconStream = Res::class.java.getResourceAsStream(path) ?: continue
            iconStream.use { stream ->
                val image = ImageIO.read(stream) ?: return@use
                taskbar.iconImage = image
                return // Successfully loaded
            }
        }
    }.onFailure { e ->
        // Log but don't crash if icon loading fails
        System.err.println("Warning: Failed to set taskbar icon: ${e.message}")
    }
}
