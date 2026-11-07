package com.music.violence.data

import kotlinx.serialization.json.jsonArray
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.music.violence.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Request
import java.io.File
import java.net.URI
import java.security.MessageDigest

/**
 * Violence is sideloaded, so there is nothing to push an update notice on its own. This reads one
 * small JSON file from a free static host (`UPDATE_INFO_URL`, see fork-docs/RELEASE_GUIDE.md) at
 * most once a day, compares its `versionCode` with the running build, and, when it is newer, offers
 * the APK: downloaded into the app cache, checked against the file's SHA-256, and handed to the
 * system package installer, so the whole round trip stays inside the app.
 *
 * With no `UPDATE_INFO_URL` in the build (the default) the checker does nothing at all and there is
 * no network call. Nothing points at the original author's repository.
 *
 * The file, `update.json`:
 * ```
 * {
 *   "versionCode": 36,
 *   "versionName": "1.9.0-violence.2",
 *   "changelog": ["Fixed ...", "Added ..."],          // a string or a list of lines
 *   "page": "https://example.org/Violence/",           // optional: where to send people if there is no APK for them
 *   "apks": {
 *     "phone-arm64-v8a":   { "url": "violence-phone-1.9.0-violence.2-arm64-v8a.apk",   "sha256": "<hex>" },
 *     "phone-armeabi-v7a": { "url": "violence-phone-1.9.0-violence.2-armeabi-v7a.apk", "sha256": "<hex>" },
 *     "tv-arm64-v8a":      { ... }
 *   }
 * }
 * ```
 * The app takes the entry for its own kind and the first of the device's CPU types (`phone-<abi>`,
 * then plain `phone` as a fallback for a single universal file); one ABI-specific APK is about half
 * the size of the universal one. APK `url`s may be relative to the JSON file's own address. Updates are compared by `versionCode`
 * only (the fork's `-violence.N` suffix would otherwise read as a pre-release).
 */
object AppUpdateChecker {

    data class UpdateInfo(
        val version: String,
        /** Where to send the user when there is no APK for this app: the file's `page`, else the file itself. */
        val releaseUrl: String,
        val apkUrl: String?,
        /** The changelog, as plain text lines, shown as this update's "what's new". */
        val notes: String?,
        /** Lower-case hex SHA-256 of the APK, when the file gives one; checked after the download. */
        val sha256: String? = null,
    )

    private const val CACHE_SUBDIR = "updates"
    private const val PREFS = "app_update"
    private const val KEY_LAST_CHECK = "last_check_ms"
    private const val KEY_LAST_BODY = "last_body"
    private const val KEY_GH_CHECK = "gh_last_check_ms"
    private const val KEY_GH_BODY = "gh_last_body"

    /** The repository whose GitHub Releases are checked when the build has no `UPDATE_INFO_URL`. */
    internal const val GITHUB_REPO = "yashaswahuh/Violence"

    /** The file is fetched at most this often; in between, the copy kept from the last fetch is used. */
    internal const val CHECK_INTERVAL_MS = 24L * 60 * 60 * 1000

    private val json = Json { ignoreUnknownKeys = true }

    private val _available = MutableStateFlow<UpdateInfo?>(null)
    val available = _available.asStateFlow()

    /** Where this update's APK download currently stands, for the dialog's progress row. */
    sealed interface DownloadState {
        data object Idle : DownloadState
        data class Downloading(val fraction: Float) : DownloadState
        data class Ready(val file: File) : DownloadState
        data class Failed(val message: String) : DownloadState
    }

    private val _download = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val download = _download.asStateFlow()

    /** Set from the UI thread when the user cancels; polled between network reads. */
    @Volatile
    private var downloadCancelled = false

    /**
     * Looks for an update, quietly, at most once per [CHECK_INTERVAL_MS]; in between, the copy kept
     * from the last fetch is used, so an update found yesterday is still offered today (until it is
     * installed) without asking again. A failed fetch keeps whatever copy there is and tries again on
     * the next launch. The caller decides whether to run it at all (Settings > Check for updates
     * automatically).
     *
     * Where it looks: the `update.json` at `UPDATE_INFO_URL` when the build has one, otherwise the
     * GitHub Releases of [GITHUB_REPO].
     */
    suspend fun check(context: Context) = withContext(Dispatchers.IO) {
        runCatching {
            val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val source = source()
            val now = System.currentTimeMillis()
            val last = prefs.getLong(source.checkKey, 0L)
            var body = prefs.getString(source.bodyKey, null)
            if (body == null || now - last >= CHECK_INTERVAL_MS || now < last) {
                val fetched = fetch(source.url, source.github)
                if (fetched != null) {
                    body = fetched
                    prefs.edit().putLong(source.checkKey, now).putString(source.bodyKey, fetched).apply()
                }
            }
            val found = body?.let { resolve(it, source) } ?: return@runCatching
            _available.value = found
        }
    }

