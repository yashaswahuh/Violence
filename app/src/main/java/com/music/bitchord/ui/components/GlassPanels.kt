package com.music.bitchord.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

/**
 * Liquid Glass for the surfaces that live in a window of their own: dialogs and bottom sheets.
 *
 * They cannot sample the page behind them the way the floating bars do (the page is drawn in a
 * different window, and recording it a second time for every dialog would be a cost no dialog
 * deserves), so they wear the rest of the material instead: the glass tint, the light-catching
 * rim and the hairline edge. It is nearly opaque — through a scrim the page's own text would
 * otherwise show through a clear panel and make it unreadable. With Liquid Glass off, or
 * unsupported, every function here returns exactly what it was given.
 */
private const val PANEL_OPACITY = 0.90f

/** The fill for a glass panel: the glass tint in place of [normal] while Liquid Glass is on. */
@Composable
fun glassPanelColor(normal: Color): Color {
    if (!LocalLiquidGlassEnabled.current || !isGlassSupported()) return normal
    val tint = if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) Color(0xFFFAFAFA) else Color(0xFF161618)
    return tint.copy(alpha = PANEL_OPACITY)
}

/** The rim and hairline of a glass panel of [shape]; nothing while Liquid Glass is off. */
@Composable
fun Modifier.glassPanelEdge(shape: CornerBasedShape): Modifier {
    if (!LocalLiquidGlassEnabled.current || !isGlassSupported()) return this
    return liquidGlassRim(shape).border(GLASS_EDGE_WIDTH, GLASS_EDGE_COLOR, shape)
}

/** [AlertDialog] in the app's glass material (identical to M3's while Liquid Glass is off). */
@Composable
fun GlassAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    shape: CornerBasedShape = AlertDialogDefaults.shape as? CornerBasedShape ?: RoundedCornerShape(28.dp),
    containerColor: Color = AlertDialogDefaults.containerColor,
    iconContentColor: Color = AlertDialogDefaults.iconContentColor,
    titleContentColor: Color = AlertDialogDefaults.titleContentColor,
    textContentColor: Color = AlertDialogDefaults.textContentColor,
    tonalElevation: androidx.compose.ui.unit.Dp = AlertDialogDefaults.TonalElevation,
    properties: DialogProperties = DialogProperties(),
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        // Tint only: M3 hands this modifier to a box that includes the dialog's outer padding, so a rim
        // drawn on it would outline more than the dialog.
        modifier = modifier,
        dismissButton = dismissButton,
        icon = icon,
        title = title,
        text = text,
        shape = shape,
        containerColor = glassPanelColor(containerColor),
        iconContentColor = iconContentColor,
        titleContentColor = titleContentColor,
        textContentColor = textContentColor,
        tonalElevation = tonalElevation,
        properties = properties,
    )
}

/** [ModalBottomSheet] in the app's glass material (identical to M3's while Liquid Glass is off). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    shape: CornerBasedShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    containerColor: Color = BottomSheetDefaults.ContainerColor,
    contentColor: Color = contentColorFor(containerColor),
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        // No rim here: the sheet's modifier lands on a box that spans the whole window, so an edge
        // drawn on it outlined the screen above the sheet. The tint is what carries the glass.
        modifier = modifier,
        sheetState = sheetState,
        shape = shape,
        containerColor = glassPanelColor(containerColor),
        contentColor = contentColor,
        content = content,
    )
}
