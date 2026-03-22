package io.github.hayatoyagi.prvisualizer.ui.shortcut

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import io.github.hayatoyagi.prvisualizer.VisualizerViewModel
import java.awt.KeyEventDispatcher
import java.awt.KeyboardFocusManager
import java.awt.Toolkit
import java.awt.event.InputEvent
import java.awt.event.KeyEvent

@Composable
fun RegisterShortcuts(vm: VisualizerViewModel) {
    RegisterResetViewportShortcut(vm)
}

@Composable
private fun RegisterResetViewportShortcut(vm: VisualizerViewModel) {
    DisposableEffect(vm) {
        val focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager()
        val shortcutMask = defaultShortcutMask()
        val dispatcher = KeyEventDispatcher { event ->
            if (!event.isViewportResetShortcut(shortcutMask)) return@KeyEventDispatcher false
            if (!focusManager.hasActiveWindow()) return@KeyEventDispatcher false
            vm.resetViewport()
            true
        }
        focusManager.addKeyEventDispatcher(dispatcher)
        onDispose {
            focusManager.removeKeyEventDispatcher(dispatcher)
        }
    }
}

private fun KeyEvent.isViewportResetShortcut(shortcutMask: Int): Boolean =
    isViewportResetShortcut(id, keyCode, modifiersEx, shortcutMask)

internal fun viewportResetShortcutHint(shortcutMask: Int = defaultShortcutMask()): String =
    "${shortcutModifierLabel(shortcutMask)}+R: reset view"

internal fun isViewportResetShortcut(id: Int, keyCode: Int, modifiersEx: Int, shortcutMask: Int): Boolean {
    if (id != KeyEvent.KEY_PRESSED) return false
    if (keyCode != KeyEvent.VK_R) return false
    if (modifiersEx and shortcutMask == 0) return false
    return true
}

private fun defaultShortcutMask(): Int = Toolkit.getDefaultToolkit().menuShortcutKeyMaskEx

private fun shortcutModifierLabel(shortcutMask: Int): String =
    if (shortcutMask and InputEvent.META_DOWN_MASK != 0) "Cmd" else "Ctrl"

private fun KeyboardFocusManager.hasActiveWindow(): Boolean = activeWindow != null
