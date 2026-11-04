package com.music.violence.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopUpdateCheckerTest {

    private val infoUrl = "https://example.org/Violence/updates/update.json"
    private val sha = "b".repeat(64)

    private fun sampleUpdateJson(extra: String = "") = """
        {
          "versionCode": 36,
          "versionName": "1.9.0-violence.2",
          "changelog": ["Desktop parity extras", " ", "SMTC & tray improvements"],
          "page": "https://example.org/Violence/release.html",
          "desktop": {
            "versionCode": 36,
            "versionName": "1.9.0-violence.2",
            "windows": "Violence-1.9.0-windows-x64-setup.exe",
            "linux": "Violence-1.9.0-linux-x86_64.AppImage",
            "macos": "https://example.org/Violence/downloads/Violence-1.9.0-macos.dmg",
            "sha256": "$sha"
          }$extra
        }
    """.trimIndent()

    @Test
    fun parseUpdateExtractsWindowsInstallerAndResolvesRelativeUrl() {
        val parsed = DesktopUpdateChecker.parseUpdate(sampleUpdateJson(), infoUrl, os = "windows")
        assertNotNull(parsed)
        assertEquals(36, parsed.versionCode)
        assertEquals("1.9.0-violence.2", parsed.versionName)
        assertEquals("Desktop parity extras\nSMTC & tray improvements", parsed.notes)
        assertEquals("https://example.org/Violence/updates/Violence-1.9.0-windows-x64-setup.exe", parsed.downloadUrl)
        assertEquals("https://example.org/Violence/release.html", parsed.page)
        assertEquals(sha, parsed.sha256)
    }

    @Test
    fun parseUpdateExtractsLinuxInstaller() {
        val parsed = DesktopUpdateChecker.parseUpdate(sampleUpdateJson(), infoUrl, os = "linux")
        assertNotNull(parsed)
        assertEquals("https://example.org/Violence/updates/Violence-1.9.0-linux-x86_64.AppImage", parsed.downloadUrl)
    }

    @Test
    fun parseUpdatePreservesAbsoluteUrlForMac() {
        val parsed = DesktopUpdateChecker.parseUpdate(sampleUpdateJson(), infoUrl, os = "macos")
        assertNotNull(parsed)
        assertEquals("https://example.org/Violence/downloads/Violence-1.9.0-macos.dmg", parsed.downloadUrl)
    }

    @Test
    fun parseUpdateHandlesMissingDesktopSectionGracefully() {
        val body = """
            {
              "versionCode": 37,
              "versionName": "1.9.1",
              "changelog": "Bugfixes",
              "page": "https://example.org/releases/1.9.1"
            }
        """.trimIndent()
        val parsed = DesktopUpdateChecker.parseUpdate(body, infoUrl, os = "windows")
        assertNotNull(parsed)
        assertEquals(37, parsed.versionCode)
        assertEquals("1.9.1", parsed.versionName)
        assertEquals("Bugfixes", parsed.notes)
        assertEquals("https://example.org/releases/1.9.1", parsed.page)
        assertNull(parsed.downloadUrl)
    }

    @Test
    fun parseUpdateIgnoresInsecureHttpInstallerUrls() {
        val body = """
            {
              "versionCode": 38,
              "versionName": "1.9.2",
              "desktop": {
                "windows": "http://insecure.example.com/installer.exe"
              }
            }
        """.trimIndent()
        val parsed = DesktopUpdateChecker.parseUpdate(body, infoUrl, os = "windows")
        assertNotNull(parsed)
        assertNull(parsed.downloadUrl)
    }

    @Test
    fun isNewerNumericComparison() {
        assertTrue(DesktopUpdateChecker.isNewer("1.9.0", "1.8.0"))
        assertTrue(DesktopUpdateChecker.isNewer("2.0.0", "1.9.9"))
        assertFalse(DesktopUpdateChecker.isNewer("1.8.0", "1.8.0"))
        assertFalse(DesktopUpdateChecker.isNewer("1.7.0", "1.8.0"))
    }

    @Test
    fun isNewerHandlesPreReleaseBeta() {
        // A release build is newer than a beta of the same version
        assertTrue(DesktopUpdateChecker.isNewer("1.8", "1.8-beta1"))
        assertFalse(DesktopUpdateChecker.isNewer("1.8-beta1", "1.8"))
    }

    @Test
    fun checkIntervalIsTwentyFourHours() {
        assertEquals(24L * 60 * 60 * 1000L, DesktopUpdateChecker.CHECK_INTERVAL_MS)
    }

    @Test
    fun invalidJsonReturnsNull() {
        assertNull(DesktopUpdateChecker.parseUpdate("not json", infoUrl))
        assertNull(DesktopUpdateChecker.parseUpdate("{}", infoUrl))
        assertNull(DesktopUpdateChecker.parseUpdate("""{"someKey": "value"}""", infoUrl))
    }
}

