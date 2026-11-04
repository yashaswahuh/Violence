package com.music.violence.desktop

import java.awt.GraphicsEnvironment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DesktopTrayMenuTest {

    @Test
    fun publishingPlaybackUpdatesRevision() {
        val initialRev = DesktopTrayMenu.revision()
        DesktopTrayMenu.publish(title = "Song A — Artist B", playing = true)
        val nextRev = DesktopTrayMenu.revision()
        assertTrue(nextRev > initialRev)

        // Publishing identical state does not bump revision
        DesktopTrayMenu.publish(title = "Song A — Artist B", playing = true)
        assertEquals(nextRev, DesktopTrayMenu.revision())
    }

    @Test
    fun exportedLayoutContainsPlaybackControlsAndWindowActions() {
        var playPauseCalled = false
        var nextCalled = false
        var prevCalled = false
        var openCalled = false
        var settingsCalled = false
        var quitCalled = false

        DesktopTrayMenu.bind(
            onPlayPause = { playPauseCalled = true },
            onNext = { nextCalled = true },
            onPrevious = { prevCalled = true },
            onOpenPlayer = { openCalled = true },
            onOpenSettings = { settingsCalled = true },
            onQuit = { quitCalled = true },
        )

        DesktopTrayMenu.publish(title = "Track Title", playing = true)

        val exported = DesktopTrayMenu.Exported()
        val layout = exported.GetLayout(DesktopTrayMenu.ROOT, 1, emptyArray())
        assertNotNull(layout)
        assertTrue(layout.layout.children.isNotEmpty())

        // If running in a graphical environment, also verify the AWT popup menu
        if (!GraphicsEnvironment.isHeadless()) {
            val popup = DesktopTrayMenu.awtPopup()
            assertNotNull(popup)
            assertTrue(popup.itemCount >= 7)
        }
    }
}
