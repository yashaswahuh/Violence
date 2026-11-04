package com.music.violence.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import com.music.violence.ui.icons.SfIcons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import com.music.violence.ui.components.backdrop.Backdrop
import com.music.violence.ui.components.backdrop.drawBackdrop
import com.music.violence.ui.components.backdrop.effects.blur
import com.music.violence.ui.components.backdrop.effects.colorControls
import com.music.violence.ui.components.backdrop.effects.lens
import com.music.violence.ui.components.backdrop.highlight.Highlight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.music.violence.R
import com.music.violence.auth.GoogleAccountSession
import com.music.violence.auth.YouTubeProfile
import com.music.violence.data.settings.AppSettings
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.violence.ui.violencestyle.LocalViolenceStyle
import com.music.violence.ui.violencestyle.ViolenceStyleSettings
import com.music.violence.ui.violencestyle.violenceGlassRim
import com.music.violence.ui.violencestyle.violencePanelSolid
import com.music.violence.ui.violencestyle.violencePanelTint
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

/** Shared compact selector for every visible YouTube account avatar. */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun AccountProfileSelector(
    accounts: List<GoogleAccountSession>,
    activeAccountId: String?,
    activeProfileId: String?,
    hazeState: HazeState,
    onSelect: (GoogleAccountSession, YouTubeProfile) -> Unit,
    onAddAccount: () -> Unit,
    onRemoveAccount: (GoogleAccountSession) -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var managing by remember { mutableStateOf(false) }
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val shape = MaterialTheme.shapes.extraLarge
    val surface = MaterialTheme.colorScheme.surface
    val dark = surface.luminance() < 0.5f
    // The thin material lets too much of the page through behind the text. Dark
    // mode gets a black wash over it; light mode a white one, which keeps the
    // dark text readable over a busy cover instead of muddying it.
    // Violence Style: a lighter wash, so the page shows through as glass, plus a faint accent tint. Off: unchanged.
    val styled = LocalViolenceStyle.current.enabled
    val panelTint = violencePanelTint()
    val panelSolid = violencePanelSolid()
    // Style on: the menu drops from the avatar (top end), just under the top bar.
    val menuTop = if (styled) topBarHeight() + 4.dp else 56.dp
    // Violence Style, "Liquid Glass account menu" switch: the bars' glass (blur and lens over the live page).
    // Needs Liquid Glass on, Android 12+ and transparency not reduced; otherwise the frosted look above.
    val accountGlass by ViolenceStyleSettings.accountLiquidGlass.collectAsStateWithLifecycle()
    val liquid = styled && accountGlass && LocalLiquidGlassEnabled.current && isGlassSupported() &&
        !reduceDynamicBlur && !panelSolid
    val menuShape = if (liquid) RoundedCornerShape(30.dp) else shape
    val liquidGlass = if (liquid) {
        Modifier.accountLiquidGlass(LocalAppBackdrop.current, menuShape, panelTint ?: Color(0xFF121214))
    } else {
        Modifier
    }
    val material = HazeMaterials.thin(surface)
    val style = remember(material, dark, styled, panelTint) {
        HazeStyle(
            backgroundColor = material.backgroundColor,
            tints = material.tints + if (styled && panelTint != null) {
                listOf(HazeTint(Color.Black.copy(alpha = 0.12f)), HazeTint(panelTint.copy(alpha = 0.16f)))
            } else {
                listOf(HazeTint(if (dark) Color.Black.copy(alpha = 0.38f) else Color.White.copy(alpha = 0.40f)))
            },
            blurRadius = material.blurRadius,
            noiseFactor = material.noiseFactor,
            fallbackTint = material.fallbackTint,
        )
    }
    Column(
        modifier = modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = if (styled) .30f else .48f))
            .clickable(onClick = onDismiss),
        // Style on: the menu opens under the account picture, on the right.
        horizontalAlignment = if (styled) Alignment.End else Alignment.CenterHorizontally,
    ) {
        Surface(
            color = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = menuShape,
            modifier = Modifier
                .padding(top = menuTop, start = if (styled) 12.dp else 20.dp, end = if (styled) 12.dp else 20.dp)
                .then(if (styled) Modifier.fillMaxWidth(0.80f) else Modifier.fillMaxWidth())
                .clip(menuShape)
                .then(
                    if (liquid) {
                        liquidGlass
                    } else if (reduceDynamicBlur || panelSolid) {
                        Modifier.background(
                            if (styled && panelTint != null) panelTint.copy(alpha = 0.94f) else MaterialTheme.colorScheme.surface,
                        )
                    } else {
                        Modifier.optimizedHazeEffect(
                            state = hazeState,
                            style = style,
                        )
                    },
                )
                // Style on: a faint sheen from the top edge, the way light falls on glass.
                .then(
                    if (styled && !liquid && !panelSolid && !reduceDynamicBlur) {
                        Modifier.background(
                            Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.02f))),
                        )
                    } else {
                        Modifier
                    },
                )
                .then(
                    if (styled) {
                        Modifier.violenceGlassRim(menuShape)
                    } else {
                        // The same hairline the mini player and the nav bar wear.
                        Modifier.border(
                            0.5.dp,
                            // White on dark like the bars; a light surface has nothing
                            // for a white hairline to show against.
                            if (dark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.10f),
                            shape,
                        )
                    },
                )
                .clickable(onClick = {}),
        ) {
            // Room under the last row to match the space between rows, so the
            // list doesn't stop short against the card's edge.
            LazyColumn(contentPadding = PaddingValues(bottom = 14.dp)) {
                item { Text(stringResource(R.string.switch_account), style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(20.dp, 18.dp, 20.dp, 8.dp)) }
                accounts.forEach { account ->
                item {
                    Text(account.email.ifBlank { account.name.ifBlank { stringResource(R.string.accounts) } },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp, 12.dp, 20.dp, 4.dp))
                }
                items(account.profiles.size) { index ->
                    val profile = account.profiles[index]
                    ProfileRow(profile, account.accountId == activeAccountId && profile.profileId == activeProfileId,
                        managing, { onSelect(account, profile); onDismiss() }, { onRemoveAccount(account) })
                }
                }
                item { SelectorAction(SfIcons.AddAccount, stringResource(R.string.add_account), onClick = onAddAccount) }
                // A wide glyph — two people and a gear — drawn a size up to match the others.
                item { SelectorAction(SfIcons.ManageAccounts, stringResource(R.string.manage_accounts), iconSize = 28.dp) { managing = !managing } }
                item { SelectorAction(SfIcons.Settings, stringResource(R.string.settings), onClick = onOpenSettings) }
            }
        }
    }
}

