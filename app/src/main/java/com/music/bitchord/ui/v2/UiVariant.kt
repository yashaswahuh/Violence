package com.music.bitchord.ui.v2

/**
 * Which look the phone app wears. [CURRENT] is the existing UI and is the default; [NEW] is the
 * custom UI (Lumen: dock and orb, solid pages, glass for focus layers). Stored by name in the
 * existing settings file (see `AppSettings.uiVariant`); anything unknown reads as [CURRENT], so a
 * settings file from a build without the new UI can never turn it on by accident.
 */
enum class UiVariant {
    CURRENT,
    NEW,
    ;

    companion object {
        fun parse(raw: String?): UiVariant = entries.firstOrNull { it.name == raw } ?: CURRENT
    }
}
