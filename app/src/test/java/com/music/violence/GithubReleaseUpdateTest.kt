package com.music.violence

import com.music.violence.data.AppUpdateChecker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GithubReleaseUpdateTest {

    private val sha = "b".repeat(64)

    private fun releases(draft: Boolean = false) = """
        [
          {
            "tag_name": "v1.9.0-violence.3",
            "draft": $draft,
            "html_url": "https://github.com/yashaswahuh/Violence/releases/tag/v1.9.0-violence.3",
            "body": "  - Fixed one\n  - Added two  ",
            "assets": [
              { "name": "violence-tv-1.9.0-arm64-v8a.apk", "browser_download_url": "https://github.com/x/tv.apk" },
              { "name": "violence-phone-1.9.0-armeabi-v7a.apk", "browser_download_url": "https://github.com/x/phone-v7.apk" },
              { "name": "violence-phone-1.9.0-arm64-v8a.apk", "browser_download_url": "https://github.com/x/phone-v8.apk", "digest": "sha256:$sha" },
              { "name": "notes.txt", "browser_download_url": "https://github.com/x/notes.txt" }
            ]
          },
          { "tag_name": "v1.8.0", "draft": false, "html_url": "https://github.com/x/old", "assets": [] }
        ]
    """.trimIndent()

    @Test
    fun readsTheNewestReleaseAndPicksThePhoneApkForTheDevicesCpu() {
        val r = AppUpdateChecker.parseGithubReleases(releases(), "phone", listOf("arm64-v8a", "armeabi-v7a"))!!
        assertEquals("1.9.0-violence.3", r.versionName)
        assertEquals("https://github.com/x/phone-v8.apk", r.apkUrl)
        assertEquals(sha, r.sha256)
        assertEquals("- Fixed one\n  - Added two", r.notes)
        assertTrue(r.page.startsWith("https://github.com/yashaswahuh/Violence/releases/tag/"))
    }

    @Test
    fun fallsBackToTheOtherCpuWhenTheFirstHasNoFile() {
        val r = AppUpdateChecker.parseGithubReleases(releases(), "phone", listOf("x86_64", "armeabi-v7a"))!!
        assertEquals("https://github.com/x/phone-v7.apk", r.apkUrl)
    }

    @Test
    fun aDraftIsSkippedAndAnEmptyListGivesNull() {
        assertEquals("1.8.0", AppUpdateChecker.parseGithubReleases(releases(draft = true), "phone")!!.versionName)
        assertNull(AppUpdateChecker.parseGithubReleases("[]", "phone"))
        assertNull(AppUpdateChecker.parseGithubReleases("<html>rate limited</html>", "phone"))
    }

    @Test
    fun versionsCompareNumberByNumberIncludingTheForkSuffix() {
        assertTrue(AppUpdateChecker.isNewerVersion("1.9.0-violence.3", "1.9.0-violence.2"))
        assertTrue(AppUpdateChecker.isNewerVersion("1.10.0", "1.9.9"))
        assertTrue(AppUpdateChecker.isNewerVersion("2.0", "1.9.0-violence.7"))
        assertFalse(AppUpdateChecker.isNewerVersion("1.9.0-violence.2", "1.9.0-violence.2"))
        assertFalse(AppUpdateChecker.isNewerVersion("1.9.0", "1.9.1"))
        assertFalse(AppUpdateChecker.isNewerVersion("nightly", "1.0"))
    }
}
