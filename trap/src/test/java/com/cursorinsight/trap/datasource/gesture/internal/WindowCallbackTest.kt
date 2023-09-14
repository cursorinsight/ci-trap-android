package com.cursorinsight.trap.datasource.gesture.internal

import android.util.Log
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
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class WindowCallbackTest {
    private lateinit var callbackHandler: TrapWindowCallback

    private var windowCallback: Window.Callback = run {
        val callback = mockk<Window.Callback>()
        every { callback.dispatchTouchEvent(any()) } returns true
        every { callback.dispatchKeyEvent(any()) } returns true
        every { callback.dispatchKeyShortcutEvent(any()) } returns true
        every { callback.dispatchTrackballEvent(any()) } returns true
        every { callback.dispatchGenericMotionEvent(any()) } returns true
        every { callback.dispatchPopulateAccessibilityEvent(any()) } returns true
        every { callback.onCreatePanelView(any()) } returns View(mockk())
        every { callback.onCreatePanelMenu(any(), any()) } returns true
        every { callback.onPreparePanel(any(), any(), any()) } returns true
        every { callback.onMenuOpened(any(), any()) } returns true
        every { callback.onMenuItemSelected(any(), any()) } returns true
        every { callback.onWindowAttributesChanged(any()) } returns Unit
        every { callback.onContentChanged() } returns Unit
        every { callback.onWindowFocusChanged(any()) } returns Unit
        every { callback.onAttachedToWindow() } returns Unit
        every { callback.onDetachedFromWindow() } returns Unit
        every { callback.onPanelClosed(any(), any()) } returns Unit
        every { callback.onSearchRequested() } returns true
        every { callback.onSearchRequested(any()) } returns true
        every { callback.onWindowStartingActionMode(any()) } returns mockk()
        every { callback.onWindowStartingActionMode(any(), any()) } returns mockk()
        every { callback.onActionModeStarted(any()) } returns Unit
        every { callback.onActionModeFinished(any()) } returns Unit

        callback
    }

    @BeforeEach
    fun setUp() {
        callbackHandler = TrapWindowCallback(windowCallback)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test all callbacks are handled`() {
        val motionEvent: MotionEvent = mockk()
        callbackHandler.dispatchTouchEvent(motionEvent)
        verify(exactly = 1) { windowCallback.dispatchTouchEvent(motionEvent) }

        val keyEvent: KeyEvent = mockk()
        callbackHandler.dispatchKeyEvent(keyEvent)
        verify(exactly = 1) { windowCallback.dispatchKeyEvent(keyEvent) }

        callbackHandler.dispatchKeyShortcutEvent(keyEvent)
        verify(exactly = 1) { windowCallback.dispatchKeyShortcutEvent(keyEvent) }

        callbackHandler.dispatchTrackballEvent(motionEvent)
        verify(exactly = 1) { windowCallback.dispatchTrackballEvent(motionEvent) }

        callbackHandler.dispatchGenericMotionEvent(motionEvent)
        verify(exactly = 1) { windowCallback.dispatchGenericMotionEvent(motionEvent) }

        val accessibilityEvent: AccessibilityEvent = mockk()
        callbackHandler.dispatchPopulateAccessibilityEvent(accessibilityEvent)
        verify(exactly = 1) { windowCallback.dispatchPopulateAccessibilityEvent(accessibilityEvent) }

        callbackHandler.onCreatePanelView(1)
        verify(exactly = 1) { windowCallback.onCreatePanelView(1) }

        val menu: Menu = mockk()
        callbackHandler.onCreatePanelMenu(2, menu)
        verify(exactly = 1) { windowCallback.onCreatePanelMenu(2, menu) }

        val view: View = mockk()
        callbackHandler.onPreparePanel(3, view, menu)
        verify(exactly = 1) { windowCallback.onPreparePanel(3, view, menu) }

        callbackHandler.onMenuOpened(4, menu)
        verify(exactly = 1) { windowCallback.onMenuOpened(4, menu) }

        val menuItem: MenuItem = mockk()
        callbackHandler.onMenuItemSelected(5, menuItem)
        verify(exactly = 1) { windowCallback.onMenuItemSelected(5, menuItem) }

        val layoutParams: WindowManager.LayoutParams = mockk()
        callbackHandler.onWindowAttributesChanged(layoutParams)
        verify(exactly = 1) { windowCallback.onWindowAttributesChanged(layoutParams) }

        callbackHandler.onContentChanged()
        verify(exactly = 1) { windowCallback.onContentChanged() }

        callbackHandler.onWindowFocusChanged(true)
        verify(exactly = 1) { windowCallback.onWindowFocusChanged(true) }

        callbackHandler.onAttachedToWindow()
        verify(exactly = 1) { windowCallback.onAttachedToWindow() }

        callbackHandler.onDetachedFromWindow()
        verify(exactly = 1) { windowCallback.onDetachedFromWindow() }

        callbackHandler.onPanelClosed(6, menu)
        verify(exactly = 1) { windowCallback.onPanelClosed(6, menu) }

        callbackHandler.onSearchRequested()
        verify(exactly = 1) { windowCallback.onSearchRequested() }

        val searchEvent: SearchEvent = mockk()
        callbackHandler.onSearchRequested(searchEvent)
        verify(exactly = 1) { windowCallback.onSearchRequested(searchEvent) }

        val actionCallback: ActionMode.Callback = mockk()
        callbackHandler.onWindowStartingActionMode(actionCallback)
        verify(exactly = 1) { windowCallback.onWindowStartingActionMode(actionCallback) }

        callbackHandler.onWindowStartingActionMode(actionCallback,7)
        verify(exactly = 1) { windowCallback.onWindowStartingActionMode(actionCallback, 7) }

        val actionMode: ActionMode = mockk()
        callbackHandler.onActionModeStarted(actionMode)
        verify(exactly = 1) { windowCallback.onActionModeStarted(actionMode) }

        callbackHandler.onActionModeFinished(actionMode)
        verify(exactly = 1) { windowCallback.onActionModeFinished(actionMode) }
    }

    companion object {
        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            mockkStatic(Log::class)
            every { Log.v(any(), any()) } returns 0
            every { Log.d(any(), any()) } returns 0
            every { Log.i(any(), any()) } returns 0
            every { Log.e(any(), any()) } returns 0
            every { Log.w(ofType(String::class), ofType(String::class)) } returns 0
        }
    }
}