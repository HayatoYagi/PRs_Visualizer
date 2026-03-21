package io.github.hayatoyagi.prvisualizer.ui.shortcut

import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShortcutsTest {
    @Test
    fun `Ctrl+R is detected as viewport reset shortcut (Windows)`() {
        assertTrue(
            isViewportResetShortcut(
                id = KeyEvent.KEY_PRESSED,
                keyCode = KeyEvent.VK_R,
                modifiersEx = InputEvent.CTRL_DOWN_MASK,
                shortcutMask = InputEvent.CTRL_DOWN_MASK,
            ),
        )
    }

    @Test
    fun `Meta+R is detected as viewport reset shortcut (macOS)`() {
        assertTrue(
            isViewportResetShortcut(
                id = KeyEvent.KEY_PRESSED,
                keyCode = KeyEvent.VK_R,
                modifiersEx = InputEvent.META_DOWN_MASK,
                shortcutMask = InputEvent.META_DOWN_MASK,
            ),
        )
    }

    @Test
    fun `R without modifier is not detected as viewport reset shortcut`() {
        assertFalse(
            isViewportResetShortcut(
                id = KeyEvent.KEY_PRESSED,
                keyCode = KeyEvent.VK_R,
                modifiersEx = 0,
                shortcutMask = InputEvent.CTRL_DOWN_MASK,
            ),
        )
    }

    @Test
    fun `Ctrl+other key is not detected as viewport reset shortcut`() {
        assertFalse(
            isViewportResetShortcut(
                id = KeyEvent.KEY_PRESSED,
                keyCode = KeyEvent.VK_S,
                modifiersEx = InputEvent.CTRL_DOWN_MASK,
                shortcutMask = InputEvent.CTRL_DOWN_MASK,
            ),
        )
    }

    @Test
    fun `KEY_RELEASED event is not detected as viewport reset shortcut`() {
        assertFalse(
            isViewportResetShortcut(
                id = KeyEvent.KEY_RELEASED,
                keyCode = KeyEvent.VK_R,
                modifiersEx = InputEvent.CTRL_DOWN_MASK,
                shortcutMask = InputEvent.CTRL_DOWN_MASK,
            ),
        )
    }

    @Test
    fun `Meta+R is not detected as viewport reset shortcut when shortcutMask is Ctrl (Windows)`() {
        assertFalse(
            isViewportResetShortcut(
                id = KeyEvent.KEY_PRESSED,
                keyCode = KeyEvent.VK_R,
                modifiersEx = InputEvent.META_DOWN_MASK,
                shortcutMask = InputEvent.CTRL_DOWN_MASK,
            ),
        )
    }
}
