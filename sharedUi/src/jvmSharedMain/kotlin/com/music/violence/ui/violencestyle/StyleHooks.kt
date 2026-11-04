package com.music.violence.ui.violencestyle

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Violence Style (experimental), the only hook the shared UI needs: true while the phone app's live
 * background is showing, so a shared screen may drop an opaque page fill and let it show through.
 *
 * The default is false: with the style off, or in any host that never provides it (the desktop app),
 * every shared screen keeps the fill it always had.
 */
val LocalViolenceSeeThrough = staticCompositionLocalOf { false }
