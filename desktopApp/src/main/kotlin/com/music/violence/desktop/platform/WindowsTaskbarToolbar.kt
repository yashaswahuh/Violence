package com.music.violence.desktop.platform

import com.music.violence.desktop.DesktopPlatform
import com.music.violence.desktop.DesktopTrackLog
import com.sun.jna.Callback
import com.sun.jna.Function
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.platform.win32.Guid
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.Ole32
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinGDI
import com.sun.jna.platform.win32.WinNT
import com.sun.jna.platform.win32.WTypes
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.PointerByReference
import com.sun.jna.win32.StdCallLibrary
import com.sun.jna.win32.W32APIOptions
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.Window
import java.awt.geom.Path2D
import java.awt.image.BufferedImage
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Windows Taskbar Thumbnail Toolbar controller.
 *
 * Exposes live playback controls (Previous, Play/Pause, Next) directly on the
 * Windows Taskbar preview window using the Win32 `ITaskbarList3` COM interface.
 *
 * Safe and completely non-blocking: on non-Windows OSes or when COM/Shell32
 * fails, operations gracefully no-op without impacting the application.
 */
internal object WindowsTaskbarToolbar {

    interface Controller {
        fun onPlay()
        fun onPause()
        fun onNext()
        fun onPrevious()
    }

    private const val ID_PREV = 1
    private const val ID_PLAY_PAUSE = 2
    private const val ID_NEXT = 3

    private const val THBN_CLICKED = 0x1800
    private const val WM_COMMAND = 0x0111
    private const val GWLP_WNDPROC = -4

    // THUMBBUTTON flags and masks
    private const val THB_BITMAP = 0x0001
    private const val THB_ICON = 0x0002
    private const val THB_TOOLTIP = 0x0004
    private const val THB_FLAGS = 0x0008

    private const val THBF_ENABLED = 0x0000
    private const val THBF_DISABLED = 0x0001
    private const val THBF_DISMISSONCLICK = 0x0002
    private const val THBF_NOBACKGROUND = 0x0004

    @Structure.FieldOrder("dwMask", "iId", "iBitmap", "hIcon", "szTip", "dwFlags")
    open class THUMBBUTTON : Structure {
        @JvmField var dwMask: Int = 0
        @JvmField var iId: Int = 0
        @JvmField var iBitmap: Int = 0
        @JvmField var hIcon: WinDef.HICON? = null
        @JvmField var szTip: CharArray = CharArray(260)
        @JvmField var dwFlags: Int = 0

        constructor() : super()
        constructor(p: Pointer) : super(p)
    }

    private interface User32Ext : StdCallLibrary {
        fun CreateIconIndirect(iconInfo: WinGDI.ICONINFO): WinDef.HICON?
        fun DestroyIcon(hIcon: WinDef.HICON): Boolean
        fun GetDC(hWnd: WinDef.HWND?): WinDef.HDC?
        fun ReleaseDC(hWnd: WinDef.HWND?, hDC: WinDef.HDC): Int
        fun CallWindowProc(lpPrevWndFunc: Pointer, hWnd: WinDef.HWND, uMsg: Int, wParam: WinDef.WPARAM, lParam: WinDef.LPARAM): WinDef.LRESULT
        fun SetWindowLongPtr(hWnd: WinDef.HWND, nIndex: Int, newProc: Callback): Pointer?
        fun SetWindowLongPtr(hWnd: WinDef.HWND, nIndex: Int, newProc: Pointer): Pointer?
        fun GetWindowLongPtr(hWnd: WinDef.HWND, nIndex: Int): Pointer?

        companion object {
            val INSTANCE: User32Ext by lazy {
                Native.load("user32", User32Ext::class.java, W32APIOptions.DEFAULT_OPTIONS)
            }
        }
    }

    private interface Gdi32Ext : StdCallLibrary {
        fun CreateDIBSection(
            hdc: WinDef.HDC?,
            pbmi: WinGDI.BITMAPINFO,
            usage: Int,
            ppvBits: PointerByReference,
            hSection: WinNT.HANDLE?,
            offset: Int,
        ): WinDef.HBITMAP?

