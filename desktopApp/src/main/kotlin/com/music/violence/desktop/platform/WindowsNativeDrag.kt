package com.music.violence.desktop.platform

import com.music.violence.desktop.DesktopPlatform
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.win32.StdCallLibrary
import com.sun.jna.win32.W32APIOptions
import java.awt.Window

/**
 * Initiates native Windows modal window dragging for undecorated Compose/AWT windows.
 *
 * Directs the OS DWM kernel to manage the move loop (WM_SYSCOMMAND / SC_DRAGMOVE).
 * This enables 100% native window snapping (Windhawk, Windows 11 Snap Layouts, Aero Snap,
 * PowerToys FancyZones) with zero cursor desync, zero runaway acceleration, and no
 * interference from user-space setLocation loops.
 */
internal object WindowsNativeDrag {

    private const val WM_SYSCOMMAND = 0x0112
    private const val SC_DRAGMOVE = 0xF012
    private const val WM_LBUTTONUP = 0x0202
    private const val SM_SWAPBUTTON = 23
    private const val VK_LBUTTON = 0x01
    private const val VK_RBUTTON = 0x02

    private interface User32Drag : StdCallLibrary {
        fun ReleaseCapture(): Boolean
        fun DefWindowProc(hWnd: WinDef.HWND, uMsg: Int, wParam: WinDef.WPARAM, lParam: WinDef.LPARAM): WinDef.LRESULT
        fun GetSystemMetrics(nIndex: Int): Int
        fun GetAsyncKeyState(vKey: Int): Short
        fun GetCursorPos(lpPoint: WinDef.POINT): Boolean
        fun ScreenToClient(hWnd: WinDef.HWND, lpPoint: WinDef.POINT): Boolean
        fun PostMessage(hWnd: WinDef.HWND, uMsg: Int, wParam: WinDef.WPARAM, lParam: WinDef.LPARAM): Boolean

        companion object {
            val INSTANCE: User32Drag by lazy {
                Native.load("user32", User32Drag::class.java, W32APIOptions.DEFAULT_OPTIONS)
            }
        }
    }

    fun startDrag(window: Window): Boolean {
        if (!DesktopPlatform.isWindows || java.awt.GraphicsEnvironment.isHeadless()) return false

        val hwndId = runCatching { Native.getWindowID(window) }.getOrNull() ?: return false
        if (hwndId == 0L) return false
        val hwnd = WinDef.HWND(Pointer.createConstant(hwndId))

        return runCatching {
            val user32 = User32Drag.INSTANCE
            val swapped = user32.GetSystemMetrics(SM_SWAPBUTTON) != 0
            val primaryKey = if (swapped) VK_RBUTTON else VK_LBUTTON

            // Ensure the primary mouse button is currently held down
            if ((user32.GetAsyncKeyState(primaryKey).toInt() and 0x8000) == 0) {
                return false
            }

            // Release AWT's mouse capture so Windows takes over the drag loop
            user32.ReleaseCapture()

            // Initiate the native Windows modal move loop (SC_DRAGMOVE = 0xF012)
            // Windhawk and Windows native snapping hooks intercept this loop seamlessly
            user32.DefWindowProc(
                hwnd,
                WM_SYSCOMMAND,
                WinDef.WPARAM(SC_DRAGMOVE.toLong()),
                WinDef.LPARAM(0),
            )

            // Synthetic button-up at the current cursor position so AWT/Compose clears its press state
            val point = WinDef.POINT()
            if (user32.GetCursorPos(point)) {
                user32.ScreenToClient(hwnd, point)
                val lparam = (point.y.toLong() shl 16) or (point.x.toLong() and 0xFFFF)
                user32.PostMessage(hwnd, WM_LBUTTONUP, WinDef.WPARAM(0), WinDef.LPARAM(lparam))
            }
            true
        }.getOrDefault(false)
    }
}
