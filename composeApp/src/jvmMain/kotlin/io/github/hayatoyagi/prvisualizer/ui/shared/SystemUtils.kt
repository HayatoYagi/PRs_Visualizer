package io.github.hayatoyagi.prvisualizer.ui.shared

import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.net.URI

/**
 * Opens a URL in the default system browser.
 *
 * @param url The URL to open
 */
fun openUrl(url: String) {
    runCatching {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(URI(url))
        }
    }
}

/**
 * Copies text to the system clipboard.
 *
 * @param text The text to copy
 */
fun copyToClipboard(text: String) {
    runCatching {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(StringSelection(text), null)
    }
}