        fun CreateBitmap(nWidth: Int, nHeight: Int, nPlanes: Int, nBitCount: Int, lpBits: ByteArray): WinDef.HBITMAP?
        fun DeleteObject(hObj: WinNT.HANDLE): Boolean

        companion object {
            val INSTANCE: Gdi32Ext by lazy {
                Native.load("gdi32", Gdi32Ext::class.java, W32APIOptions.DEFAULT_OPTIONS)
            }
        }
    }

    private val started = AtomicBoolean(false)
    private var taskbarPtr: Pointer? = null
    private var windowHwnd: WinDef.HWND? = null
    private var originalWndProc: Pointer? = null
    private var customWndProcHolder: Callback? = null
    private var controller: Controller? = null

    @Volatile
    private var isPlayingState = false

    private var iconPrev: WinDef.HICON? = null
    private var iconPlay: WinDef.HICON? = null
    private var iconPause: WinDef.HICON? = null
    private var iconNext: WinDef.HICON? = null

    fun start(window: Window, controller: Controller) {
        if (!DesktopPlatform.isWindows) return
        this.controller = controller

        if (!started.compareAndSet(false, true)) {
            update(isPlayingState, true)
            return
        }

        runCatching {
            initIcons()
            initComAndTaskbar(window)
        }.onFailure {
            DesktopTrackLog.log("WindowsTaskbarToolbar: failed to initialize: ${it.message}")
            stop()
        }
    }

    fun update(isPlaying: Boolean, hasTrack: Boolean) {
        isPlayingState = isPlaying
        val ptr = taskbarPtr ?: return
        val hwnd = windowHwnd ?: return
        if (!started.get()) return

        runCatching {
            val buttons = THUMBBUTTON().toArray(3) as Array<THUMBBUTTON>

            // Button 1: Previous
            buttons[0].dwMask = THB_ICON or THB_TOOLTIP or THB_FLAGS
            buttons[0].iId = ID_PREV
            buttons[0].hIcon = iconPrev
            setTip(buttons[0], "Previous Track")
            buttons[0].dwFlags = if (hasTrack) THBF_ENABLED else THBF_DISABLED

            // Button 2: Play / Pause
            buttons[1].dwMask = THB_ICON or THB_TOOLTIP or THB_FLAGS
            buttons[1].iId = ID_PLAY_PAUSE
            buttons[1].hIcon = if (isPlaying) iconPause else iconPlay
            setTip(buttons[1], if (isPlaying) "Pause" else "Play")
            buttons[1].dwFlags = if (hasTrack) THBF_ENABLED else THBF_DISABLED

            // Button 3: Next
            buttons[2].dwMask = THB_ICON or THB_TOOLTIP or THB_FLAGS
            buttons[2].iId = ID_NEXT
            buttons[2].hIcon = iconNext
            setTip(buttons[2], "Next Track")
            buttons[2].dwFlags = if (hasTrack) THBF_ENABLED else THBF_DISABLED

            val vtable = ptr.getPointer(0L)
            // Slot 16: ThumbBarUpdateButtons(HWND hwnd, UINT cButtons, LPTHUMBBUTTON pButton)
            val updateButtonsFunc = Function.getFunction(vtable.getPointer(16L * Native.POINTER_SIZE))
            val hr = updateButtonsFunc.invoke(
                Int::class.java,
                arrayOf(ptr, hwnd, 3, buttons[0].pointer),
            ) as Int
            if (hr != 0) {
                // If update failed (e.g. buttons not added yet), try adding them
                val addButtonsFunc = Function.getFunction(vtable.getPointer(15L * Native.POINTER_SIZE))
                addButtonsFunc.invoke(Int::class.java, arrayOf(ptr, hwnd, 3, buttons[0].pointer))
            }
        }.onFailure {
            DesktopTrackLog.log("WindowsTaskbarToolbar: update failed: ${it.message}")
        }
    }

