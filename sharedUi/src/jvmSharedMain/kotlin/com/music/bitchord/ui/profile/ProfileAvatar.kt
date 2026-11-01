package com.music.bitchord.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.music.bitchord.data.profile.AvatarCatalog

/**
 * A person's picture, in the order of what is known about them:
 *
 *  1. [photo] — a local file the user picked. Only ever passed for the user's *own* avatar on
 *     their own device; it is never part of any profile, card or Jam message.
 *  2. a preset ([avatarId], one of [AvatarCatalog]) on a circle in [color].
 *  3. [imageUrl] — the picture that came with a Google profile or an older Jam client.
 *  4. the first letter of [name].
 *
 * An avatar id this build does not know (a newer app's preset) falls through to 3 and 4, so a
 * member list never breaks because somebody else is on a newer version.
 */
@Composable
fun ProfileAvatar(
    avatarId: String?,
    color: String?,
    name: String,
    size: Dp,
    modifier: Modifier = Modifier,
    photo: Any? = null,
    imageUrl: String? = null,
) {
    val glyph = AvatarCatalog.glyphOf(avatarId)
    val base = remember(color) { parseColor(color) }
    when {
        photo != null -> AsyncImage(
            model = photo,
            contentDescription = null,
            modifier = modifier.size(size).clip(CircleShape),
        )
        glyph != null -> {
            val fontSize = with(LocalDensity.current) { (size * 0.52f).toSp() }
            Box(
                modifier = modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(base.lighten(0.18f), base.darken(0.22f)),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = glyph, fontSize = fontSize, textAlign = TextAlign.Center)
            }
        }
        imageUrl != null -> AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = modifier.size(size).clip(CircleShape),
        )
        else -> Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = name.trim().take(1).uppercase().ifEmpty { "?" },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** A six-digit hex colour (no `#`) as a [Color]; anything unreadable is the app's red. */
fun parseColor(hex: String?): Color {
    val value = hex?.trim()?.removePrefix("#")
    if (value == null || value.length != 6) return DEFAULT_COLOR
    val rgb = value.toLongOrNull(16) ?: return DEFAULT_COLOR
    return Color((0xFF000000L or rgb).toInt())
}

private val DEFAULT_COLOR = Color(0xFFFA2D48)

private fun Color.lighten(by: Float) = Color(
    red + (1f - red) * by,
    green + (1f - green) * by,
    blue + (1f - blue) * by,
)

private fun Color.darken(by: Float) = Color(red * (1f - by), green * (1f - by), blue * (1f - by))
