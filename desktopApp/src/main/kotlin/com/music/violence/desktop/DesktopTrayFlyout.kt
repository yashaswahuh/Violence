package com.music.violence.desktop

import java.awt.AlphaComposite
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Cursor
import java.awt.Dimension
import java.awt.EventQueue
import java.awt.Font
import java.awt.FontMetrics
import java.awt.GradientPaint
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.GraphicsEnvironment
import java.awt.RenderingHints
import java.awt.Toolkit
import java.awt.Window
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.geom.Path2D
import java.awt.geom.RoundRectangle2D
import javax.swing.JComponent
import javax.swing.JDialog
import javax.swing.JPanel
import javax.swing.KeyStroke

/**
 * Modern, dark obsidian tray flyout matching Violence's design language.
 *
 * Designed with Violence's signature frosted glass surfaces, glowing crimson accents,
 * crisp anti-aliased vector iconography, Now Playing transport controls, and direct
 * shortcuts to Mini Player (Ctrl+M), settings, and window actions.
 */
internal object DesktopTrayFlyout {

    private var dialog: JDialog? = null
    private var flyoutPanel: FlyoutPanel? = null

    private var currentTitle: String = "Violence"
    private var currentArtist: String = "Not playing"
    private var isPlaying: Boolean = false

    fun update(title: String?, isPlaying: Boolean) {
        this.isPlaying = isPlaying
        if (title.isNullOrBlank()) {
            currentTitle = "Violence"
            currentArtist = "Not playing"
        } else {
            val separator = when {
                title.contains(" — ") -> " — "
                title.contains(" - ") -> " - "
                else -> null
            }
            if (separator != null) {
                currentTitle = title.substringBefore(separator).trim()
                currentArtist = title.substringAfter(separator).trim()
            } else {
                currentTitle = title.trim()
                currentArtist = "Violence"
            }
        }

        EventQueue.invokeLater {
            flyoutPanel?.repaint()
        }
    }

    fun showAt(targetX: Int, targetY: Int) {
        EventQueue.invokeLater {
            ensureDialog()
            val d = dialog ?: return@invokeLater

            val width = 296
            val height = 316
            d.setSize(width, height)

            val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
            val gc = ge.defaultScreenDevice.defaultConfiguration
            val bounds = gc.bounds
            val insets = Toolkit.getDefaultToolkit().getScreenInsets(gc)

            val usableX = bounds.x + insets.left
            val usableY = bounds.y + insets.top
            val usableWidth = bounds.width - insets.left - insets.right
            val usableHeight = bounds.height - insets.top - insets.bottom

            // Center horizontally on click, clamped inside screen
            val x = (targetX - width / 2).coerceIn(usableX + 8, usableX + usableWidth - width - 8)

            // If taskbar is at bottom (default on Windows): place above taskbar
            val y = if (targetY > usableY + usableHeight / 2) {
                (usableY + usableHeight - height - 8).coerceAtLeast(usableY + 8)
            } else {
                (usableY + 8).coerceAtMost(usableY + usableHeight - height - 8)
            }

            d.setLocation(x, y)
            d.isVisible = true
            d.toFront()
            d.requestFocus()
        }
    }

    fun toggle(targetX: Int, targetY: Int) {
        if (dialog?.isVisible == true) {
            hide()
        } else {
            showAt(targetX, targetY)
        }
    }

    fun hide() {
        EventQueue.invokeLater {
            dialog?.isVisible = false
        }
    }

    private fun ensureDialog() {
        if (dialog != null) return

        val d = JDialog(null as java.awt.Frame?).apply {
            isUndecorated = true
            type = Window.Type.POPUP
            isAlwaysOnTop = true
            background = Color(0, 0, 0, 0)
            focusableWindowState = true
        }

        val panel = FlyoutPanel()
        flyoutPanel = panel
        d.contentPane = panel

        d.addWindowFocusListener(object : WindowAdapter() {
            override fun windowLostFocus(e: WindowEvent?) {
                DesktopTrayFlyout.hide()
            }
        })

        panel.registerKeyboardAction(
            { DesktopTrayFlyout.hide() },
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW,
        )

        dialog = d
    }