    /** What a check the user asked for found. */
    sealed interface CheckResult {
        /** There is nowhere to look. */
        data object NotConfigured : CheckResult
        data object UpToDate : CheckResult
        data class Available(val info: UpdateInfo) : CheckResult
        /** The file could not be fetched or read. */
        data object Failed : CheckResult
    }

    private val _showRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Emits when a check the user asked for found an update, so the app opens its update sheet. */
    val showRequests: SharedFlow<Unit> = _showRequests.asSharedFlow()

    /**
     * A check the user asked for: asks now, whatever the once-a-day limit of [check] says, and
     * reports what it found. An update it finds is published the same way [check]'s is, and the
     * app is asked to open its update sheet.
     */
    suspend fun checkNow(context: Context): CheckResult = withContext(Dispatchers.IO) {
        val source = source()
        if (source.url.isBlank()) return@withContext CheckResult.NotConfigured
        runCatching {
            val body = fetch(source.url, source.github) ?: return@runCatching CheckResult.Failed
            context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putLong(source.checkKey, System.currentTimeMillis()).putString(source.bodyKey, body).apply()
            val found = resolve(body, source)
            if (found != null) {
                _available.value = found
                _showRequests.tryEmit(Unit)
                CheckResult.Available(found)
            } else if (parseOk(body, source)) {
                CheckResult.UpToDate
            } else {
                CheckResult.Failed
            }
        }.getOrDefault(CheckResult.Failed)
    }

    /** Where updates are looked for, and the keys that source's last answer is kept under. */
    private class UpdateSource(val url: String, val github: Boolean) {
        val bodyKey get() = if (github) KEY_GH_BODY else KEY_LAST_BODY
        val checkKey get() = if (github) KEY_GH_CHECK else KEY_LAST_CHECK
    }

    private fun source(): UpdateSource {
        val infoUrl = BuildConfig.UPDATE_INFO_URL.trim()
        return if (infoUrl.startsWith("https://", ignoreCase = true)) {
            UpdateSource(infoUrl, github = false)
        } else {
            UpdateSource("https://api.github.com/repos/$GITHUB_REPO/releases?per_page=5", github = true)
        }
    }

    /** The update [body] describes if it is newer than this build, else null. */
    private fun resolve(body: String, source: UpdateSource): UpdateInfo? {
        val abis = Build.SUPPORTED_ABIS.toList()
        return if (source.github) {
            val release = parseGithubReleases(body, BuildConfig.UPDATE_APK_KEY, abis) ?: return null
            if (!isNewerVersion(release.versionName, BuildConfig.VERSION_NAME)) return null
            UpdateInfo(release.versionName, release.page, release.apkUrl, release.notes, release.sha256)
        } else {
            val parsed = parseUpdate(body, source.url, BuildConfig.UPDATE_APK_KEY, abis) ?: return null
            if (parsed.versionCode <= BuildConfig.VERSION_CODE) return null
            UpdateInfo(parsed.versionName, parsed.page, parsed.apkUrl, parsed.notes, parsed.sha256)
        }
    }

    /** Whether [body] was readable at all, so "up to date" is not said about an answer that was garbage. */
    private fun parseOk(body: String, source: UpdateSource): Boolean = if (source.github) {
        parseGithubReleases(body, BuildConfig.UPDATE_APK_KEY, Build.SUPPORTED_ABIS.toList()) != null
    } else {
        parseUpdate(body, source.url, BuildConfig.UPDATE_APK_KEY, Build.SUPPORTED_ABIS.toList()) != null
    }

    /** What [parseGithubReleases] reads out of GitHub's answer. */
    internal data class ParsedRelease(
        val versionName: String,
        val notes: String?,
        val page: String,
        val apkUrl: String?,
        val sha256: String?,
    )

