package com.music.bitchord.ui.brutal.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.music.bitchord.ui.brutal.BrutalAccent
import com.music.bitchord.ui.brutal.theme.BrutalPalette
import com.music.bitchord.ui.brutal.theme.BrutalShapes
import com.music.bitchord.ui.brutal.theme.LocalBrutalPalette
import com.music.bitchord.ui.components.LocalAppBackdrop
import com.music.bitchord.ui.components.LocalLiquidGlassEnabled
import com.music.bitchord.ui.components.backdrop.backdrops.layerBackdrop
import com.music.bitchord.ui.components.backdrop.backdrops.rememberLayerBackdrop
import kotlin.math.abs

/**
 * The screen the glass is measured on (spec 7.1, the task's section 5): [liveSurfaces] Floating
 * surfaces over a lazy list of [tiles] Tiles that scrolls by itself, up and down, for as long as it
 * is shown. With the app's `FrameStats` attached (debug builds, `adb logcat -s VioFrames`) this
 * gives the frame times of "three live surfaces over a scrolling list of 30 tiles".
 *
 * It is a harness: nothing in the app shows it. Put it behind a debug-only entry when measuring, on
 * the reference phone, and record the device with the numbers. The list is wrapped in
 * [BrutalGlassListScope], so a live surface can never end up inside an item.
 */
@Composable
fun BrutalGlassPerfHarness(
    modifier: Modifier = Modifier,
    liveSurfaces: Int = 3,
    tiles: Int = 30,
    glow: Boolean = true,
    level: GlassLevel = GlassLevel.FULL,
    accent: BrutalAccent = BrutalAccent.Default,
    autoScroll: Boolean = true,
) {
    val backdrop = rememberLayerBackdrop()
    val listState = rememberLazyListState()
    if (autoScroll) {
        LaunchedEffect(Unit) {
            // Continuous scrolling is the point of a harness; the app never animates like this idle.
            var down = true
            while (true) {
                val target = if (down) 6000f else -6000f
                var done = 0f
                while (abs(done) < abs(target)) {
                    val step = if (down) 14f else -14f
                    listState.scrollBy(step)
                    done += step
                    withFrameNanos { }
                }
                down = !down
            }
        }
    }
    CompositionLocalProvider(
        LocalBrutalPalette provides BrutalPalette.of(accent, glow),
        LocalGlassLevel provides level,
        LocalGlassBudget provides GlassBudget(),
        LocalAppBackdrop provides backdrop,
        LocalLiquidGlassEnabled provides (level == GlassLevel.FULL),
    ) {
        Box(modifier.fillMaxSize().background(BrutalPalette.Page)) {
            Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
                BrutalGlassListScope {
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                        items((0 until tiles).toList(), key = { it }) { index ->
                            val hue = (index * 37) % 360
                            Box(
                                Modifier
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .brutalGlass(BrutalShapes.Tile, GlassKind.Tile),
                            ) {
                                Box(
                                    Modifier
                                        .padding(5.dp)
                                        .fillMaxSize()
                                        .clip(BrutalShapes.ListArtwork)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color.hsv(hue.toFloat(), 0.8f, 0.9f), Color.hsv(((hue + 60) % 360).toFloat(), 0.9f, 0.4f)),
                                            ),
                                        ),
                                )
                            }
                        }
                    }
                }
            }
            // The live surfaces: they are outside the list, over it.
            repeat(liveSurfaces) { i ->
                Box(
                    Modifier
                        .align(if (i == 0) Alignment.TopCenter else Alignment.BottomCenter)
                        // One at the top, one at the bottom, one lifted above it (a bar, a dock, a panel).
                        .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = if (i == 2) 120.dp else 24.dp)
                        .fillMaxWidth()
                        .height(80.dp)
                        .brutalGlass(BrutalShapes.Stadium, GlassKind.Floating),
                )
            }
        }
    }
}