    fun stop() {
        if (!started.compareAndSet(true, false)) return
        controller = null

        runCatching {
            val hwnd = windowHwnd
            val orig = originalWndProc
            if (hwnd != null && orig != null) {
                User32Ext.INSTANCE.SetWindowLongPtr(hwnd, GWLP_WNDPROC, orig)
            }
        }
        originalWndProc = null
        customWndProcHolder = null
        windowHwnd = null

        taskbarPtr?.let { ptr ->
            runCatching {
                val vtable = ptr.getPointer(0L)
                // Release is slot 2 in IUnknown
                val releaseFunc = Function.getFunction(vtable.getPointer(2L * Native.POINTER_SIZE))
                releaseFunc.invoke(Int::class.java, arrayOf(ptr))
            }
        }
        taskbarPtr = null

        destroyIcons()
    }

    private fun setTip(button: THUMBBUTTON, tip: String) {
        val chars = tip.toCharArray()
        System.arraycopy(chars, 0, button.szTip, 0, minOf(chars.size, 259))
        button.szTip[minOf(chars.size, 259)] = '\u0000'
    }

    private fun initComAndTaskbar(window: Window) {
        Ole32.INSTANCE.CoInitializeEx(null, Ole32.COINIT_APARTMENTTHREADED)

        val clsid = Guid.CLSID.fromString("{56FDF344-FD6D-11D0-958A-006097C9A090}")
        val iid = Guid.IID.fromString("{EA1AFB91-9E28-4B86-90E9-9E9F8A5EEFAF}")
        val pTaskbar = PointerByReference()

        val hr = Ole32.INSTANCE.CoCreateInstance(
            clsid,
            null,
            WTypes.CLSCTX_INPROC_SERVER,
            iid,
            pTaskbar,
        )
        if (hr.toInt() != 0 || pTaskbar.value == null) {
            DesktopTrackLog.log("WindowsTaskbarToolbar: CoCreateInstance ITaskbarList3 failed (hr=${hr.toInt()})")
            return
        }

        val ptr = pTaskbar.value
        taskbarPtr = ptr

        val vtable = ptr.getPointer(0L)
        // HrInit is slot 3 in ITaskbarList
        val hrInitFunc = Function.getFunction(vtable.getPointer(3L * Native.POINTER_SIZE))
        hrInitFunc.invoke(Int::class.java, arrayOf(ptr))

        val hwnd = resolveHwnd(window) ?: run {
            DesktopTrackLog.log("WindowsTaskbarToolbar: could not find window HWND")
            return
        }
        windowHwnd = hwnd

        installSubclass(hwnd)

        // Initial toolbar registration
        val buttons = THUMBBUTTON().toArray(3) as Array<THUMBBUTTON>
        buttons[0].dwMask = THB_ICON or THB_TOOLTIP or THB_FLAGS
        buttons[0].iId = ID_PREV
        buttons[0].hIcon = iconPrev
        setTip(buttons[0], "Previous Track")
        buttons[0].dwFlags = THBF_ENABLED

        buttons[1].dwMask = THB_ICON or THB_TOOLTIP or THB_FLAGS
        buttons[1].iId = ID_PLAY_PAUSE
        buttons[1].hIcon = if (isPlayingState) iconPause else iconPlay
        setTip(buttons[1], if (isPlayingState) "Pause" else "Play")
        buttons[1].dwFlags = THBF_ENABLED

        buttons[2].dwMask = THB_ICON or THB_TOOLTIP or THB_FLAGS
        buttons[2].iId = ID_NEXT
        buttons[2].hIcon = iconNext
        setTip(buttons[2], "Next Track")
        buttons[2].dwFlags = THBF_ENABLED

        // Slot 15: ThumbBarAddButtons(HWND hwnd, UINT cButtons, LPTHUMBBUTTON pButton)
        val addButtonsFunc = Function.getFunction(vtable.getPointer(15L * Native.POINTER_SIZE))
        val addHr = addButtonsFunc.invoke(
            Int::class.java,
            arrayOf(ptr, hwnd, 3, buttons[0].pointer),
        ) as Int

        if (addHr != 0) {
            // Already added or needs update
            val updateButtonsFunc = Function.getFunction(vtable.getPointer(16L * Native.POINTER_SIZE))
            updateButtonsFunc.invoke(Int::class.java, arrayOf(ptr, hwnd, 3, buttons[0].pointer))
        }
        DesktopTrackLog.log("WindowsTaskbarToolbar: successfully initialized and attached thumbnail buttons")
    }

