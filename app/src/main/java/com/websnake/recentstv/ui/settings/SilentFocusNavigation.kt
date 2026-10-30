package com.websnake.recentstv.ui.settings

import android.view.FocusFinder
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup

/** Consume native D-pad navigation before ViewRootImpl adds a focus sound.
 * The content root must exclude DecorView, whose callback would recurse into the window.
 */
object SilentFocusNavigation {
    fun dispatch(content: ViewGroup, event: KeyEvent): Boolean {
        if (!event.hasNoModifiers()) return false
        val direction = when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> View.FOCUS_UP
            KeyEvent.KEYCODE_DPAD_DOWN -> View.FOCUS_DOWN
            KeyEvent.KEYCODE_DPAD_LEFT -> View.FOCUS_LEFT
            KeyEvent.KEYCODE_DPAD_RIGHT -> View.FOCUS_RIGHT
            else -> return false
        }
        // Give text editing and scroll views their ordinary key behavior first.
        if (content.dispatchKeyEvent(event)) return true
        if (event.action == KeyEvent.ACTION_DOWN) {
            val current = content.findFocus()
            FocusFinder.getInstance().findNextFocus(content, current, direction)?.requestFocus(direction)
        }
        // Also consume boundaries and key-up; neither should reach system focus navigation.
        return true
    }
}
