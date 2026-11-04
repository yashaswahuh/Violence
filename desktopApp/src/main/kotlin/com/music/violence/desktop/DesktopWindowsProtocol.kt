package com.music.violence.desktop

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Registers the `violence://` protocol in the Windows registry (HKCU)
 * so clicking violence links in browser/Discord opens Violence.exe.
 */
internal object DesktopWindowsProtocol {

    suspend fun registerProtocolIfNeeded() = withContext(Dispatchers.IO) {
        if (!DesktopPlatform.isWindows) return@withContext
        runCatching {
            val exePath = ProcessHandle.current().info().command().orElse(null) ?: return@withContext
            if (!exePath.endsWith(".exe", ignoreCase = true) || exePath.contains("java.exe", ignoreCase = true)) {
                return@withContext
            }
            val openCmd = "\"$exePath\" \"%1\""
            Runtime.getRuntime().exec(arrayOf("reg", "add", "HKCU\\Software\\Classes\\violence", "/ve", "/d", "URL:Violence Protocol", "/f")).waitFor()
            Runtime.getRuntime().exec(arrayOf("reg", "add", "HKCU\\Software\\Classes\\violence", "/v", "URL Protocol", "/d", "", "/f")).waitFor()
            Runtime.getRuntime().exec(arrayOf("reg", "add", "HKCU\\Software\\Classes\\violence\\shell\\open\\command", "/ve", "/d", openCmd, "/f")).waitFor()
        }
    }
}

