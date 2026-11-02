package com.music.bitchord.data.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * The phone's and the TV's handle on the local Violence profile and friends list.
 *
 * Everything lives in the app's private storage (`violence_profile` preferences plus one photo
 * file) and nothing here talks to a network. The rules are in the shared
 * [ProfileRepository]; this object only supplies the storage and the photo.
 *
 * Nothing in this file logs: a name, an id or a photo path never reaches a logger.
 */
object ProfileManager {

    private const val PREFS = "violence_profile"
    private const val PHOTO_DIR = "profile"
    private const val PHOTO_FILE = "photo.jpg"
    private const val PHOTO_EDGE_PX = 256

    private lateinit var repository: ProfileRepository
    private var settings: android.content.SharedPreferences? = null
    private const val KEY_AUTO_JOIN = "auto_join_friends"

    private val _autoJoinFriends = MutableStateFlow(false)

    /** Join a Jam straight away when a friend on the list shares it by NFC tap. Off unless switched on. */
    val autoJoinFriends: StateFlow<Boolean> = _autoJoinFriends.asStateFlow()

    fun setAutoJoinFriends(value: Boolean) {
        _autoJoinFriends.value = value
        settings?.edit()?.putBoolean(KEY_AUTO_JOIN, value)?.apply()
    }

    /** Whether [id] is on the friends list (false before [init], as in a unit test). */
    fun isFriend(id: String): Boolean = ::repository.isInitialized && repository.friends.value.any { it.id == id }

    /** The name the user gave [id] (or their own), if they are a friend. */
    fun friendName(id: String): String? =
        if (::repository.isInitialized) repository.friends.value.firstOrNull { it.id == id }?.shownName else null
    private lateinit var photoFile: File

    /** Call once from `Application.onCreate`, before anything reads the profile. */
    fun init(context: Context) {
        val app = context.applicationContext
        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        repository = ProfileRepository(
            storage = object : ProfileStorage {
                override fun read(key: String): String? = prefs.getString(key, null)
                override fun write(key: String, value: String?) {
                    prefs.edit().apply { if (value == null) remove(key) else putString(key, value) }.apply()
                }
            },
            newId = { UUID.randomUUID().toString() },
            nowMs = { System.currentTimeMillis() },
        )
        photoFile = File(File(app.filesDir, PHOTO_DIR), PHOTO_FILE)
        settings = prefs
        _autoJoinFriends.value = prefs.getBoolean(KEY_AUTO_JOIN, false)
    }

    val profile: StateFlow<InAppProfile?> get() = repository.profile
    val friends: StateFlow<List<FriendCard>> get() = repository.friends

    /** The profile right now, or null if there is none (or [init] has not run, as in a unit test). */
    fun current(): InAppProfile? = if (::repository.isInitialized) repository.profile.value else null

    fun save(name: String, avatarId: String, color: String) = repository.save(name, avatarId, color)

    /** Removes the profile and its photo. Friends are kept (clear them separately). */
    fun deleteProfile() {
        repository.deleteProfile()
        clearPhoto()
    }

    fun addFriend(card: ProfileCard) = repository.addFriend(card)
    fun renameFriend(id: String, nickname: String) = repository.renameFriend(id, nickname)
    fun removeFriend(id: String) = repository.removeFriend(id)
    fun clearFriends() = repository.clearFriends()

    fun shouldSuggest(): Boolean = repository.shouldSuggest()
    fun markSuggestionHandled() = repository.markSuggestionHandled()

    // ------------------------------------------------------------- local photo --

    /** The photo file if one was picked, else null. Local only: never uploaded, never in a card. */
    fun photo(): File? = if (::photoFile.isInitialized && photoFile.isFile) photoFile else null

    /**
     * Copies the picked image into private storage, centre-cropped to a small square JPEG.
     * Returns false if it could not be read. The source [uri] is not kept.
     */
    suspend fun setPhoto(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            // Reading only the size makes decodeStream return null by design: that is not a failure,
            // so what counts is that the stream opened and the size came out.
            val opened = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds); true }
            if (opened != true) return@runCatching false
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching false
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= PHOTO_EDGE_PX && bounds.outHeight / (sample * 2) >= PHOTO_EDGE_PX) {
                sample *= 2
            }
            val decoded = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            } ?: return@runCatching false
            val edge = minOf(decoded.width, decoded.height)
            val square = Bitmap.createBitmap(decoded, (decoded.width - edge) / 2, (decoded.height - edge) / 2, edge, edge)
            val scaled = Bitmap.createScaledBitmap(square, PHOTO_EDGE_PX, PHOTO_EDGE_PX, true)
            photoFile.parentFile?.mkdirs()
            photoFile.outputStream().use { scaled.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            true
        }.getOrDefault(false)
    }

    fun clearPhoto() {
        if (::photoFile.isInitialized) photoFile.delete()
    }
}
