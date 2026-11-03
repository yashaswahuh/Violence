package com.music.bitchord.ui.components

import com.music.bitchord.ui.violencestyle.violenceGlassRim
import com.music.bitchord.ui.violencestyle.violenceButtonColor
import com.music.bitchord.ui.violencestyle.violencePanelSolid
import com.music.bitchord.ui.violencestyle.violencePanelTint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

/**
 * Liquid Glass for the surfaces that live in a window of their own: dialogs and bottom sheets.
 *
 * They are drawn by the same backdrop pipeline as the bars (blur, edge lens, rim, shadow), over a
 * small still of the page taken as the first one opens — see [AppStill]. With Liquid Glass off,
 * unsupported, or under "reduce dynamic blur", every function here is exactly the Material one.
 */

/** [AlertDialog] in the app's glass material (identical to M3's while Liquid Glass is off). */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    if (!panelGlassActive()) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            confirmButton = confirmButton,
            modifier = modifier,
            dismissButton = dismissButton,
            icon = icon,
            title = title,
            text = text,
            shape = shape,
            containerColor = containerColor,
            iconContentColor = iconContentColor,
            titleContentColor = titleContentColor,
            textContentColor = textContentColor,
            tonalElevation = tonalElevation,
            properties = properties,
        )
        return
    }
    // The same anatomy as M3's AlertDialog (icon, title, text, a row of buttons), laid out here so
    // the glass can be the panel itself rather than a tint on M3's surface.
    BasicAlertDialog(onDismissRequest = onDismissRequest, modifier = modifier, properties = properties) {
        GlassSurface(
            shape = shape,
            plainColor = containerColor,
            modifier = Modifier.sizeIn(minWidth = 280.dp, maxWidth = 560.dp),
        ) {
            Column(Modifier.padding(24.dp)) {
                icon?.let {
                    CompositionLocalProvider(LocalContentColor provides iconContentColor) {
                        Box(Modifier.padding(bottom = 16.dp).align(Alignment.CenterHorizontally)) { it() }
                    }
                }
                title?.let {
                    CompositionLocalProvider(LocalContentColor provides titleContentColor) {
                        ProvideTextStyle(MaterialTheme.typography.headlineSmall) {
                            Box(
                                Modifier
                                    .padding(bottom = 16.dp)
                                    .align(if (icon == null) Alignment.Start else Alignment.CenterHorizontally),
                            ) { it() }
                        }
                    }
                }
                text?.let {
                    CompositionLocalProvider(LocalContentColor provides textContentColor) {
                        ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                            Box(
                                Modifier
                                    .weight(1f, fill = false)
                                    .padding(bottom = 24.dp)
                                    .align(Alignment.Start)
                                    .verticalScroll(rememberScrollState()),
                            ) { it() }
                        }
                    }
                }
                Box(Modifier.align(Alignment.End)) {
                    CompositionLocalProvider(LocalContentColor provides (violenceButtonColor() ?: MaterialTheme.colorScheme.primary)) {
                        ProvideTextStyle(MaterialTheme.typography.labelLarge) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                dismissButton?.invoke()
                                confirmButton()
                            }
                        }
                    }
                }
            }
        }
    }
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
    if (!panelGlassActive()) {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            sheetState = sheetState,
            shape = shape,
            containerColor = containerColor,
            contentColor = contentColor,
            content = content,
        )
        return
    }
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = shape,
        // The glass is the sheet: M3's own surface is left clear, and its bottom inset is applied
        // inside the glass, so the panel reaches the bottom of the screen behind the system bar.
        containerColor = Color.Transparent,
        contentColor = contentColor,
        tonalElevation = 0.dp,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        dragHandle = null,
    ) {
        GlassSurface(shape = shape, plainColor = containerColor, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))) {
                Box(Modifier.align(Alignment.CenterHorizontally)) { BottomSheetDefaults.DragHandle() }
                content()
            }
        }
    }
}
