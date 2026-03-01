package io.github.hayatoyagi.prvisualizer

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.github.hayatoyagi.prvisualizer.generated.resources.Res
import io.github.hayatoyagi.prvisualizer.generated.resources.icon
import java.awt.Taskbar
import java.net.URI
import javax.imageio.ImageIO
import org.jetbrains.compose.resources.painterResource

fun main() = application {
    setDockAndTaskbarIcon()

    Window(
        onCloseRequest = ::exitApplication,
        title = "GitHubPRsVisualizer",
        icon = painterResource(Res.drawable.icon),
    ) {
        App()
    }
}

private fun setDockAndTaskbarIcon() {
    if (!Taskbar.isTaskbarSupported()) return

    runCatching {
        val iconUri = URI.create(Res.getUri("drawable/icon.png")).toURL()
        ImageIO.read(iconUri)?.let { image ->
            Taskbar.getTaskbar().iconImage = image
        }
    }
}