    private class FlyoutPanel : JPanel() {
        private var hoveredItem: Int = -1
        private var hoveredControl: Int = -1

        init {
            isOpaque = false
            layout = null

            val mouseHandler = object : MouseAdapter() {
                override fun mouseMoved(e: MouseEvent) {
                    val prevItem = hoveredItem
                    val prevControl = hoveredControl
                    hoveredItem = getItemAt(e.x, e.y)
                    hoveredControl = getControlAt(e.x, e.y)
                    if (hoveredItem != prevItem || hoveredControl != prevControl) {
                        cursor = if (hoveredItem != -1 || hoveredControl != -1) {
                            Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                        } else {
                            Cursor.getDefaultCursor()
                        }
                        repaint()
                    }
                }

                override fun mouseExited(e: MouseEvent) {
                    if (hoveredItem != -1 || hoveredControl != -1) {
                        hoveredItem = -1
                        hoveredControl = -1
                        cursor = Cursor.getDefaultCursor()
                        repaint()
                    }
                }

                override fun mousePressed(e: MouseEvent) {
                    if (e.button != MouseEvent.BUTTON1) return
                    val control = getControlAt(e.x, e.y)
                    if (control != -1) {
                        when (control) {
                            CONTROL_PREV -> DesktopTrayMenu.triggerPrevious()
                            CONTROL_PLAY_PAUSE -> DesktopTrayMenu.triggerPlayPause()
                            CONTROL_NEXT -> DesktopTrayMenu.triggerNext()
                        }
                        return
                    }

                    val item = getItemAt(e.x, e.y)
                    if (item != -1) {
                        DesktopTrayFlyout.hide()
                        when (item) {
                            ITEM_OPEN -> DesktopTrayMenu.triggerOpenPlayer()
                            ITEM_MINI_PLAYER -> DesktopTrayMenu.triggerToggleMiniPlayer()
                            ITEM_MINIMIZE -> DesktopTrayMenu.triggerMinimizeToTray()
                            ITEM_SETTINGS -> DesktopTrayMenu.triggerOpenSettings()
                            ITEM_QUIT -> DesktopTrayMenu.triggerQuit()
                        }
                    }
                }
            }

            addMouseListener(mouseHandler)
            addMouseMotionListener(mouseHandler)
        }

        private fun getControlAt(x: Int, y: Int): Int {
            if (y !in 66..102) return -1
            val cardCenterX = width / 2
            return when (x) {
                in (cardCenterX - 65)..(cardCenterX - 25) -> CONTROL_PREV
                in (cardCenterX - 20)..(cardCenterX + 20) -> CONTROL_PLAY_PAUSE
                in (cardCenterX + 25)..(cardCenterX + 65) -> CONTROL_NEXT
                else -> -1
            }
        }

