package com.music.bitchord.ui.brutal.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.bitchord.ui.brutal.AccentPreset
import com.music.bitchord.ui.brutal.BrutalAccent
import com.music.bitchord.ui.brutal.theme.BrutalPalette
import com.music.bitchord.ui.brutal.theme.BrutalShapes
import com.music.bitchord.ui.brutal.theme.LocalBrutalPalette
import com.music.bitchord.ui.components.LocalAppBackdrop
import com.music.bitchord.ui.components.LocalLiquidGlassEnabled
import com.music.bitchord.ui.components.backdrop.backdrops.layerBackdrop
import com.music.bitchord.ui.components.backdrop.backdrops.rememberLayerBackdrop

/**
 * The gallery the glass is checked against the mockups with: every kind of surface, over a page
 * with colour in it (so a live backdrop has something to sample), at one level, one accent and one
 * Glow setting, on a dark or a light page.
 *
 * It provides everything the kit reads (palette, level, a budget with room for all of it, the page
 * backdrop), so it can be dropped into a preview, a Robolectric test or a debug screen as it is.
 * No page still is provided, so a Panel at FULL falls to SIMPLIFIED here, as it does anywhere that
 * has none.
 */
@Composable
fun BrutalGlassGallery(
    level: GlassLevel,
    accent: BrutalAccent,
    glow: Boolean,
    darkPage: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val backdrop = rememberLayerBackdrop()
    CompositionLocalProvider(
        LocalBrutalPalette provides BrutalPalette.of(accent, glow),
        LocalGlassLevel provides level,
        LocalGlassBudget provides GlassBudget(limit = 64),
        LocalAppBackdrop provides backdrop,
        LocalLiquidGlassEnabled provides (level == GlassLevel.FULL),
    ) {
        Box(modifier.layerBackdrop(backdrop).background(if (darkPage) BrutalPalette.Page else Color(0xFFEDEDF0))) {
            GalleryPage(darkPage)
        }
    }
}

/** Glow off and on, side by side, for the same level and accent: the comparison the previews make. */
@Composable
fun BrutalGlassGalleryPair(level: GlassLevel, accent: BrutalAccent, darkPage: Boolean = true) {
    Row(Modifier.background(if (darkPage) BrutalPalette.Page else Color(0xFFEDEDF0))) {
        Column(Modifier.width(360.dp)) {
            GalleryCaption("NO-GLOW  $level", darkPage)
            BrutalGlassGallery(level, accent, glow = false, darkPage = darkPage)
        }
        Column(Modifier.width(360.dp)) {
            GalleryCaption("GLOW  $level", darkPage)
            BrutalGlassGallery(level, accent, glow = true, darkPage = darkPage)
        }
    }
}

@Composable
private fun GalleryCaption(text: String, darkPage: Boolean) {
    Text(
        text = text,
        color = if (darkPage) Color.White else Color.Black,
        fontSize = 12.sp,
        modifier = Modifier.padding(start = 16.dp, top = 8.dp),
    )
}

