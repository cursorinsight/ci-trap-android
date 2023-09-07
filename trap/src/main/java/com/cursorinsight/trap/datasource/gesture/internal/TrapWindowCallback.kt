package com.cursorinsight.trap.datasource.gesture.internal

import android.view.ActionMode
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.SearchEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.cursorinsight.trap.util.TrapBackgroundExecutor

typealias TouchHandler = (event: MotionEvent?) -> Unit

/**
 * The Window.Callback implementation which
 * can be hooked up to the activity window
 * for touch and pointer collection.
 *
 * @property underlying The previous system callback.
 */
class TrapWindowCallback internal constructor(
    private val underlying: Window.Callback
): Window.Callback {

    companion object {
        // The set of touch handlers to notify
        private var touchHandlers = mutableListOf<TouchHandler>()

        /**
         * Adds a touch handler to the window callback.
         */
        fun addTouchHandler(handler: TouchHandler) {
            if (!touchHandlers.contains(handler)) {
                touchHandlers.add(handler)
            }
        }

        /**
         * Removes a touch handler from the window callback.
         */
        fun removeTouchHandler(handler: TouchHandler) {
            touchHandlers.remove(handler)
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        TrapBackgroundExecutor.run {
            touchHandlers.forEach { it(event) }
        }
        underlying.dispatchTouchEvent(event)

        return underlying.dispatchTouchEvent(event)
    }

    override fun dispatchKeyEvent(p0: KeyEvent?): Boolean {
        return underlying.dispatchKeyEvent(p0)
    }

    override fun dispatchKeyShortcutEvent(p0: KeyEvent?): Boolean {
        return underlying.dispatchKeyShortcutEvent(p0)
    }

    override fun dispatchTrackballEvent(p0: MotionEvent?): Boolean {
        return underlying.dispatchTrackballEvent(p0)
    }

    override fun dispatchGenericMotionEvent(p0: MotionEvent?): Boolean {
        return underlying.dispatchGenericMotionEvent(p0)
    }

    override fun dispatchPopulateAccessibilityEvent(p0: AccessibilityEvent?): Boolean {
        return underlying.dispatchPopulateAccessibilityEvent(p0)
    }

    override fun onCreatePanelView(p0: Int): View? {
        return underlying.onCreatePanelView(p0)
    }

    override fun onCreatePanelMenu(p0: Int, p1: Menu): Boolean {
        return underlying.onCreatePanelMenu(p0, p1)
    }

    override fun onPreparePanel(p0: Int, p1: View?, p2: Menu): Boolean {
        return underlying.onPreparePanel(p0, p1, p2)
    }

    override fun onMenuOpened(p0: Int, p1: Menu): Boolean {
        return underlying.onMenuOpened(p0, p1)
    }

    override fun onMenuItemSelected(p0: Int, p1: MenuItem): Boolean {
        return underlying.onMenuItemSelected(p0, p1)
    }

    override fun onWindowAttributesChanged(p0: WindowManager.LayoutParams?) {
        underlying.onWindowAttributesChanged(p0)
    }

    override fun onContentChanged() {
        underlying.onContentChanged()
    }

    override fun onWindowFocusChanged(p0: Boolean) {
        underlying.onWindowFocusChanged(p0)
    }

    override fun onAttachedToWindow() {
        underlying.onAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        underlying.onDetachedFromWindow()
    }

    override fun onPanelClosed(p0: Int, p1: Menu) {
        underlying.onPanelClosed(p0, p1)
    }

    override fun onSearchRequested(): Boolean {
        return underlying.onSearchRequested()
    }

    override fun onSearchRequested(p0: SearchEvent?): Boolean {
        return underlying.onSearchRequested(p0)
    }

    override fun onWindowStartingActionMode(p0: ActionMode.Callback?): ActionMode? {
        return underlying.onWindowStartingActionMode(p0)
    }

    override fun onWindowStartingActionMode(p0: ActionMode.Callback?, p1: Int): ActionMode? {
        return underlying.onWindowStartingActionMode(p0, p1)
    }

    override fun onActionModeStarted(p0: ActionMode?) {
        underlying.onActionModeStarted(p0)
    }

    override fun onActionModeFinished(p0: ActionMode?) {
        underlying.onActionModeFinished(p0)
    }
}