        private fun getItemAt(x: Int, y: Int): Int {
            if (x !in 8..(width - 8)) return -1
            return when (y) {
                in 120..152 -> ITEM_OPEN
                in 154..186 -> ITEM_MINI_PLAYER
                in 188..220 -> ITEM_MINIMIZE
                in 222..254 -> ITEM_SETTINGS
                in 264..298 -> ITEM_QUIT
                else -> -1
            }
        }

        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            val g2 = g as? Graphics2D ?: return
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB)
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)

            val w = width
            val h = height

            // 1. Outer Container: Violence obsidian glass
            val bgRect = RoundRectangle2D.Float(1f, 1f, w - 2f, h - 2f, 18f, 18f)
            val bgGradient = GradientPaint(0f, 0f, Color(0x15, 0x15, 0x1A), 0f, h.toFloat(), Color(0x0C, 0x0C, 0x0F))
            g2.paint = bgGradient
            g2.fill(bgRect)

            // 2. Dual-tone glass rim border (specular white top, subtle crimson glow bottom)
            val rimGradient = GradientPaint(0f, 0f, Color(255, 255, 255, 40), 0f, h.toFloat(), Color(0xE2, 0x2D, 0x4B, 55))
            g2.paint = rimGradient
            g2.stroke = BasicStroke(1f)
            g2.draw(bgRect)

            // Inner top specular sheen
            g2.color = Color(255, 255, 255, 18)
            g2.drawLine(20, 2, w - 20, 2)

            // --- 3. Now Playing Card ---
            val cardX = 10
            val cardY = 10
            val cardW = w - 20
            val cardH = 98
            val cardRect = RoundRectangle2D.Float(cardX.toFloat(), cardY.toFloat(), cardW.toFloat(), cardH.toFloat(), 14f, 14f)
            g2.color = Color(0x1C, 0x1C, 0x24, 230)
            g2.fill(cardRect)
            g2.color = Color(255, 255, 255, 20)
            g2.draw(cardRect)

            // Music Icon Badge with glowing Violence red rim
            val badgeX = cardX + 10
            val badgeY = cardY + 10
            val badgeRect = RoundRectangle2D.Float(badgeX.toFloat(), badgeY.toFloat(), 34f, 34f, 9f, 9f)
            val badgeGradient = GradientPaint(badgeX.toFloat(), badgeY.toFloat(), Color(0x2E, 0x14, 0x19), badgeX.toFloat(), (badgeY + 34).toFloat(), Color(0x1A, 0x0E, 0x12))
            g2.paint = badgeGradient
            g2.fill(badgeRect)
            g2.color = Color(0xE2, 0x2D, 0x4B, 180)
            g2.stroke = BasicStroke(1.2f)
            g2.draw(badgeRect)

            // Musical Note Icon inside badge
            g2.color = Color.WHITE
            g2.fillOval(badgeX + 11, badgeY + 20, 6, 5)
            g2.stroke = BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
            g2.drawLine(badgeX + 16, badgeY + 22, badgeX + 16, badgeY + 11)
            g2.drawLine(badgeX + 16, badgeY + 11, badgeX + 22, badgeY + 9)

            // Track Info (Title and Artist)
            val textX = badgeX + 42
            val maxTextW = cardW - 56

            // Track Title
            g2.font = Font("Segoe UI", Font.BOLD, 12)
            g2.color = Color.WHITE
            val truncatedTitle = truncateString(currentTitle, g2.fontMetrics, maxTextW)
            g2.drawString(truncatedTitle, textX, badgeY + 16)

            // Track Artist
            g2.font = Font("Segoe UI", Font.PLAIN, 11)
            g2.color = Color(0x9E, 0x9E, 0xA8)
            val truncatedArtist = truncateString(currentArtist, g2.fontMetrics, maxTextW)
            g2.drawString(truncatedArtist, textX, badgeY + 31)

            // --- Transport Controls (Prev, Play/Pause Hero, Next) ---
            val cardCenterX = w / 2

            // Previous Button
            drawTransportButton(g2, cardCenterX - 45, 84, 28, 28, hoveredControl == CONTROL_PREV) {
                color = Color(230, 230, 240)
                stroke = BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
                drawLine(cardCenterX - 52, 79, cardCenterX - 52, 89)
                val p = Path2D.Float().apply {
                    moveTo((cardCenterX - 42).toFloat(), 79f)
                    lineTo((cardCenterX - 50).toFloat(), 84f)
                    lineTo((cardCenterX - 42).toFloat(), 89f)
                    closePath()
                }
                fill(p)
            }

            // Play / Pause Button (Hero Violence Crimson Circle)
            val playCircleHover = hoveredControl == CONTROL_PLAY_PAUSE
            val playGradient = if (playCircleHover) {
                GradientPaint((cardCenterX - 16).toFloat(), 68f, Color(0xFF, 0x55, 0x75), (cardCenterX + 16).toFloat(), 100f, Color(0xF0, 0x35, 0x55))
            } else {
                GradientPaint((cardCenterX - 16).toFloat(), 68f, Color(0xFF, 0x3B, 0x5C), (cardCenterX + 16).toFloat(), 100f, Color(0xE2, 0x2D, 0x4B))
            }
            g2.paint = playGradient
            g2.fillOval(cardCenterX - 16, 68, 32, 32)
            g2.color = Color(255, 255, 255, 80)
            g2.stroke = BasicStroke(1f)
            g2.drawOval(cardCenterX - 16, 68, 32, 32)

            g2.color = Color.WHITE
            if (isPlaying) {
                // Pause Icon (two rounded vertical bars)
                g2.fillRoundRect(cardCenterX - 5, 76, 3, 16, 2, 2)
                g2.fillRoundRect(cardCenterX + 2, 76, 3, 16, 2, 2)
            } else {
                // Play Icon (centered triangle)
                val playTriangle = Path2D.Float().apply {
                    moveTo((cardCenterX - 4).toFloat(), 75f)
                    lineTo((cardCenterX + 7).toFloat(), 84f)
                    lineTo((cardCenterX - 4).toFloat(), 93f)
                    closePath()
                }
                g2.fill(playTriangle)
            }

            // Next Button
            drawTransportButton(g2, cardCenterX + 45, 84, 28, 28, hoveredControl == CONTROL_NEXT) {
                color = Color(230, 230, 240)
                stroke = BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
                drawLine(cardCenterX + 52, 79, cardCenterX + 52, 89)
                val p = Path2D.Float().apply {
                    moveTo((cardCenterX + 42).toFloat(), 79f)
                    lineTo((cardCenterX + 50).toFloat(), 84f)
                    lineTo((cardCenterX + 42).toFloat(), 89f)
                    closePath()
                }
                fill(p)
            }

            // Divider Line
            g2.color = Color(255, 255, 255, 14)
            g2.drawLine(14, 114, w - 14, 114)

            // --- 4. Menu Navigation Rows ---
            drawMenuItem(g2, ITEM_OPEN, 120, "Open Violence", shortcut = null, hoveredItem == ITEM_OPEN) { ix, iy ->
                // Window icon: rectangle with caption line
                stroke = BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
                drawRoundRect(ix, iy, 14, 12, 3, 3)
                drawLine(ix, iy + 4, ix + 14, iy + 4)
            }

            drawMenuItem(g2, ITEM_MINI_PLAYER, 154, "Mini Player", shortcut = "Ctrl+M", hoveredItem == ITEM_MINI_PLAYER) { ix, iy ->
                // PiP icon: outer frame with inner PiP window
                stroke = BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
                drawRoundRect(ix, iy, 14, 12, 3, 3)
                fillRoundRect(ix + 7, iy + 5, 6, 5, 1, 1)
            }

            drawMenuItem(g2, ITEM_MINIMIZE, 188, "Minimize to Tray", shortcut = null, hoveredItem == ITEM_MINIMIZE) { ix, iy ->
                // Minimize dash
                stroke = BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
                drawLine(ix + 2, iy + 7, ix + 12, iy + 7)
            }

            drawMenuItem(g2, ITEM_SETTINGS, 222, "Settings", shortcut = null, hoveredItem == ITEM_SETTINGS) { ix, iy ->
                // Gear icon
                stroke = BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
                drawOval(ix + 2, iy + 2, 10, 10)
                fillOval(ix + 5, iy + 5, 4, 4)
                drawLine(ix + 7, iy, ix + 7, iy + 2)
                drawLine(ix + 7, iy + 12, ix + 7, iy + 14)
                drawLine(ix, iy + 7, ix + 2, iy + 7)
                drawLine(ix + 12, iy + 7, ix + 14, iy + 7)
            }

            // Divider Line before Quit
            g2.color = Color(255, 255, 255, 14)
            g2.drawLine(14, 258, w - 14, 258)

            drawMenuItem(g2, ITEM_QUIT, 264, "Quit Violence", shortcut = null, hoveredItem == ITEM_QUIT, isDestructive = true) { ix, iy ->
                // Cross icon
                stroke = BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
                drawLine(ix + 3, iy + 3, ix + 11, iy + 11)
                drawLine(ix + 11, iy + 3, ix + 3, iy + 11)
            }
        }

        private fun drawTransportButton(
            g2: Graphics2D,
            cx: Int,
            cy: Int,
            w: Int,
            h: Int,
            hovered: Boolean,
            drawGlyph: Graphics2D.() -> Unit,
        ) {
            val rx = cx - w / 2
            val ry = cy - h / 2
            if (hovered) {
                g2.color = Color(255, 255, 255, 22)
                g2.fillRoundRect(rx, ry, w, h, 8, 8)
            }
            drawGlyph(g2)
        }

        private fun drawMenuItem(
            g2: Graphics2D,
            id: Int,
            y: Int,
            label: String,
            shortcut: String?,
            hovered: Boolean,
            isDestructive: Boolean = false,
            drawVectorIcon: Graphics2D.(ix: Int, iy: Int) -> Unit,
        ) {
            val itemX = 10
            val itemW = width - 20
            val itemH = 32

            if (hovered) {
                val highlightColor = if (isDestructive) {
                    Color(0xFF, 0x45, 0x3A, 30)
                } else {
                    Color(255, 255, 255, 20)
                }
                g2.color = highlightColor
                g2.fillRoundRect(itemX, y, itemW, itemH, 8, 8)
            }

            // Left Vector Icon
            val iconColor = when {
                hovered && isDestructive -> Color(0xFF, 0x69, 0x61)
                hovered -> Color(0xFF, 0x3B, 0x5C) // Violence red accent
                else -> Color(0x9E, 0x9E, 0xA8)
            }
            g2.color = iconColor
            drawVectorIcon(g2, itemX + 10, y + 9)

            // Text Label
            g2.font = Font("Segoe UI", Font.PLAIN, 12)
            g2.color = when {
                hovered && isDestructive -> Color(0xFF, 0x85, 0x94)
                hovered -> Color.WHITE
                else -> Color(0xDC, 0xDC, 0xE4)
            }
            g2.drawString(label, itemX + 34, y + 21)

            // Shortcut text (if any)
            if (shortcut != null) {
                g2.font = Font("Segoe UI", Font.PLAIN, 11)
                g2.color = if (hovered) Color(0xFF, 0x3B, 0x5C).brighter() else Color(0x7A, 0x7A, 0x88)
                val sw = g2.fontMetrics.stringWidth(shortcut)
                g2.drawString(shortcut, itemX + itemW - sw - 8, y + 21)
            }
        }

        private fun truncateString(str: String, fm: FontMetrics, maxW: Int): String {
            if (fm.stringWidth(str) <= maxW) return str
            val ellipsis = "…"
            val ellipsisW = fm.stringWidth(ellipsis)
            var len = str.length
            while (len > 0 && fm.stringWidth(str.substring(0, len)) + ellipsisW > maxW) {
                len--
            }
            return str.substring(0, len) + ellipsis
        }

        companion object {
            private const val CONTROL_PREV = 1
            private const val CONTROL_PLAY_PAUSE = 2
            private const val CONTROL_NEXT = 3

            private const val ITEM_OPEN = 10
            private const val ITEM_MINI_PLAYER = 11
            private const val ITEM_MINIMIZE = 12
            private const val ITEM_SETTINGS = 13
            private const val ITEM_QUIT = 14
        }
    }
}
