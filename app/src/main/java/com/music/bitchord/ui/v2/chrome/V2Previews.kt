package com.music.bitchord.ui.v2.chrome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.music.bitchord.ui.components.LocalAppBackdrop
import com.music.bitchord.ui.components.LocalLiquidGlassEnabled
import com.music.bitchord.ui.components.backdrop.backdrops.layerBackdrop
import com.music.bitchord.ui.components.backdrop.backdrops.rememberLayerBackdrop
import com.music.bitchord.ui.v2.glass.GlassBudget
import com.music.bitchord.ui.v2.glass.GlassLevel
import com.music.bitchord.ui.v2.glass.LocalGlassBudget
import com.music.bitchord.ui.v2.glass.LocalGlassLevel
import com.music.bitchord.ui.v2.theme.LocalV2Colors
import com.music.bitchord.ui.v2.theme.V2Colors
import com.music.bitchord.ui.v2.theme.V2Type

/** The dock over a colourful page, at one glass level in one theme. */
@Composable
private fun DockPreview(level: GlassLevel, dark: Boolean) {
    val colors = if (dark) V2Colors.Dark else V2Colors.Light
    val backdrop = rememberLayerBackdrop()
    CompositionLocalProvider(
        LocalV2Colors provides colors,
        LocalGlassLevel provides level,
        LocalGlassBudget provides GlassBudget(),
        LocalAppBackdrop provides backdrop,
        LocalLiquidGlassEnabled provides (level == GlassLevel.FULL),
    ) {
        MaterialTheme(
            colorScheme = if (dark) androidx.compose.material3.darkColorScheme() else androidx.compose.material3.lightColorScheme(),
            typography = V2Type.material,
        ) {
            Box(Modifier.width(380.dp).height(220.dp).background(colors.background)) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .layerBackdrop(backdrop)
                        .background(Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFFEC4899), Color(0xFF14B8A6)))),
                )
                V2Dock(
                    tabLabels = listOf("Home", "Explore", "Library", "Search"),
                    selectedIndex = 0,
                    onTabSelected = {},
                    song = null,
                    isPlaying = false,
                    onPlayPause = {},
                    onNext = {},
                    onPrevious = {},
                    onExpand = {},
                    onLongPress = {},
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}

@Preview(name = "Dock FULL dark") @Composable private fun DockFullDark() = DockPreview(GlassLevel.FULL, true)
@Preview(name = "Dock FULL light") @Composable private fun DockFullLight() = DockPreview(GlassLevel.FULL, false)
@Preview(name = "Dock SIMPLIFIED dark") @Composable private fun DockSimplifiedDark() = DockPreview(GlassLevel.SIMPLIFIED, true)
@Preview(name = "Dock SIMPLIFIED light") @Composable private fun DockSimplifiedLight() = DockPreview(GlassLevel.SIMPLIFIED, false)
@Preview(name = "Dock SOLID dark") @Composable private fun DockSolidDark() = DockPreview(GlassLevel.SOLID, true)
@Preview(name = "Dock SOLID light") @Composable private fun DockSolidLight() = DockPreview(GlassLevel.SOLID, false)
@Preview(name = "Dock SIMPLIFIED, font 200%", fontScale = 2f) @Composable private fun DockLargeText() = DockPreview(GlassLevel.SIMPLIFIED, true)
