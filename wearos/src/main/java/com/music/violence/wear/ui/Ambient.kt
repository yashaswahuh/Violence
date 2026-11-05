package com.music.violence.wear.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.Text
import com.music.violence.wear.R
import com.music.violence.wear.data.NowPlaying

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
 * The player as the always-on display shows it: the live player's outline.
 *
 * Built to the always-on rules rather than dimmed from the live screen. The
 * screen is black and only a few percent of its pixels are lit: a hairline
 * ring round the bezel in the song's own accent, a short bright arc on it for
 * how far in the song is, the title big and bold, the artist, the time, and
 * the mark. No cover, no blur, no fills, nothing that can be tapped. The whole
 * picture drifts a few pixels each minute where the panel asks for burn-in
 * protection, and it is pure white, with no greys and a 1 px ring, on a
 * low-bit panel. Redrawn only on a track change or the system's own
 * once-a-minute ambient tick.
 */
@Composable
fun AmbientPlayer(np: NowPlaying, ambient: Ambient) {
    val primary = if (ambient.lowBit) Color.White else Color.White.copy(alpha = 0.94f)
    val secondary = if (ambient.lowBit) Color.White else Color.White.copy(alpha = 0.56f)
    val accent = remember(np.colors) { accentFor(np.colors) }
    val ringColor = if (ambient.lowBit) Color.White else accent.copy(alpha = 0.50f)
    val arcColor = if (ambient.lowBit) Color.White else accent
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
    // Read once per ambient tick: the time moves a minute at a time here.
    val now = remember(ambient.tick, np) { System.currentTimeMillis() }
    val position = remember(now, np) { np.positionAt(now) }
    val fraction = remember(now, np) { np.progressAt(now) }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        val w = maxWidth
        val h = maxHeight

        // The ring and its arc: outlines only, so almost every pixel stays dark.
        Canvas(
            Modifier
                .fillMaxSize()
                .offset(shift.x.dp, shift.y.dp),
        ) {
            val hairline = (if (ambient.lowBit) 1.dp else 1.5.dp).toPx()
            val bold = (if (ambient.lowBit) 2.dp else 3.dp).toPx()
            val inset = 10.dp.toPx()
            val side = minOf(size.width, size.height) - inset * 2f
            val topLeft = Offset((size.width - side) / 2f, (size.height - side) / 2f)
            val arcSize = Size(side, side)
            val start = -90f + AMBIENT_GAP / 2f
            val span = 360f - AMBIENT_GAP
            drawArc(
                color = ringColor,
                startAngle = start,
                sweepAngle = span,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(hairline, cap = StrokeCap.Round),
            )
            if (fraction * span > 0.5f) {
                drawArc(
                    color = arcColor,
                    startAngle = start,
                    sweepAngle = span * fraction,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(bold, cap = StrokeCap.Round),
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(shift.x.dp, shift.y.dp),
        ) {
            Icon(
                painter = painterResource(if (np.playing) R.drawable.ic_play else R.drawable.ic_pause),
                contentDescription = null,
                tint = if (ambient.lowBit) Color.White else accent,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = np.title,
                style = WearType.display,
                color = primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = w * 0.76f),
            )
            Text(
                text = np.artist,
                style = WearType.subtitle,
                color = secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = w * 0.68f),
            )
            Spacer(Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val tnum = WearType.caption.copy(fontFeatureSettings = "tnum")
                Text(text = ambientTime(position), style = tnum, color = primary)
                if (np.durationMs > 0) {
                    Spacer(Modifier.width(4.dp))
                    Text(text = "/ " + ambientTime(np.durationMs), style = tnum, color = secondary)
                }
            }
        }
        // The Home page's mark, at the foot of the always-on screen.
        Icon(
            painter = painterResource(R.drawable.ic_logo),
            contentDescription = null,
            tint = secondary,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(shift.x.dp, shift.y.dp)
                .padding(bottom = h * 0.085f)
                .size(width = h * 0.10f * LOGO_ASPECT, height = h * 0.10f),
        )
    }
}

private fun ambientTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}

/** Degrees left open at the top of the ring, under the clock. */
private const val AMBIENT_GAP = 44f

/** ic_logo's viewport, 730 x 484 — as the phone's Home draws it. */
private const val LOGO_ASPECT = 730f / 484f
