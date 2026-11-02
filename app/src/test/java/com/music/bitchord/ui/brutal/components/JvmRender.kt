package com.music.bitchord.ui.brutal.components

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.test.ext.junit.rules.ActivityScenarioRule
import java.io.File
import java.io.FileOutputStream

/**
 * A real Compose tree drawn to a bitmap on the JVM, with no emulator and no window capture.
 *
 * `captureToImage` waits for a draw callback that Robolectric never fires (it timed out in the glass
 * kit's first attempt), so this draws the activity's decor view straight into a bitmap. It needs
 * `@GraphicsMode(GraphicsMode.Mode.NATIVE)` on the test class (real Skia drawing; in legacy mode the
 * bitmap stays blank).
 */
fun AndroidComposeTestRule<ActivityScenarioRule<ComponentActivity>, ComponentActivity>.renderToBitmap(): Bitmap {
    waitForIdle()
    val view = activity.window.decorView
    val bitmap = Bitmap.createBitmap(view.width.coerceAtLeast(1), view.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    runOnUiThread { view.draw(Canvas(bitmap)) }
    return bitmap
}

/** PNGs from the JVM renders go to `app/build/brutal-components/` (copy the ones worth keeping into the docs). */
fun saveRender(name: String, bitmap: Bitmap) {
    val dir = File("build/brutal-components").apply { mkdirs() }
    FileOutputStream(File(dir, "$name.png")).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
}
