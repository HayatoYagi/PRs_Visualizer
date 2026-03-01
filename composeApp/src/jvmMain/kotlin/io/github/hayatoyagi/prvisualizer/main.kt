package io.github.hayatoyagi.prvisualizer

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.github.hayatoyagi.prvisualizer.generated.resources.Res
import io.github.hayatoyagi.prvisualizer.generated.resources.icon
import org.jetbrains.compose.resources.painterResource
import java.awt.Taskbar
import java.net.URI
import javax.imageio.ImageIO

fun main() {
    val appDisplayName = System.getProperty("app.display.name", "GitHub PRs Visualizer")

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
}

private fun setDockAndTaskbarIcon() {
    if (!Taskbar.isTaskbarSupported()) return
    val taskbar = Taskbar.getTaskbar()
    if (!taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) return

    runCatching {
        val iconUri = URI.create(Res.getUri("drawable/icon.png")).toURL()
        ImageIO.read(iconUri)?.let { image ->
            taskbar.iconImage = image
        }
    }
}
