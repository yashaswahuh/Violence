package com.music.bitchord.ui.brutal.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.bitchord.ui.brutal.AccentPreset
import com.music.bitchord.ui.brutal.BrutalAccent
import com.music.bitchord.ui.brutal.glass.GlassBudget
import com.music.bitchord.ui.brutal.glass.GlassLevel
import com.music.bitchord.ui.brutal.glass.LocalGlassBudget
import com.music.bitchord.ui.brutal.glass.LocalGlassLevel
import com.music.bitchord.ui.brutal.theme.BrutalMotion
import com.music.bitchord.ui.brutal.theme.BrutalPalette
import com.music.bitchord.ui.brutal.theme.LocalBrutalMotion
import com.music.bitchord.ui.brutal.theme.LocalBrutalPalette
import com.music.bitchord.ui.components.LocalLiquidGlassEnabled
import java.time.LocalDate

/**
 * Everything the components read (palette, glass level, budget, motion), for a preview, a test or a
 * debug screen. [darkPage] false puts a light page behind the glass (the look is dark only; the light
 * page is only there to check the glass and the contrast guards against a bright backdrop).
 */
@Composable
fun BrutalPreviewHost(
    accent: BrutalAccent,
    glow: Boolean,
    level: GlassLevel = GlassLevel.SIMPLIFIED,
    darkPage: Boolean = true,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalBrutalPalette provides BrutalPalette.of(accent, glow),
        LocalGlassLevel provides level,
        LocalGlassBudget provides GlassBudget(limit = 64),
        LocalBrutalMotion provides BrutalMotion(reduced = false),
        LocalLiquidGlassEnabled provides (level == GlassLevel.FULL),
    ) {
        Box(Modifier.background(if (darkPage) BrutalPalette.Page else Color(0xFFEDEDF0))) { content() }
    }
}

/** Gradient artwork for previews and tests: no network, no loader. */
fun galleryArt(top: Color, bottom: Color): @Composable (Modifier) -> Unit = { m ->
    Box(m.background(Brush.verticalGradient(listOf(top, bottom))))
}

private val ArtOrange = galleryArt(Color(0xFFFFB000), Color(0xFFD14A00))
private val ArtBlue = galleryArt(Color(0xFF2B63FF), Color(0xFF15204F))
private val ArtRed = galleryArt(Color(0xFFD81B3B), Color(0xFF3A0610))
private val ArtGrey = galleryArt(Color(0xFFD8D2C4), Color(0xFF6B6558))

private data class GalleryTile(val id: Int, val title: String, val date: LocalDate, val aspect: Float, val art: @Composable (Modifier) -> Unit)

private val GalleryTiles = listOf(
    GalleryTile(1, "Weekend", LocalDate.of(2023, 2, 1), 1.04f, ArtOrange),
    GalleryTile(2, "Ed Sheeran", LocalDate.of(2023, 1, 13), 1.78f, ArtBlue),
    GalleryTile(3, "Chilling", LocalDate.of(2022, 11, 28), 1.0f, ArtGrey),
    GalleryTile(4, "R&B", LocalDate.of(2022, 5, 27), 1.27f, ArtRed),
)

/**
 * Every component of the kit on one page, over a [BrutalPreviewHost]: the sticker and the masthead, the
 * rail, a masonry of labelled tiles, track rows, and the loading, empty and error states.
 */
