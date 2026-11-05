package com.music.violence.ui.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.violence.data.listentogether.DiscoveredJam
import com.music.violence.data.listentogether.JamEmoji
import com.music.violence.data.listentogether.JamReaction
import com.music.violence.ui.haptics.Haptic
import com.music.violence.ui.haptics.rememberHaptics
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

private class ParticleItem(
    val id: Long,
    val emoji: String,
    val initialXRatio: Float, // 0.0 to 1.0 across screen
    val driftX: Float,        // Horizontal drift in dp
    val travelY: Float,       // Vertical rise in dp
    val baseScale: Float,
)

/**
 * Floating physics-based particle burst overlay rendered across peers.
 */
@Composable
fun JamReactionBurstOverlay(
    reactions: SharedFlow<JamReaction>,
    modifier: Modifier = Modifier,
) {
    val particles = remember { mutableStateListOf<ParticleItem>() }

    LaunchedEffect(reactions) {
        reactions.collect { reaction ->
            val symbol = reaction.emoji.symbol
            val count = 8
            for (i in 0 until count) {
                val item = ParticleItem(
                    id = Random.nextLong(),
                    emoji = symbol,
                    initialXRatio = 0.2f + Random.nextFloat() * 0.6f,
                    driftX = (Random.nextFloat() - 0.5f) * 120f,
                    travelY = 220f + Random.nextFloat() * 240f,
                    baseScale = 0.85f + Random.nextFloat() * 0.5f,
                )
                particles.add(item)
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        particles.forEach { particle ->
            SingleFloatingParticle(
                particle = particle,
                containerWidthPx = widthPx,
                containerHeightPx = heightPx,
                onFinished = { particles.remove(particle) },
            )
        }
    }
}

@Composable
private fun SingleFloatingParticle(
    particle: ParticleItem,
    containerWidthPx: Float,
    containerHeightPx: Float,
    onFinished: () -> Unit,
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(particle.id) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1800, easing = LinearOutSlowInEasing),
        )
        onFinished()
    }

    val currentProgress = progress.value
    // Ease alpha: visible until 0.6, then fades to 0
    val alpha = if (currentProgress < 0.6f) 1f else (1f - (currentProgress - 0.6f) / 0.4f).coerceIn(0f, 1f)
    val scale = (particle.baseScale * (0.6f + currentProgress * 0.7f)).coerceIn(0f, 2f)

    val startX = containerWidthPx * particle.initialXRatio
    val currentX = startX + (particle.driftX * currentProgress)
    val currentY = (containerHeightPx * 0.85f) - (particle.travelY * currentProgress)

    Text(
        text = particle.emoji,
        fontSize = 28.sp,
        modifier = Modifier
            .offset { IntOffset(currentX.toInt(), currentY.toInt()) }
            .scale(scale)
            .alpha(alpha),
    )
}

/**
 * Row of reaction emoji chips: 🔥, ❤️, 💀, 🎵, 👏
 */
@Composable
fun JamReactionChips(
    onReaction: (JamEmoji) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        JamEmoji.entries.forEach { emoji ->
            ReactionChipItem(
                emoji = emoji,
                onClick = {
                    haptics.play(Haptic.Tap)
                    onReaction(emoji)
                },
            )
        }
    }
}

@Composable
private fun ReactionChipItem(
    emoji: JamEmoji,
    onClick: () -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }
    val scale = remember { Animatable(1f) }

    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = if (pressed) 0.22f else 0.08f))
            .border(1.dp, Color.White.copy(alpha = if (pressed) 0.35f else 0.12f), CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                onClick()
            }
            .scale(scale.value),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = emoji.symbol,
            fontSize = 22.sp,
        )
    }
}

/**
 * Glowing Violence-accent card showing a discovered nearby Jam session.
 */
@Composable
fun NearbyJamCard(
    jam: DiscoveredJam,
    onJoin: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    val accent = MaterialTheme.colorScheme.primary

    val borderBrush = Brush.horizontalGradient(
        colors = listOf(
            accent.copy(alpha = 0.85f),
            Color(0xFFE040FB).copy(alpha = 0.65f),
            accent.copy(alpha = 0.40f),
        ),
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .border(1.5.dp, borderBrush, RoundedCornerShape(18.dp))
            .clickable {
                haptics.play(Haptic.Select)
                onJoin(jam.partyCode)
            }
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.18f))
                    .border(1.dp, accent.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Podcasts,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(24.dp),
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "NEARBY JAM",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                        ),
                        color = accent,
                    )
                }

                Spacer(Modifier.height(2.dp))

                Text(
                    text = "${jam.hostName} is listening",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (jam.currentTrack.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = jam.currentTrack,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.70f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            Button(
                onClick = {
                    haptics.play(Haptic.Select)
                    onJoin(jam.partyCode)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = Color.White,
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    text = "Join",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                )
            }
        }
    }
}