@Composable private fun ProfileRow(profile: YouTubeProfile, selected: Boolean, managing: Boolean,
    onClick: () -> Unit, onRemove: () -> Unit) {
    val description = stringResource(if (selected) R.string.selected_account else R.string.switch_account)
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.RadioButton, onClick = if (managing) onRemove else onClick)
        .semantics { contentDescription = description }
        .padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (profile.avatar != null) AsyncImage(profile.avatar, null, Modifier.size(38.dp).clip(MaterialTheme.shapes.extraLarge))
        else Icon(SfIcons.AccountPlaceholder, null, Modifier.size(38.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) {
            Text(profile.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(profile.handle.ifBlank { stringResource(if (profile.isBrandAccount) R.string.brand_account else R.string.personal) },
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (selected && !managing) Icon(Icons.Rounded.Check, stringResource(R.string.selected_account), tint = MaterialTheme.colorScheme.primary)
        if (managing) Text(stringResource(R.string.sign_out), color = MaterialTheme.colorScheme.error)
    }
}

@Composable private fun SelectorAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    iconSize: Dp = 24.dp,
    onClick: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(role = Role.Button, onClick = onClick)
        .semantics { contentDescription = label }.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        // In the avatars' column, so the labels line up with the names above.
        Box(Modifier.width(38.dp), contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(iconSize)) }
        Spacer(Modifier.width(12.dp)); Text(label)
    }
}

/**
 * The account menu as Liquid Glass: the live page blurred, lensed at the edges (Android 13+; Android 12
 * gets the blur) and a light-catching highlight, under a thin accent-tinted fill. The same pipeline as the
 * bars and the song menu.
 */
@Composable
private fun Modifier.accountLiquidGlass(backdrop: Backdrop, shape: androidx.compose.ui.graphics.Shape, tint: Color): Modifier {
    val density = LocalDensity.current
    val blurPx = with(density) { 20.dp.toPx() } * ACCOUNT_GLASS_SCALE
    val lensPx = with(density) { 22.dp.toPx() } * ACCOUNT_GLASS_SCALE
    val fill = remember(tint) { tint.copy(alpha = 0.30f) }
    val glass = remember(backdrop, shape, blurPx, lensPx, fill) {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                colorControls(saturation = 1.8f)
                blur(blurPx)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    lens(
                        refractionHeight = lensPx,
                        refractionAmount = lensPx,
                        depthEffect = true,
                        chromaticAberration = true,
                    )
                }
            },
            highlight = { Highlight.Default },
            shadow = null,
            onDrawSurface = { drawRect(fill) },
            backdropScale = ACCOUNT_GLASS_SCALE,
        )
    }
    return then(glass)
}

private const val ACCOUNT_GLASS_SCALE = 0.25f