@Composable
private fun GalleryPage(darkPage: Boolean) {
    val ink = if (darkPage) Color.White else Color(0xFF111114)
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        // Colour behind the bar, so the backdrop (and its lens) has something to bend.
        Box {
            Row(Modifier.fillMaxWidth().height(96.dp)) {
                listOf(Color(0xFF1B4DFF), Color(0xFFFF7A00), Color(0xFFF2F2F2), Color(0xFF7A1FA2)).forEach {
                    Box(Modifier.weight(1f).height(96.dp).background(it))
                }
            }
            // A mini player: artwork, title, artist, chevron.
            Row(
                Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .height(80.dp)
                    .brutalGlass(BrutalShapes.Stadium, GlassKind.Floating),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .padding(start = 12.dp)
                        .size(56.dp)
                        .clip(BrutalShapes.ListArtwork)
                        .background(Brush.linearGradient(listOf(Color(0xFF2B63FF), Color(0xFF1D2B6B)))),
                )
                Column(Modifier.padding(start = 12.dp).weight(1f)) {
                    Text("Betrayed", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                    Text("Lil Xan", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                }
                Text("^", color = Color.White, fontSize = 20.sp, modifier = Modifier.padding(end = 20.dp))
            }
        }

        // Round buttons and a rail tab: Floating, icon only, over the plain page.
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            repeat(3) { i ->
                Box(
                    Modifier.size(48.dp).brutalGlass(BrutalShapes.Stadium, GlassKind.Floating, textScrim = false),
                    contentAlignment = Alignment.Center,
                ) { Text(listOf("v", "o", "+")[i], color = ink, fontSize = 18.sp) }
            }
            Box(
                Modifier.size(width = 48.dp, height = 96.dp).brutalGlass(BrutalShapes.Stadium, GlassKind.Floating, textScrim = false),
                contentAlignment = Alignment.Center,
            ) { Text("LIBRARY", color = ink, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.rotate(-90f)) }
            Box(
                Modifier.size(width = 48.dp, height = 96.dp).brutalGlass(BrutalShapes.Stadium, GlassKind.Floating, textScrim = false),
                contentAlignment = Alignment.Center,
            ) { Text("HOME", color = ink, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.rotate(-90f)) }
        }

        // A panel: a dialog or sheet.
        Column(
            Modifier
                .fillMaxWidth()
                .brutalGlass(RoundedCornerShape(28.dp), GlassKind.Panel)
                .padding(20.dp),
        ) {
            Text("Make your Violence profile?", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("Pick a name and a picture so friends see you in Jam.", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
        }

        // Tiles (artwork in a frosted frame) and a chip.
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf(
                listOf(Color(0xFFFFB000), Color(0xFFD14A00)),
                listOf(Color(0xFF2B63FF), Color(0xFF15204F)),
                listOf(Color(0xFFD81B3B), Color(0xFF3A0610)),
            ).forEach { art ->
                Box(Modifier.size(96.dp).brutalGlass(BrutalShapes.Tile, GlassKind.Tile)) {
                    Box(
                        Modifier
                            .padding(5.dp)
                            .fillMaxWidth()
                            .height(86.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.verticalGradient(art)),
                    )
                    Box(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(LocalBrutalPalette.current.accentFill),
                    )
                }
            }
            Box(
                Modifier.height(24.dp).brutalGlass(BrutalShapes.Stadium, GlassKind.Chip, textScrim = false).padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) { Text("03:45", color = ink, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

// ---- Previews: the matrix of the task (kind x level x glow x accent x page) ----
//
// Each preview draws NO-GLOW and GLOW side by side for one level and accent, on a dark or a light
// page. They need Android Studio's preview pane (or the JVM render test in the test sources).

private val Yellow = BrutalAccent.Preset(AccentPreset.YELLOW)
private val Red = BrutalAccent.Preset(AccentPreset.RED)
private val Blue = BrutalAccent.Preset(AccentPreset.BLUE)

@Preview(name = "FULL yellow dark", widthDp = 720, heightDp = 640)
@Composable
private fun GalleryFullYellowDark() = BrutalGlassGalleryPair(GlassLevel.FULL, Yellow)

@Preview(name = "SIMPLIFIED yellow dark", widthDp = 720, heightDp = 640)
@Composable
private fun GallerySimplifiedYellowDark() = BrutalGlassGalleryPair(GlassLevel.SIMPLIFIED, Yellow)

@Preview(name = "SOLID yellow dark", widthDp = 720, heightDp = 640)
@Composable
private fun GallerySolidYellowDark() = BrutalGlassGalleryPair(GlassLevel.SOLID, Yellow)

@Preview(name = "FULL red dark", widthDp = 720, heightDp = 640)
@Composable
private fun GalleryFullRedDark() = BrutalGlassGalleryPair(GlassLevel.FULL, Red)

@Preview(name = "SIMPLIFIED red dark", widthDp = 720, heightDp = 640)
@Composable
private fun GallerySimplifiedRedDark() = BrutalGlassGalleryPair(GlassLevel.SIMPLIFIED, Red)

@Preview(name = "FULL blue dark", widthDp = 720, heightDp = 640)
@Composable
private fun GalleryFullBlueDark() = BrutalGlassGalleryPair(GlassLevel.FULL, Blue)

@Preview(name = "SIMPLIFIED blue dark", widthDp = 720, heightDp = 640)
@Composable
private fun GallerySimplifiedBlueDark() = BrutalGlassGalleryPair(GlassLevel.SIMPLIFIED, Blue)

@Preview(name = "FULL yellow light page", widthDp = 720, heightDp = 640)
@Composable
private fun GalleryFullYellowLight() = BrutalGlassGalleryPair(GlassLevel.FULL, Yellow, darkPage = false)

@Preview(name = "SIMPLIFIED yellow light page", widthDp = 720, heightDp = 640)
@Composable
private fun GallerySimplifiedYellowLight() = BrutalGlassGalleryPair(GlassLevel.SIMPLIFIED, Yellow, darkPage = false)
