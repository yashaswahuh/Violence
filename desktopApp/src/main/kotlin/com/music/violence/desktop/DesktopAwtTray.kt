package com.music.violence.desktop

import java.awt.EventQueue
import java.awt.MouseInfo
import java.awt.Point
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

/** The tray icon on the platforms that have no StatusNotifierItem. */
internal object DesktopAwtTray {

    private val lock = Any()
    private var icon: TrayIcon? = null
    private var image: BufferedImage? = null
    private var tooltip: String = APP_NAME

    /** Whether this platform has a tray AWT can reach. */
    fun isSupported(): Boolean = runCatching { SystemTray.isSupported() }.getOrDefault(false)

    /** The logo, rasterised. */
    fun setIcon(rendered: BufferedImage?) {
        if (rendered == null) return
        synchronized(lock) {
            image = rendered
            icon?.image = rendered
        }
    }

    fun start(onActivate: () -> Unit) {
        if (!isSupported()) return
        EventQueue.invokeLater {
            synchronized(lock) {
                if (icon != null) return@invokeLater
                val tray = runCatching { SystemTray.getSystemTray() }.getOrNull() ?: return@invokeLater
                val art = image ?: blank(tray.trayIconSize.width.coerceAtLeast(16))
                val created = TrayIcon(art, tooltip, null).apply {
                    isImageAutoSize = true
                    addMouseListener(object : MouseAdapter() {
                        override fun mousePressed(e: MouseEvent) {
                            if (e.isPopupTrigger || e.button == MouseEvent.BUTTON3) {
                                showFlyout()
                            }
                        }

                        override fun mouseReleased(e: MouseEvent) {
                            if (e.isPopupTrigger || e.button == MouseEvent.BUTTON3) {
                                showFlyout()
                            }
                        }

                        override fun mouseClicked(e: MouseEvent) {
                            if (e.button == MouseEvent.BUTTON1) {
                                DesktopTrayFlyout.hide()
                                EventQueue.invokeLater(onActivate)
                            }
                        }
                    })
                }
                runCatching { tray.add(created) }
                    .onSuccess { icon = created }
                    .onFailure { DesktopTrackLog.log("tray unavailable: ${it.message}") }
            }
        }
    }

    private fun showFlyout() {
        val loc = MouseInfo.getPointerInfo()?.location ?: Point(0, 0)
        DesktopTrayFlyout.toggle(loc.x, loc.y)
    }

    fun stop() {
        EventQueue.invokeLater {
            DesktopTrayFlyout.hide()
            synchronized(lock) {
                val current = icon ?: return@invokeLater
                runCatching { SystemTray.getSystemTray().remove(current) }
                icon = null
            }
        }
    }

    /**
     * Re-labels the icon and keeps the tray flyout state in step with playback.
     */
    fun update(title: String?, isPlaying: Boolean) {
        val text = title?.let { "$APP_NAME — $it" } ?: APP_NAME
        EventQueue.invokeLater {
            synchronized(lock) {
                tooltip = text
                val current = icon ?: return@invokeLater
                current.toolTip = text
                DesktopTrayFlyout.update(title, isPlaying)
            }
        }
    }

    /** PNG bytes to an image AWT can hand the tray. */
    fun decode(png: ByteArray): BufferedImage? =
        runCatching { ImageIO.read(ByteArrayInputStream(png)) }.getOrNull()

    /** A placeholder while the logo is still being rasterised. */
    private fun blank(size: Int) = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)

    private const val APP_NAME = "Violence"
}
