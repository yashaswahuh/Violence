package com.music.bitchord

import com.music.bitchord.data.AppUpdateChecker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateCheckerTest {

    private val infoUrl = "https://example.org/Violence/updates/update.json"
    private val sha = "a".repeat(64)

    private fun file(extra: String = "") = """
        {
          "versionCode": 36,
          "versionName": "1.9.0-violence.2",
          "changelog": ["Fixed one", " ", "Added two"],
          "apks": {
            "phone": { "url": "violence-phone-1.9.0-violence.2.apk", "sha256": "$sha" },
            "tv": { "url": "https://example.org/other/violence-tv.apk" }
          }$extra
        }
    """.trimIndent()

    @Test
    fun readsTheFieldsForThePhone() {
        val u = AppUpdateChecker.parseUpdate(file(), infoUrl, "phone")!!
        assertEquals(36, u.versionCode)
        assertEquals("1.9.0-violence.2", u.versionName)
        assertEquals("Fixed one\nAdded two", u.notes)
        assertEquals("https://example.org/Violence/updates/violence-phone-1.9.0-violence.2.apk", u.apkUrl)
        assertEquals(sha, u.sha256)
        assertEquals(infoUrl, u.page)
    }

    @Test
    fun tvGetsItsOwnApkAndAbsoluteUrlsAreKept() {
        val u = AppUpdateChecker.parseUpdate(file(), infoUrl, "tv")!!
        assertEquals("https://example.org/other/violence-tv.apk", u.apkUrl)
        assertNull(u.sha256)
    }

    @Test
    fun picksTheApkForTheDevicesCpu() {
        val body = """{"versionCode":2,"versionName":"2","apks":{
            "phone":{"url":"universal.apk"},
            "phone-arm64-v8a":{"url":"arm64.apk"},
            "phone-armeabi-v7a":{"url":"arm32.apk"}}}"""
        fun url(vararg abis: String) = AppUpdateChecker.parseUpdate(body, infoUrl, "phone", abis.toList())!!.apkUrl
        assertEquals("https://example.org/Violence/updates/arm64.apk", url("arm64-v8a", "armeabi-v7a"))
        assertEquals("https://example.org/Violence/updates/arm32.apk", url("armeabi-v7a", "armeabi"))
        assertEquals("https://example.org/Violence/updates/universal.apk", url("x86_64"))
    }

    @Test
    fun changelogMayBeAPlainString() {
        val body = """{"versionCode":2,"versionName":"2","changelog":"Hello"}"""
        assertEquals("Hello", AppUpdateChecker.parseUpdate(body, infoUrl, "phone")!!.notes)
    }

    @Test
    fun missingApkEntryMeansNoApkUrl() {
        val body = """{"versionCode":2,"versionName":"2","apks":{"tv":{"url":"a.apk"}}}"""
        assertNull(AppUpdateChecker.parseUpdate(body, infoUrl, "phone")!!.apkUrl)
    }

    @Test
    fun plainHttpApkIsDropped() {
        val body = """{"versionCode":2,"versionName":"2","apks":{"phone":{"url":"http://example.org/a.apk"}}}"""
        assertNull(AppUpdateChecker.parseUpdate(body, infoUrl, "phone")!!.apkUrl)
    }

    @Test
    fun badChecksumIsIgnoredRatherThanTrusted() {
        val body = """{"versionCode":2,"versionName":"2","apks":{"phone":{"url":"a.apk","sha256":"nothex"}}}"""
        assertNull(AppUpdateChecker.parseUpdate(body, infoUrl, "phone")!!.sha256)
    }

    @Test
    fun notAnUpdateFileGivesNull() {
        assertNull(AppUpdateChecker.parseUpdate("<html>404</html>", infoUrl, "phone"))
        assertNull(AppUpdateChecker.parseUpdate("""{"versionName":"2"}""", infoUrl, "phone"))
        assertNull(AppUpdateChecker.parseUpdate("""{"versionCode":"x","versionName":"2"}""", infoUrl, "phone"))
        assertNull(AppUpdateChecker.parseUpdate("""{"versionCode":3}""", infoUrl, "phone"))
    }

    @Test
    fun checkIntervalIsOneDay() {
        assertTrue(AppUpdateChecker.CHECK_INTERVAL_MS == 24L * 60 * 60 * 1000)
    }
}
