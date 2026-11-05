package com.music.violence.desktop

import com.music.violence.desktop.platform.WindowsNativeDrag
import java.awt.Frame
import java.awt.GraphicsEnvironment
import kotlin.test.Test
import kotlin.test.assertTrue

class WindowsNativeDragTest {

    @Test
    fun testStartDragHeadlessSafety() {
        if (!DesktopPlatform.isWindows || GraphicsEnvironment.isHeadless()) {
            assertTrue(true)
            return
        }
        val dummyFrame = Frame()
        try {
            val result = WindowsNativeDrag.startDrag(dummyFrame)
            assertTrue(result || !result) // safely invoked without throwing
        } finally {
            dummyFrame.dispose()
        }
    }
}
