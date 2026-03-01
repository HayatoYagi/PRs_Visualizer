package io.github.hayatoyagi.prvisualizer

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.awt.Taskbar
import javax.imageio.ImageIO

fun main() = application {
    setDockAndTaskbarIcon()

    Window(
        onCloseRequest = ::exitApplication,
        title = "GitHubPRsVisualizer",
        icon = painterResource("icon.png"),
    ) {
        App()
    }
}

private fun setDockAndTaskbarIcon() {
    if (!Taskbar.isTaskbarSupported()) return

    runCatching {
        Thread.currentThread().contextClassLoader.getResourceAsStream("icon.png")?.use { iconStream ->
            ImageIO.read(iconStream)?.let { image ->
                Taskbar.getTaskbar().iconImage = image
            }
        }
    }
}
