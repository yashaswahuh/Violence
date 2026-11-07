package com.music.violence.ui.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.violence.data.profile.ProfileManager
import java.io.File

/**
 * The photo the user picked for their own profile, or null if there is none. Redrawn when it is
 * picked or removed, so every place that shows the user's own picture (the profile button, the
 * profile page, Settings, their own row in a Jam) changes together.
 *
 * The photo is local only: it is shown on this device for the user's own avatar and is never put
 * in a profile card, a friend request or a Jam message.
 */
@Composable
fun rememberMyPhoto(): File? {
    val version by ProfileManager.photoVersion.collectAsStateWithLifecycle()
    return remember(version) { ProfileManager.photo() }
}
