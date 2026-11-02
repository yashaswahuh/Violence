package com.music.bitchord.ui.brutal.components

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode
import org.robolectric.annotation.GraphicsMode

/** Proves the JVM renderer draws real pixels (a red box on black) before any component relies on it. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = android.app.Application::class, qualifiers = "w360dp-h640dp-xxhdpi")
@ConscryptMode(ConscryptMode.Mode.OFF)
class JvmRenderSmokeTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun aComposeTreeDrawsRealPixels() {
        rule.setContent {
            Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                Box(Modifier.size(100.dp).background(Color.Red)) { Text("Hi", color = Color.White) }
            }
        }
        val bmp = rule.renderToBitmap()
        saveRender("smoke", bmp)
        assertTrue("the decor view has a size: ${bmp.width} x ${bmp.height}", bmp.width > 100 && bmp.height > 100)
        val centre = bmp.getPixel(bmp.width / 2, bmp.height / 2)
        // The centre is inside the red box (the text is small and white, off to the start).
        val r = (centre shr 16) and 0xFF
        assertTrue("centre pixel red channel $r", r > 150)
        assertEquals("the corner is the black page", 0, bmp.getPixel(2, bmp.height / 2) and 0xFFFFFF)
    }
}
