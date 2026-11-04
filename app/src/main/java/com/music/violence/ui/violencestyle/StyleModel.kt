package com.music.violence.ui.violencestyle

import androidx.compose.runtime.staticCompositionLocalOf

/*
 * Violence Style (experimental): Compose CompositionLocal for StyleState.
 *
 * Core pure enums, StyleDefaults, and StyleState live in :shared.
 */

/** Read by styled components. Default DISABLED: outside the provider nothing changes. */
val LocalViolenceStyle = staticCompositionLocalOf { StyleState.DISABLED }