    /**
     * The newest published release in GitHub's `releases` list (pre-releases included, drafts not).
     * The tag is the version (`v1.9.0-violence.3` reads as `1.9.0-violence.3`), the release text is
     * the changelog, and the APK is the asset for this app: named with [apkKey] and, best, one of
     * [abis], else any `.apk`. GitHub's own `digest` is the checksum when it gives one.
     */
    internal fun parseGithubReleases(body: String, apkKey: String, abis: List<String> = emptyList()): ParsedRelease? =
        runCatching {
            val release = json.parseToJsonElement(body).jsonArray
                .mapNotNull { it as? JsonObject }
                .firstOrNull { (it["draft"] as? JsonPrimitive)?.contentOrNull != "true" }
                ?: return@runCatching null
            val tag = release["tag_name"]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }
                ?: return@runCatching null
            val version = tag.removePrefix("v").removePrefix("V")
            val notes = release["body"]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }?.take(4000)
            val page = release["html_url"]?.jsonPrimitive?.contentOrNull?.takeIf { it.startsWith("https://") }
                ?: "https://github.com/$GITHUB_REPO/releases"
            val apks = (release["assets"] as? JsonArray).orEmpty()
                .mapNotNull { it as? JsonObject }
                .filter { it["name"]?.jsonPrimitive?.contentOrNull?.endsWith(".apk", ignoreCase = true) == true }
            fun named(asset: JsonObject) = asset["name"]!!.jsonPrimitive.content.lowercase()
            val forApp = apks.filter {
                val n = named(it)
                if (apkKey.equals("tv", ignoreCase = true)) {
                    "tv" in n
                } else {
                    ("phone" in n || "android" in n || "app" in n) && "tv" !in n
                }
            }.ifEmpty { apks }
            val apk = abis.firstNotNullOfOrNull { abi -> forApp.firstOrNull { abi.lowercase() in named(it) } }
                ?: forApp.firstOrNull { "universal" in named(it) }
                ?: forApp.firstOrNull()
            val url = apk?.get("browser_download_url")?.jsonPrimitive?.contentOrNull?.takeIf { it.startsWith("https://") }
            val sha = apk?.get("digest")?.jsonPrimitive?.contentOrNull?.removePrefix("sha256:")?.trim()?.lowercase()
                ?.takeIf { Regex("[0-9a-f]{64}").matches(it) }
            ParsedRelease(version, notes, page, url, sha)
        }.getOrNull()

    /**
     * Whether [candidate] is a later version than [current]. Compares the numbers in each in order
     * (`1.9.0-violence.3` is 1, 9, 0, 3), so the fork's `-violence.N` counts up like any other part.
     */
    internal fun isNewerVersion(candidate: String, current: String): Boolean {
        fun parts(v: String) = Regex("""\d+""").findAll(v).map { it.value.toLongOrNull() ?: 0L }.toList()
        val a = parts(candidate)
        val b = parts(current)
        if (a.isEmpty()) return false
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0L }
            val y = b.getOrElse(i) { 0L }
            if (x != y) return x > y
        }
        return false
    }

    private fun fetch(url: String, github: Boolean = false): String? {
        val request = Request.Builder().url(url).header("Cache-Control", "no-cache").apply {
            if (github) {
                header("Accept", "application/vnd.github+json")
                header("User-Agent", "Violence-updater")
            }
        }.build()
        return Http.client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) null else response.body?.string()
        }
    }

    /** What [parseUpdate] reads out of the file. */
    internal data class ParsedUpdate(
        val versionCode: Int,
        val versionName: String,
        val notes: String?,
        val page: String,
        val apkUrl: String?,
        val sha256: String?,
    )

    /**
     * Reads the update file, or null if it is not one. [apkKey] is `"phone"` or `"tv"`: the entry of
     * `apks` that belongs to this app, `<apkKey>-<abi>` for the first of [abis] that has one, else plain `<apkKey>`. Relative APK addresses are resolved against [infoUrl]; anything
     * that does not end up as an https address is dropped (an update is never fetched over plain http).
     */
    internal fun parseUpdate(body: String, infoUrl: String, apkKey: String, abis: List<String> = emptyList()): ParsedUpdate? = runCatching {
        val root = json.parseToJsonElement(body).jsonObject
        val code = root["versionCode"]?.jsonPrimitive?.intOrNull ?: return@runCatching null
        val name = root["versionName"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } ?: return@runCatching null
        val notes = when (val c = root["changelog"]) {
            is JsonArray -> c.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf(String::isNotEmpty) }
                .joinToString("\n")
            is JsonPrimitive -> c.contentOrNull?.trim()
            else -> null
        }?.takeIf { it.isNotEmpty() }
        val page = root["page"]?.jsonPrimitive?.contentOrNull?.let { resolveHttps(infoUrl, it) } ?: infoUrl
        val apks = root["apks"] as? JsonObject
        val candidates = buildList {
            abis.forEach {
                add("$apkKey-$it")
                if (apkKey == "phone") add("android-$it")
                if (apkKey == "android") add("phone-$it")
            }
            add(apkKey)
            if (apkKey == "phone") add("android")
            if (apkKey == "android") add("phone")
        }
        val apk = candidates.firstNotNullOfOrNull { apks?.get(it) as? JsonObject }
        val apkUrl = apk?.get("url")?.jsonPrimitive?.contentOrNull?.let { resolveHttps(infoUrl, it) }
        val sha = apk?.get("sha256")?.jsonPrimitive?.contentOrNull?.trim()?.lowercase()
            ?.takeIf { Regex("[0-9a-f]{64}").matches(it) }
        ParsedUpdate(code, name, notes, page, apkUrl, sha)
    }.getOrNull()

    private fun resolveHttps(base: String, ref: String): String? = runCatching {
        URI(base).resolve(ref.trim()).takeIf { it.scheme.equals("https", ignoreCase = true) }?.toString()
    }.getOrNull()

    /**
     * Wipes any APK left over from a previous run. Called once at cold start
     * so a downloaded update is only ever "Install Now" for the session that
     * downloaded it — the next launch starts clean rather than trying to work
     * out whether a leftover file is still good.
     */
    suspend fun clearCache(context: Context) = withContext(Dispatchers.IO) {
        File(context.cacheDir, CACHE_SUBDIR).listFiles()?.forEach { it.delete() }
    }

    /**
     * Streams the current update's APK into the app cache, reporting progress
     * through [download], and checks it against the file's SHA-256 when there is one (a mismatch
     * deletes it). A finished file survives a cancelled dialog: until the state is reset,
     * "Install Now" comes straight back without a second download.
     */
    suspend fun downloadApk(context: Context): Unit = withContext(Dispatchers.IO) {
        val info = _available.value ?: return@withContext
        val url = info.apkUrl ?: return@withContext
        downloadCancelled = false
        _download.value = DownloadState.Downloading(0f)

        runCatching {
            val dir = File(context.cacheDir, CACHE_SUBDIR).apply { mkdirs() }
            // Drop anything left over from an earlier attempt.
            dir.listFiles()?.forEach { it.delete() }
            val target = File(dir, "violence-${info.version}.apk")
            val digest = MessageDigest.getInstance("SHA-256")

            val request = Request.Builder().url(url).build()
            Http.client.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "Download failed: HTTP ${response.code}" }
                val body = response.body ?: error("Empty download body")
                val total = body.contentLength().takeIf { it > 0 }

                body.byteStream().use { input ->
                    target.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var readTotal = 0L
                        while (true) {
                            if (downloadCancelled) {
                                _download.value = DownloadState.Idle
                                return@withContext
                            }
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            digest.update(buffer, 0, read)
                            readTotal += read
                            total?.let {
                                _download.value =
                                    DownloadState.Downloading((readTotal.toFloat() / it).coerceIn(0f, 1f))
                            }
                        }
                    }
                }
            }
            val expected = info.sha256
            if (expected != null) {
                val actual = digest.digest().joinToString("") { "%02x".format(it) }
                if (!actual.equals(expected, ignoreCase = true)) {
                    target.delete()
                    error("The download did not match its checksum, so it was discarded. Try again.")
                }
            }
            _download.value = DownloadState.Ready(target)
        }.onFailure { error ->
            _download.value = if (downloadCancelled) {
                DownloadState.Idle
            } else {
                DownloadState.Failed(error.message ?: "Download failed")
            }
        }
    }

    /** Stops an in-flight download; the next read loop sees this and bails. */
    fun cancelDownload() {
        downloadCancelled = true
    }

    /** Back to square one after a failure, so the dialog offers Download again. */
    fun resetDownload() {
        _download.value = DownloadState.Idle
    }

    /**
     * Hands a downloaded APK to the system installer.
     *
     * Sideloaded apps need the user's blessing per app ("install unknown apps");
     * without it the installer intent silently does nothing on most ROMs, so
     * the user is sent to that one switch first and taps Install again after.
     * Some TV builds have no such settings screen; there the installer is
     * launched anyway and shows its own "not allowed" prompt with a way in.
     * The file is shared through the FileProvider (`<applicationId>.provider`, which is
     * com.yashaswahuh.violencemusic.provider for the release app), limited to the `updates/` cache folder.
     */
    fun installApk(context: Context, file: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            val opened = runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                        .setData(Uri.parse("package:${context.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }.isSuccess
            if (opened) return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        context.startActivity(
            Intent(Intent.ACTION_INSTALL_PACKAGE)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                .putExtra(Intent.EXTRA_RETURN_RESULT, true)
                .addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_ACTIVITY_NEW_TASK,
                ),
        )
    }
}
