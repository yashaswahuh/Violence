package com.music.bitchord.ui.brutal

/**
 * Which look the phone app wears. [CURRENT] is the existing UI and is the default; [BRUTAL] is the
 * new "brutal" look (`fork-docs/BRUTAL_UI_SPEC.md`). Stored by name in the existing settings file
 * (`AppSettings.uiVariant`). Anything unknown, including the value an earlier, removed experiment
 * wrote ("NEW"), reads as [CURRENT], so a stale or damaged settings file can never turn the new UI
 * on by accident.
 */
enum class UiVariant {
    CURRENT,
    BRUTAL,
    ;

    companion object {
        fun parse(raw: String?): UiVariant = entries.firstOrNull { it.name == raw } ?: CURRENT
    }
}

/**
 * The edge the navigation rail sits on (spec section 6.5). Physical sides, not start and end, so a
 * left-handed user gets what they chose in either text direction. Unknown values read as [RIGHT].
 */
enum class BrutalRailSide {
    LEFT,
    RIGHT,
    ;

    companion object {
        fun parse(raw: String?): BrutalRailSide = entries.firstOrNull { it.name == raw } ?: RIGHT
    }
}