@Composable
fun BrutalComponentsGallery(
    accent: BrutalAccent,
    glow: Boolean,
    level: GlassLevel = GlassLevel.SIMPLIFIED,
    darkPage: Boolean = true,
    modifier: Modifier = Modifier,
) {
    BrutalPreviewHost(accent, glow, level, darkPage) {
        val ink = if (darkPage) Color.White else Color(0xFF111114)
        Column(modifier.fillMaxWidth().padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            // Header: the sticker bleeds off the start edge; the masthead sits at the end.
            Box(Modifier.fillMaxWidth().padding(end = 8.dp)) {
                StickerLabel(listOf("About", "Privacy"), Modifier.offset(x = (-14).dp).padding(top = 8.dp), metaLabel = "Version", metaValue = "0.4")
            }
            Box(Modifier.fillMaxWidth().padding(end = 4.dp), contentAlignment = Alignment.CenterEnd) {
                WordmarkMasthead()
            }
            Text("BRUTALISM is all about acidic colours", color = ink, fontSize = 15.sp, modifier = Modifier.padding(horizontal = 16.dp))

            // The hybrid layout: the broken grid, with the rail at the end edge.
            Row(Modifier.fillMaxWidth().height(560.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                MasonryColumns(items = GalleryTiles, key = { it.id }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(start = 24.dp, top = 24.dp, end = 4.dp, bottom = 24.dp)) { tile, m ->
                    LabeledArtTile(label = tile.title, date = tile.date, modifier = m) { tm ->
                        ArtTile(
                            title = tile.title,
                            subtitle = spokenDate(tile.date),
                            artwork = tile.art,
                            heightToWidth = tile.aspect,
                            onClick = {},
                            onPlay = {},
                            modifier = tm,
                        )
                    }
                }
                // The rail: collapse, search, then the tabs.
                Column(Modifier.padding(end = 8.dp, top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RoundGlassButton(Icons.Rounded.KeyboardArrowDown, "Collapse navigation", {}, state = "expanded")
                    RoundGlassButton(Icons.Rounded.Search, "Search", {}, tall = true)
                    listOf("Home", "Explore", "Library", "Search").forEachIndexed { i, name ->
                        GenreRailTab(name, selected = i == 1, onClick = {}, index = i, count = 4)
                    }
                }
            }
            // Rows.
            Column(Modifier.padding(horizontal = 16.dp)) {
                TrackRow("Too Young", "Post Malone", {}, durationMs = 225_000, artwork = ArtOrange, onOverflow = {})
                TrackRow("Save That Shit", "Lil Peep", {}, durationMs = 225_000, artwork = ArtGrey, onOverflow = {})
                TrackRow("6rns", "Shabjadeed", {}, durationMs = 3_723_000, artwork = ArtBlue, onOverflow = {})
            }

            // Loading, empty, error.
            BrutalLoadingGroup(Modifier.padding(horizontal = 16.dp)) {
                BrutalSkeletonRow()
                BrutalSkeletonRow()
            }
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BrutalSkeletonTile(Modifier.weight(1f), 1f)
                BrutalSkeletonTile(Modifier.weight(1f), 1.4f)
            }
            BrutalEmptyState(title = "Nothing here yet", body = "Songs you play will show up here.", actionLabel = "Browse", onAction = {})
            BrutalErrorState(message = "The server did not answer.", onRetry = {})
        }
    }
}

// ---- Previews: glow on/off x accent x light/dark page x font scale 1x and 2x ----
//
// They need Android Studio's preview pane; the JVM render test in the test sources draws the same
// gallery to PNG files without an emulator.

private val Yellow = BrutalAccent.Preset(AccentPreset.YELLOW)
private val Red = BrutalAccent.Preset(AccentPreset.RED)
private val Blue = BrutalAccent.Preset(AccentPreset.BLUE)

@Preview(name = "yellow glow dark 1x", widthDp = 360, heightDp = 1900)
@Composable
private fun YellowGlowDark() = BrutalComponentsGallery(Yellow, glow = true)

@Preview(name = "yellow no-glow dark 1x", widthDp = 360, heightDp = 1900)
@Composable
private fun YellowNoGlowDark() = BrutalComponentsGallery(Yellow, glow = false)

@Preview(name = "yellow glow dark 2x font", widthDp = 360, heightDp = 3200, fontScale = 2f)
@Composable
private fun YellowGlowDark2x() = BrutalComponentsGallery(Yellow, glow = true)

@Preview(name = "yellow no-glow dark 2x font", widthDp = 360, heightDp = 3200, fontScale = 2f)
@Composable
private fun YellowNoGlowDark2x() = BrutalComponentsGallery(Yellow, glow = false)

@Preview(name = "red glow dark 1x", widthDp = 360, heightDp = 1900)
@Composable
private fun RedGlowDark() = BrutalComponentsGallery(Red, glow = true)

@Preview(name = "blue glow dark 1x", widthDp = 360, heightDp = 1900)
@Composable
private fun BlueGlowDark() = BrutalComponentsGallery(Blue, glow = true)

@Preview(name = "blue no-glow dark 2x font", widthDp = 360, heightDp = 3200, fontScale = 2f)
@Composable
private fun BlueNoGlowDark2x() = BrutalComponentsGallery(Blue, glow = false)

@Preview(name = "yellow glow light page 1x", widthDp = 360, heightDp = 1900)
@Composable
private fun YellowGlowLight() = BrutalComponentsGallery(Yellow, glow = true, darkPage = false)

@Preview(name = "yellow no-glow light page 1x", widthDp = 360, heightDp = 1900)
@Composable
private fun YellowNoGlowLight() = BrutalComponentsGallery(Yellow, glow = false, darkPage = false)

@Preview(name = "red no-glow light page 2x font", widthDp = 360, heightDp = 3200, fontScale = 2f)
@Composable
private fun RedNoGlowLight2x() = BrutalComponentsGallery(Red, glow = false, darkPage = false)
