package com.music.violence.wear.ui

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.Text
import com.music.violence.wear.R
import com.music.violence.wear.data.NowPlaying
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Always-on state, set by the activity's AmbientLifecycleObserver.
 *
 * [tick] moves on each ambient update — about once a minute — and is the only
 * thing that redraws the ambient player besides the phone changing track.
 */
@Immutable
data class Ambient(
    val active: Boolean = false,
    val burnInProtection: Boolean = false,
    val lowBit: Boolean = false,
    val tick: Int = 0,
)

val LocalAmbient = compositionLocalOf { Ambient() }

/**
 * The player as the always-on display shows it: black, outline-led, with
 * almost every pixel dark (about nine in a hundred are lit).
 *
 * From the top: our own big time (the system clock is hidden in ambient mode,
 * so there is one time, not two); the scalloped play-button outline in the
 * song's accent with the **V** logo inside; the title (two lines at most); the
 * artist; and play state with elapsed and total time. A hairline ring runs
 * all the way round the bezel, with a wavy accent arc on it for how far into
 * the song you are — flat when paused, like the live player's ring.
 *
 * No cover, no blur, no fills, nothing that can be tapped. The whole picture
 * drifts a few pixels each minute where the panel asks for burn-in protection,
 * and on a low-bit panel it is pure white with a dotted track and no greys.
 * Redrawn only on a track change or the system's own once-a-minute tick.
 */
@Composable
fun AmbientPlayer(np: NowPlaying, ambient: Ambient) {
    val lowBit = ambient.lowBit
    val palette = remember(np.colors) { paletteFor(np.colors) }
    val line = if (lowBit) Color.White else palette.aod
    val track = if (lowBit) Color.White else palette.aodDim
    val text = if (lowBit) Color.White else palette.text
    val textDim = if (lowBit) Color.White else palette.textDim
    // A small orbit rather than a random jump: never more than 6dp from centre.
    val shift = remember(ambient.tick, ambient.burnInProtection) {
        if (!ambient.burnInProtection) {
            Offset.Zero
        } else {
            val step = ambient.tick % 8
            val dx = intArrayOf(0, 4, 6, 4, 0, -4, -6, -4)[step]
            val dy = intArrayOf(-6, -4, 0, 4, 6, 4, 0, -4)[step]
            Offset(dx.toFloat(), dy.toFloat())
        }
    }
    // Read once per ambient tick: everything here moves a minute at a time.
    val now = remember(ambient.tick, np) { System.currentTimeMillis() }
    val position = remember(now, np) { np.positionAt(now) }
    val fraction = remember(now, np) { np.progressAt(now) }
    val context = LocalContext.current
    val clock = remember(now) { clockText(context, now) }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        // One grid unit: the screen is 384 of them across, as in the live player.
        val u = maxWidth / 384f

        Box(
            Modifier
                .fillMaxSize()
                .offset(shift.x.dp, shift.y.dp),
        ) {
            // The ring, its arc, and the play-button outline: lines only.
            Canvas(Modifier.fillMaxSize()) {
                val k = size.minDimension / 384f
                val hair = (if (lowBit) 1f else 1.5f) * k
                val bold = (if (lowBit) 2f else 3f) * k
                val cx = size.width / 2f
                val cy = size.height / 2f
                val ringRadius = 178f * k
                drawCircle(
                    color = track,
                    radius = ringRadius,
                    center = Offset(cx, cy),
                    style = Stroke(
                        width = hair,
                        pathEffect = if (lowBit) PathEffect.dashPathEffect(floatArrayOf(2f * k, 4f * k)) else null,
                    ),
                )
                if (fraction * 360f > 0.5f) {
                    drawPath(
                        path = wavyArcPath(
                            cx = cx,
                            cy = cy,
                            radius = ringRadius,
                            amplitude = if (np.playing && !lowBit) 3f * k else 0f,
                            sweepDeg = 360f * fraction,
                            startDeg = -90f,
                            waves = AOD_WAVES,
                            phase = 0.6f,
                        ),
                        color = line,
                        style = Stroke(bold, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                }
                drawPath(
                    path = cookiePath(
                        cx = cx,
                        cy = 166f * k,
                        radius = 47f * k,
                        lobes = 10,
                        amplitude = if (np.playing && !lowBit) 0.07f else 0f,
                    ),
                    color = line,
                    style = Stroke(width = hair + 0.5f * k, join = StrokeJoin.Round),
                )
            }

            // The V, in the middle of the outline.
            Icon(
                painter = painterResource(R.drawable.ic_logo),
                contentDescription = null,
                tint = line,
                modifier = Modifier
                    .offset(x = u * (192f - 29.5f), y = u * (167f - 19.5f))
                    .size(width = u * 59, height = u * 39),
            )

            Text(
                text = clock,
                style = WearType.display.copy(
                    fontSize = (u.value * 58f).sp,
                    fontWeight = FontWeight.W400,
                    letterSpacing = (-1.5f).sp,
                    fontFeatureSettings = "tnum",
                ),
                color = text,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = u * 42),
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = u * 224)
                    .widthIn(max = u * 290),
            ) {
                Text(
                    text = np.title,
                    style = WearType.display.copy(
                        fontSize = (u.value * 25f).sp,
                        lineHeight = (u.value * 27f).sp,
                        letterSpacing = (-0.4f).sp,
                    ),
                    color = text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = np.artist,
                    style = WearType.subtitle.copy(fontSize = (u.value * 17f).sp),
                    color = textDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = u * 250),
                )
                Spacer(Modifier.height(u * 6))
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val tnum = WearType.caption.copy(fontSize = (u.value * 14.5f).sp, fontFeatureSettings = "tnum")
                    // Play or pause, small: the live state at a glance.
                    Icon(
                        painter = painterResource(if (np.playing) R.drawable.ic_play else R.drawable.ic_pause),
                        contentDescription = null,
                        tint = textDim,
                        modifier = Modifier.size(u * 12),
                    )
                    Spacer(Modifier.width(u * 7))
                    Text(text = elapsed(position), style = tnum, color = text, maxLines = 1)
                    if (np.durationMs > 0) {
                        Spacer(Modifier.width(u * 4))
                        Text(text = "/ " + elapsed(np.durationMs), style = tnum, color = textDim, maxLines = 1)
                    }
                }
            }
        }
    }
}

/** Our own clock for the always-on screen, in the wearer's 12 or 24 hour choice, without AM/PM. */
private fun clockText(context: Context, ms: Long): String {
    val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(ms))
}

private fun elapsed(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}

/** 36 ripples round the full circle, in radians per degree. */
private val AOD_WAVES = (36 * 2 * Math.PI / 360).toFloat()