    private fun resolveHwnd(window: Window): WinDef.HWND? {
        val directId = runCatching { Native.getWindowID(window) }.getOrNull()
        if (directId != null && directId != 0L) {
            return WinDef.HWND(Pointer.createConstant(directId))
        }

        // Fallback: search for window matching PID & Title
        val currentPid = Kernel32.INSTANCE.GetCurrentProcessId()
        var found: WinDef.HWND? = null
        User32.INSTANCE.EnumWindows({ hwnd, _ ->
            val pidRef = IntByReference()
            User32.INSTANCE.GetWindowThreadProcessId(hwnd, pidRef)
            if (pidRef.value == currentPid && User32.INSTANCE.IsWindowVisible(hwnd)) {
                val buffer = CharArray(256)
                User32.INSTANCE.GetWindowText(hwnd, buffer, 256)
                val title = String(buffer).trimEnd('\u0000')
                if (title == "Violence") {
                    found = hwnd
                    return@EnumWindows false
                }
            }
            true
        }, null)
        return found
    }

    private interface SubclassWndProc : Callback {
        fun callback(
            hWnd: WinDef.HWND,
            uMsg: Int,
            wParam: WinDef.WPARAM,
            lParam: WinDef.LPARAM,
        ): WinDef.LRESULT
    }

    private fun installSubclass(hwnd: WinDef.HWND) {
        val orig = User32Ext.INSTANCE.GetWindowLongPtr(hwnd, GWLP_WNDPROC)
        originalWndProc = orig

        val proc = object : SubclassWndProc {
            override fun callback(
                hWnd: WinDef.HWND,
                uMsg: Int,
                wParam: WinDef.WPARAM,
                lParam: WinDef.LPARAM,
            ): WinDef.LRESULT {
                if (uMsg == WM_COMMAND) {
                    val wLong = wParam.toLong()
                    val notifyCode = (wLong shr 16).toInt() and 0xFFFF
                    val buttonId = wLong.toInt() and 0xFFFF
                    if (notifyCode == THBN_CLICKED) {
                        val activeCtrl = controller
                        if (activeCtrl != null) {
                            when (buttonId) {
                                ID_PREV -> activeCtrl.onPrevious()
                                ID_PLAY_PAUSE -> {
                                    if (isPlayingState) activeCtrl.onPause() else activeCtrl.onPlay()
                                }
                                ID_NEXT -> activeCtrl.onNext()
                            }
                        }
                        return WinDef.LRESULT(0)
                    }
                }
                if (orig != null) {
                    return User32Ext.INSTANCE.CallWindowProc(orig, hWnd, uMsg, wParam, lParam)
                }
                return WinDef.LRESULT(0)
            }
        }
        customWndProcHolder = proc
        User32Ext.INSTANCE.SetWindowLongPtr(hwnd, GWLP_WNDPROC, proc)
    }

    private fun initIcons() {
        iconPrev = createHIcon(drawPrevIcon(20, 20))
        iconPlay = createHIcon(drawPlayIcon(20, 20))
        iconPause = createHIcon(drawPauseIcon(20, 20))
        iconNext = createHIcon(drawNextIcon(20, 20))
    }

    private fun destroyIcons() {
        iconPrev?.let { User32Ext.INSTANCE.DestroyIcon(it) }
        iconPlay?.let { User32Ext.INSTANCE.DestroyIcon(it) }
        iconPause?.let { User32Ext.INSTANCE.DestroyIcon(it) }
        iconNext?.let { User32Ext.INSTANCE.DestroyIcon(it) }
        iconPrev = null
        iconPlay = null
        iconPause = null
        iconNext = null
    }

