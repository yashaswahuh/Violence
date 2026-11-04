package com.music.violence.desktop

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URI

/**
 * Checks `update.json` from [resolvedInfoUrl] at most once a day and notifies the user
 * when a newer desktop version or release is available.
 *
 * Sideloaded installations are not auto-replaced; the user is offered a download link
 * or directed to the release page.
 */
internal object DesktopUpdateChecker {

    data class UpdateInfo(
        val version: String,
        val releaseUrl: String,
        /** The installer for this platform, or null when the release has none. */
        val downloadUrl: String?,
        val notes: String?,
    )

    data class ParsedUpdate(
        val versionCode: Int?,
        val versionName: String,
        val notes: String?,
        val page: String,
        val downloadUrl: String?,
        val sha256: String?,
    )

    const val DEFAULT_UPDATE_URL = "https://yashaswahuh.is-a.dev/Violence/updates/update.json"
    internal const val CHECK_INTERVAL_MS = 24L * 60 * 60 * 1000L
    private const val PREFS_KEY_LAST_CHECK = "desktop_update_last_check_ms"
    private const val PREFS_KEY_LAST_BODY = "desktop_update_last_body"

    val currentVersion: String = System.getProperty("violence.version") ?: "1.8-beta1"
    val currentVersionCode: Int = System.getProperty("violence.version.code")?.toIntOrNull() ?: 29

    fun resolvedInfoUrl(): String =
        System.getProperty("violence.update.url")?.takeIf(String::isNotBlank)
            ?: System.getenv("UPDATE_INFO_URL")?.takeIf(String::isNotBlank)
            ?: DEFAULT_UPDATE_URL

    private val json = Json { ignoreUnknownKeys = true }

    private val http = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
        }
    }

    suspend fun check(persistence: DesktopPersistence = DesktopPersistence()): UpdateInfo? =
        withContext(Dispatchers.IO) {
            runCatching {
                val infoUrl = resolvedInfoUrl().trim()
                if (infoUrl.isBlank() || (!infoUrl.startsWith("https://", ignoreCase = true) && !infoUrl.startsWith("file://", ignoreCase = true))) {
                    return@runCatching null
                }
                val now = System.currentTimeMillis()
                val last = persistence.preferences.getLong(PREFS_KEY_LAST_CHECK, 0L)
                var body = persistence.preferences.get(PREFS_KEY_LAST_BODY, null)
                if (body == null || now - last >= CHECK_INTERVAL_MS || now < last) {
                    val fetched = fetch(infoUrl)
                    if (fetched != null) {
                        body = fetched
                        persistence.preferences.putLong(PREFS_KEY_LAST_CHECK, now)
                        persistence.saveString(PREFS_KEY_LAST_BODY, fetched)
                    }
                }
                val currentBody = body ?: return@runCatching null
                val parsed = parseUpdate(currentBody, infoUrl) ?: return@runCatching null

                val isNewerVersion = if (parsed.versionCode != null && parsed.versionCode > 0) {
                    parsed.versionCode > currentVersionCode
                } else {
                    isNewer(parsed.versionName.removePrefix("v"), currentVersion.removePrefix("v"))
                }
                if (!isNewerVersion) return@runCatching null

                UpdateInfo(
                    version = parsed.versionName,
                    releaseUrl = parsed.page,
                    downloadUrl = parsed.downloadUrl,
                    notes = parsed.notes,
                )
            }.getOrNull()
        }

    private suspend fun fetch(url: String): String? = runCatching {
        val response = http.get(url) {
            header("Cache-Control", "no-cache")
            header("Accept", "application/json")
        }
        if (response.status.isSuccess()) response.bodyAsText() else null
    }.getOrNull()

    internal fun parseUpdate(
        body: String,
        infoUrl: String,
        os: String = currentPlatformOs(),
    ): ParsedUpdate? = runCatching {
        val root = json.parseToJsonElement(body).jsonObject
        val desktopObj = root["desktop"] as? JsonObject
        val code = desktopObj?.get("versionCode")?.jsonPrimitive?.intOrNull
            ?: root["versionCode"]?.jsonPrimitive?.intOrNull
        val name = desktopObj?.get("versionName")?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank)
            ?: root["versionName"]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank)
            ?: desktopObj?.get("tag_name")?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank)
            ?: root["tag_name"]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank)
            ?: return@runCatching null

        val notes = when (val c = root["changelog"] ?: desktopObj?.get("changelog")) {
            is JsonArray -> c.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf(String::isNotEmpty) }
                .joinToString("\n")
            is JsonPrimitive -> c.contentOrNull?.trim()
            else -> null
        }?.takeIf(String::isNotEmpty)

        val page = root["page"]?.jsonPrimitive?.contentOrNull?.let { resolveHttps(infoUrl, it) }
            ?: desktopObj?.get("page")?.jsonPrimitive?.contentOrNull?.let { resolveHttps(infoUrl, it) }
            ?: infoUrl

        val downloadUrl = resolveInstallerUrl(desktopObj, root, infoUrl, os)
        val sha = desktopObj?.get("sha256")?.jsonPrimitive?.contentOrNull?.trim()?.lowercase()
            ?.takeIf { Regex("[0-9a-f]{64}").matches(it) }

        ParsedUpdate(code, name, notes, page, downloadUrl, sha)
    }.getOrNull()

    private fun resolveInstallerUrl(
        desktopObj: JsonObject?,
        root: JsonObject,
        infoUrl: String,
        os: String,
    ): String? {
        if (desktopObj != null) {
            val key = when (os.lowercase()) {
                "windows" -> listOf("windows", "windows-x64-setup.exe", "msi", "exe", "url")
                "linux" -> listOf("linux", "AppImage", "deb", "url")
                "macos" -> listOf("macos", "dmg", "pkg", "url")
                else -> listOf("url")
            }
            for (k in key) {
                val candidate = desktopObj[k]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank)
                if (candidate != null) {
                    val resolved = resolveHttps(infoUrl, candidate)
                    if (resolved != null) return resolved
                }
            }
        }
        val fallbackUrl = root["desktopUrl"]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank)
        if (fallbackUrl != null) {
            return resolveHttps(infoUrl, fallbackUrl)
        }
        return null
    }

    private fun currentPlatformOs(): String = when {
        DesktopPlatform.isWindows -> "windows"
        DesktopPlatform.isLinux -> "linux"
        DesktopPlatform.isMac -> "macos"
        else -> "unknown"
    }

    private fun resolveHttps(base: String, ref: String): String? = runCatching {
        URI(base).resolve(ref.trim())
            .takeIf { it.scheme.equals("https", ignoreCase = true) || it.scheme.equals("file", ignoreCase = true) }
            ?.toString()
    }.getOrNull()

    private class Parsed(val parts: List<Int>, val preRelease: Boolean)

    private fun parse(raw: String): Parsed {
        val dash = raw.indexOf('-')
        val base = if (dash >= 0) raw.substring(0, dash) else raw
        return Parsed(base.split('.').map { it.toIntOrNull() ?: 0 }, dash >= 0)
    }

    /** Numeric comparison, with a `-betaN` build counted as older than the plain release it leads up to. */
    internal fun isNewer(latest: String, current: String): Boolean {
        val l = parse(latest)
        val c = parse(current)
        for (i in 0 until maxOf(l.parts.size, c.parts.size)) {
            val a = l.parts.getOrElse(i) { 0 }
            val b = c.parts.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return c.preRelease && !l.preRelease
    }
}
