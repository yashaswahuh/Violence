package com.music.violence.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.music.violence.R
import com.music.violence.ui.components.GlassAlertDialog

/** One release's notes in the in-app changelog: its name and what changed, newest first. */
private class ChangelogEntry(val title: String, val lines: List<String>)

private val CHANGELOG = listOf(
    ChangelogEntry(
        "v0.9.5",
        listOf(
            "Made the app ready for Pre-release. Release scheduled for 10 November 2026",
            "New app icons: Blue, Red, Pink, Yellow and Purple, along with Minimal and Monochrome options for simpler options.",
            "Headphone Crossfeed now has three buttons for its mode instead of tapping round a cycle.",
            "AutoEQ headphone calibration moved to Playback settings, since it works without the equaliser.",
            "Android TV remains untested for the time being.",
        ),
    ),
    ChangelogEntry(
        "V0.0.2-0.9.0",
        listOf(
            "Fixed Liquid Glass stutters.",
            "Added AutoEQ, Binaural Crossfeed, Storage Breakdown, local user profiles, NFC Sharing, etc",
            "Tons of performance improvements.",
            "Created Violence Style (enabled by default) along with options for customization.",
        ),
    ),
    ChangelogEntry(
        "V0.0.1",
        listOf(
            "Forked BitChord v1.9.0 and rebranded to Violence.",
            "Jam server uses Violence's own server instead of Bitchord's.",
            "Icons changed and added a local profile system.",
            "Credits to BitChord for the base code.",
        ),
    ),
)

/** A line of credit: who or what, and what it is for. */
private class Credit(val name: String, val role: String)

private class CreditGroup(val title: String, val credits: List<Credit>)

private val CREDITS = listOf(
    CreditGroup(
        "Violence",
        listOf(
            Credit("Yashaswa, Namra Kapadia", "Violence, maintained as a private fork. GPL-3.0."),
            Credit("Kushagra Singh", "Created BitChord, the open-source app Violence is forked from. GPL-3.0."),
        ),
    ),
    CreditGroup(
        "Design",
        listOf(
            Credit("Apple Music", "The interface is inspired by it. Violence is not affiliated with Apple."),
            Credit("Kyant0 (backdrop)", "The Liquid Glass effect used by the bars and menus."),
            Credit("Haze (Chris Banes)", "The frosted-glass fallback."),
        ),
    ),
    CreditGroup(
        "Fonts (SIL Open Font License 1.1)",
        listOf(
            Credit("Inter", "The Inter Project Authors."),
            Credit("Bodoni Moda", "The Bodoni Moda Project Authors."),
            Credit("Anton", "The Anton Project Authors."),
            Credit("Cormorant Garamond", "The Cormorant Project Authors."),
        ),
    ),
    CreditGroup(
        "Lyrics",
        listOf(
            Credit("binimum (am-lyrics)", "The Apple-like lyrics animation."),
            Credit("Lyrics sources", "lrc.red, BiniLyrics, BetterLyrics, PaxSenix, LyricsPlus, SimpMusic, Unison, Megalobiz, KuGou, LRCLIB, Musixmatch and Genius."),
        ),
    ),
    CreditGroup(
        "Audio",
        listOf(
            Credit("Mornye", "The reference for bit-perfect USB exclusive output."),
        ),
    ),
    CreditGroup(
        "Animations",
        listOf(
            Credit("Mark Clarke", "\"Phone tap\" animation, from LottieFiles."),
            Credit("BASURI AUTOMOTIVE", "\"Black successful check\" animation, from LottieFiles."),
            Credit("Lottie for Android", "Plays them. Apache-2.0."),
        ),
    ),
)

/** The in-app changelog: a scrolling list of what changed in each release. */
@Composable
internal fun ChangelogDialog(onDismiss: () -> Unit) {
    GlassAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.changelog)) },
        text = {
            val accent = MaterialTheme.colorScheme.primary
            val lineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
            val pastDot = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            Column(Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                CHANGELOG.forEachIndexed { index, entry ->
                    val current = index == 0
                    val last = index == CHANGELOG.lastIndex
                    // A straight line down the left with a dot per release: grey for the
                    // past, and a glowing one, in the app's accent colour, for the current.
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .drawBehind {
                                val x = 10.dp.toPx()
                                val dotY = 11.dp.toPx()
                                drawLine(
                                    color = lineColor,
                                    start = Offset(x, if (index == 0) dotY else 0f),
                                    end = Offset(x, if (last) dotY else size.height),
                                    strokeWidth = 2.dp.toPx(),
                                )
                                if (current) {
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(accent.copy(alpha = 0.55f), Color.Transparent),
                                            center = Offset(x, dotY),
                                            radius = 13.dp.toPx(),
                                        ),
                                        radius = 13.dp.toPx(),
                                        center = Offset(x, dotY),
                                    )
                                    drawCircle(color = accent, radius = 5.dp.toPx(), center = Offset(x, dotY))
                                } else {
                                    drawCircle(color = pastDot, radius = 3.5.dp.toPx(), center = Offset(x, dotY))
                                }
                            }
                            .padding(start = 28.dp, bottom = if (last) 0.dp else 16.dp),
                    ) {
                        Column {
                            Text(
                                entry.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (current) accent else MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(Modifier.height(4.dp))
                            entry.lines.forEach { line ->
                                Text("• $line", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}

/** Everyone and everything Violence credits: the fork's origin, design, fonts, lyrics and the rest. */
@Composable
internal fun CreditsDialog(onDismiss: () -> Unit) {
    GlassAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.credits)) },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                CREDITS.forEachIndexed { index, group ->
                    if (index > 0) Spacer(Modifier.height(14.dp))
                    Text(group.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    group.credits.forEach { credit ->
                        Text(credit.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(
                            credit.role,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                }
                Text(
                    stringResource(R.string.credits_disclaimer),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}