    private fun drawPrevIcon(w: Int, h: Int): BufferedImage {
        val img = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        configureG2D(g)
        g.color = Color.WHITE
        // Left rounded bar
        g.fillRoundRect(3, 4, 3, 12, 2, 2)
        // Left triangle
        val path = Path2D.Float()
        path.moveTo(16f, 4.5f)
        path.lineTo(7f, 10f)
        path.lineTo(16f, 15.5f)
        path.closePath()
        g.fill(path)
        g.dispose()
        return img
    }

    private fun drawNextIcon(w: Int, h: Int): BufferedImage {
        val img = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        configureG2D(g)
        g.color = Color.WHITE
        // Right triangle
        val path = Path2D.Float()
        path.moveTo(4f, 4.5f)
        path.lineTo(13f, 10f)
        path.lineTo(4f, 15.5f)
        path.closePath()
        g.fill(path)
        // Right rounded bar
        g.fillRoundRect(14, 4, 3, 12, 2, 2)
        g.dispose()
        return img
    }

    private fun drawPlayIcon(w: Int, h: Int): BufferedImage {
        val img = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        configureG2D(g)
        g.color = Color.WHITE
        // Centered play triangle
        val path = Path2D.Float()
        path.moveTo(6.5f, 4.5f)
        path.lineTo(15.5f, 10f)
        path.lineTo(6.5f, 15.5f)
        path.closePath()
        g.fill(path)
        g.dispose()
        return img
    }

    private fun drawPauseIcon(w: Int, h: Int): BufferedImage {
        val img = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        configureG2D(g)
        g.color = Color.WHITE
        // Two rounded vertical bars
        g.fillRoundRect(5, 4, 3, 12, 2, 2)
        g.fillRoundRect(12, 4, 3, 12, 2, 2)
        g.dispose()
        return img
    }

    private fun configureG2D(g: Graphics2D) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
    }

    private fun createHIcon(image: BufferedImage): WinDef.HICON? {
        val width = image.width
        val height = image.height
        val hdc = User32Ext.INSTANCE.GetDC(null) ?: return null
        try {
            val bmi = WinGDI.BITMAPINFO()
            bmi.bmiHeader.biSize = bmi.bmiHeader.size()
            bmi.bmiHeader.biWidth = width
            bmi.bmiHeader.biHeight = -height // Top-down
            bmi.bmiHeader.biPlanes = 1
            bmi.bmiHeader.biBitCount = 32
            bmi.bmiHeader.biCompression = WinGDI.BI_RGB

            val ppvBits = PointerByReference()
            val hbmColor = Gdi32Ext.INSTANCE.CreateDIBSection(hdc, bmi, WinGDI.DIB_RGB_COLORS, ppvBits, null, 0)
                ?: return null

            val colorBits = ppvBits.value
            val rgbArray = image.getRGB(0, 0, width, height, null, 0, width)
            colorBits.write(0L, rgbArray, 0, rgbArray.size)

            // 1-bit monochrome mask bitmap (all 0s so 32-bit ARGB alpha is used)
            val maskBytes = ByteArray((width * height + 7) / 8)
            val hbmMask = Gdi32Ext.INSTANCE.CreateBitmap(width, height, 1, 1, maskBytes)
            if (hbmMask == null) {
                Gdi32Ext.INSTANCE.DeleteObject(hbmColor)
                return null
            }

            val iconInfo = WinGDI.ICONINFO().apply {
                fIcon = true
                xHotspot = 0
                yHotspot = 0
                this.hbmColor = hbmColor
                this.hbmMask = hbmMask
            }
            val hIcon = User32Ext.INSTANCE.CreateIconIndirect(iconInfo)

            Gdi32Ext.INSTANCE.DeleteObject(hbmColor)
            Gdi32Ext.INSTANCE.DeleteObject(hbmMask)
            return hIcon
        } finally {
            User32Ext.INSTANCE.ReleaseDC(null, hdc)
        }
    }
}
