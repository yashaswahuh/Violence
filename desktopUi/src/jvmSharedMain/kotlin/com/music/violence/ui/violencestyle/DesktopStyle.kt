package com.music.violence.ui.violencestyle

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

/**
 * Colours, heading text styles and strengths the desktop components need under Violence Style.
 * Defaults are null / false / DISABLED: with the style off, every component renders
 * 100% identically to upstream.
 */
@Immutable
class ViolenceSharedStyle(
    val accent: Color,
    val accentSoft: Color,
    val textAccent: Color,
    /** The neutral (White) accent: no coloured rim light. */
    val neutral: Boolean,
    /** Peak glow alpha of the chosen intensity (0.38, 0.62, 0.85); scales the rim bloom. */
    val strength: Float,
    /** "TOP PICKS": display serif, caps. */
    val displayCaps: TextStyle,
    /** "for You": the italic of the same family. */
    val displayItalic: TextStyle,
    /** "RECENTS". */
    val sectionCaps: TextStyle,
    /** Card title in caps. */
    val cardTitleCaps: TextStyle,
    /** Card label and subtitle (the body face). */
    val label: TextStyle,
    /** True when the live background may move (motion not Off, FULL mode, no reduce motion). */
    val motion: Boolean = false,
)

/** Read by desktop styled components. Default DISABLED: outside the provider nothing changes. */
val LocalViolenceStyle = staticCompositionLocalOf { StyleState.DISABLED }

/** Null while the style is off. */
val LocalViolenceShared = compositionLocalOf<ViolenceSharedStyle?> { null }

/** Provided by style host; defaults to AccentTokens.DEFAULT outside provider. */
val LocalViolenceAccent = compositionLocalOf { AccentTokens.DEFAULT }

/** True while the live background is showing, allowing transparent page fills. Default false. */
val LocalViolenceSeeThrough = staticCompositionLocalOf { false }

