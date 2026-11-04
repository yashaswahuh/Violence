package com.music.violence.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.violence.data.settings.AppSettings
import com.music.violence.playback.autoeq.AutoEqDatabase
import com.music.violence.playback.autoeq.AutoEqParser
import com.music.violence.playback.autoeq.AutoEqProfileSummary
import com.music.violence.ui.components.GlassBottomSheet
import com.music.violence.ui.haptics.Haptic
import com.music.violence.ui.haptics.rememberHaptics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoEqPickerSheet(
    onDismiss: () -> Unit,
) {
    val haptics = rememberHaptics()
    val selectedModel by AppSettings.autoEqSelectedModel.collectAsStateWithLifecycle()
    val selectedSource by AppSettings.autoEqSelectedSource.collectAsStateWithLifecycle()
    val customCurve by AppSettings.autoEqCustomCurve.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var showImportDialog by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    var isLoaded by remember { mutableStateOf(AutoEqDatabase.isLoaded) }

    // Ensure database is indexed in background using assets
    LaunchedEffect(Unit) {
        if (!AutoEqDatabase.isLoaded) {
            if (AutoEqDatabase.streamProvider == null) {
                AutoEqDatabase.streamProvider = {
                    try {
                        context.assets.open("autoeq/autoeq_database.json.gz")
                    } catch (_: Throwable) {
                        null
                    }
                }
            }
            withContext(Dispatchers.IO) {
                AutoEqDatabase.ensureLoaded()
            }
            isLoaded = true
        }
    }

    val searchResults = remember(searchQuery, selectedCategoryFilter, isLoaded) {
        if (!isLoaded) return@remember emptyList()
        val query = searchQuery.trim()
        val baseList = if (query.isEmpty()) {
            AutoEqDatabase.popularSummaries()
        } else {
            AutoEqDatabase.search(query, limit = 80)
        }

        if (selectedCategoryFilter == "All") {
            baseList
        } else {
            baseList.filter { it.category.contains(selectedCategoryFilter, ignoreCase = true) }
        }
    }

    GlassBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Headphones,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Headphone Calibration",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    )
                }

                IconButton(
                    onClick = { showImportDialog = true },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.UploadFile,
                        contentDescription = "Import ParametricEQ.txt",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Search Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp,
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search 8,850+ headphones...",
                                    style = TextStyle(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        fontSize = 15.sp,
                                    ),
                                )
                            }
                            innerTextField()
                        },
                    )

                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.size(24.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Category Filter Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf("All", "over-ear", "in-ear").forEach { cat ->
                    val isSelected = selectedCategoryFilter.equals(cat, ignoreCase = true)
                    val label = when (cat.lowercase()) {
                        "over-ear" -> "Over-Ear"
                        "in-ear" -> "In-Ear / IEM"
                        else -> "All"
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            )
                            .clickable {
                                selectedCategoryFilter = cat
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Active / Reset row
            if (selectedModel.isNotBlank() || customCurve.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .clickable {
                            haptics.play(Haptic.Select)
                            AppSettings.setAutoEqProfile("", "")
                            AppSettings.setAutoEqCustomCurve("")
                            AppSettings.setAutoEqEnabled(false)
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (customCurve.isNotBlank()) "Custom Profile Active" else "$selectedModel ($selectedSource)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "Tap to disable calibration and reset to flat",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.Clear,
                        contentDescription = "Clear",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.height(10.dp))
            }

            if (!isLoaded) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Loading 8,850+ headphone profiles...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                Text(
                    text = if (searchQuery.isEmpty()) "POPULAR HEADPHONES & IEMS" else "RESULTS (${searchResults.size})",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )

                Spacer(Modifier.height(6.dp))

                if (searchResults.isEmpty() && searchQuery.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No headphones matching \"$searchQuery\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    // Results List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp),
                    ) {
                        items(searchResults, key = { "${it.id}_${it.model}_${it.source}" }) { item ->
                            val isChecked = customCurve.isBlank() &&
                                item.model.equals(selectedModel, ignoreCase = true) &&
                                item.source.equals(selectedSource, ignoreCase = true)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        haptics.play(Haptic.Select)
                                        AppSettings.setAutoEqProfile(item.model, item.source, item.category)
                                        AppSettings.setAutoEqCustomCurve("")
                                        AppSettings.setAutoEqEnabled(true)
                                        onDismiss()
                                    }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.model,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Medium,
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "${item.source} · ${item.category}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                    )
                                }

                                if (isChecked) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        }
                    }
                }
            }
        }
    }

    if (showImportDialog) {
        CustomProfileImportDialog(
            currentText = customCurve,
            onDismiss = { showImportDialog = false },
            onApply = { parsedText ->
                AppSettings.setAutoEqCustomCurve(parsedText)
                AppSettings.setAutoEqProfile("Custom Profile", "Imported")
                AppSettings.setAutoEqEnabled(true)
                showImportDialog = false
                onDismiss()
            },
        )
    }
}

@Composable
private fun CustomProfileImportDialog(
    currentText: String,
    onDismiss: () -> Unit,
    onApply: (String) -> Unit,
) {
    var rawText by remember { mutableStateOf(currentText) }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import Parametric EQ") },
        text = {
            Column {
                Text(
                    text = "Paste ParametricEQ.txt text (Equalizer APO syntax).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                BasicTextField(
                    value = rawText,
                    onValueChange = {
                        rawText = it
                        errorText = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(10.dp),
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                )
                if (errorText != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = errorText ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val profile = AutoEqParser.parse(rawText)
                    if (profile.filters.isEmpty()) {
                        errorText = "No valid filters detected. Format: 'Filter 1: ON PK Fc 1000 Hz Gain 3.0 dB Q 1.4'"
                    } else {
                        onApply(rawText)
                    }
                },
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}