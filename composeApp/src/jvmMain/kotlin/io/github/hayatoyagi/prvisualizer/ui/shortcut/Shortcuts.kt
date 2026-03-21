package io.github.hayatoyagi.prvisualizer.ui.shortcut

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import io.github.hayatoyagi.prvisualizer.VisualizerViewModel
import java.awt.KeyEventDispatcher
import java.awt.KeyboardFocusManager
import java.awt.Toolkit
import java.awt.event.KeyEvent

@Composable
fun RegisterShortcuts(vm: VisualizerViewModel) {
    RegisterResetViewportShortcut(vm)
}

@Composable
private fun RegisterResetViewportShortcut(vm: VisualizerViewModel) {
    DisposableEffect(vm) {
        val focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager()
        val shortcutMask = Toolkit.getDefaultToolkit().menuShortcutKeyMaskEx
        val dispatcher = KeyEventDispatcher { event ->
            if (event.id != KeyEvent.KEY_PRESSED) return@KeyEventDispatcher false
            if (event.keyCode != KeyEvent.VK_R) return@KeyEventDispatcher false
            if ((event.modifiersEx and shortcutMask) == 0) return@KeyEventDispatcher false
            if (focusManager.activeWindow == null) return@KeyEventDispatcher false
            vm.resetViewport()
            true
        }
        focusManager.addKeyEventDispatcher(dispatcher)
        onDispose {
            focusManager.removeKeyEventDispatcher(dispatcher)
        }
    }
}
