package com.coinflipper.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coinflipper.app.R
import com.coinflipper.app.data.AppSettings
import com.coinflipper.app.data.CoinSkin
import com.coinflipper.app.data.LabelPreset
import com.coinflipper.app.data.Side

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    settings: AppSettings,
    onChange: ((AppSettings) -> AppSettings) -> Unit,
    modifier: Modifier = Modifier,
    rewardedReady: Boolean = false,
    onUnlockSkin: (CoinSkin) -> Unit = {},
    removeAdsPrice: String? = null,
    onRemoveAds: () -> Unit = {},
    showPrivacyOptions: Boolean = false,
    onPrivacyOptions: () -> Unit = {},
) {
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
    ) {
        Text(
            stringResource(R.string.settings),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )

        SectionTitle(stringResource(R.string.coin_style))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            CoinSkin.entries.forEach { skin ->
                val selected = skin == settings.skin
                val locked = settings.isLocked(skin)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(role = Role.RadioButton) {
                            if (locked) onUnlockSkin(skin) else onChange { it.copy(skin = skin) }
                        }
                        .padding(6.dp),
                ) {
                    Box(
                        Modifier
                            .size(64.dp)
                            .then(if (selected) Modifier.border(2.dp, skin.metal.light, CircleShape) else Modifier)
                            .padding(5.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        RealCoinFace(Side.HEADS, skin.metal, label = "", modifier = Modifier.fillMaxSize())
                        if (locked) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.55f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Filled.Lock,
                                    contentDescription = stringResource(R.string.locked),
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(skin.nameRes),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) skin.metal.light else VaultColors.TextSecondary,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    )
                    if (locked) {
                        Text(
                            stringResource(if (rewardedReady) R.string.watch_to_unlock else R.string.unlock_with_ad),
                            style = MaterialTheme.typography.labelSmall,
                            color = skin.metal.light,
                        )
                    }
                }
            }
        }

        SectionTitle(stringResource(R.string.labels))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                LabelPreset.HEADS_TAILS to R.string.labels_heads_tails,
                LabelPreset.YES_NO to R.string.labels_yes_no,
                LabelPreset.CUSTOM to R.string.labels_custom,
            ).forEach { (preset, label) ->
                FilterChip(
                    selected = settings.labels == preset,
                    onClick = { onChange { it.copy(labels = preset) } },
                    label = { Text(stringResource(label)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = settings.skin.metal.base.copy(alpha = 0.25f),
                        selectedLabelColor = settings.skin.metal.light,
                    ),
                )
            }
        }
        if (settings.labels == LabelPreset.CUSTOM) {
            // Local text state keeps the cursor stable while typing.
            var heads by rememberSaveable { mutableStateOf(settings.customHeads) }
            var tails by rememberSaveable { mutableStateOf(settings.customTails) }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = heads,
                onValueChange = { v ->
                    heads = v.take(AppSettings.MAX_LABEL_LENGTH)
                    onChange { it.copy(customHeads = heads) }
                },
                label = { Text(stringResource(R.string.heads_side)) },
                placeholder = { Text(stringResource(R.string.custom_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = tails,
                onValueChange = { v ->
                    tails = v.take(AppSettings.MAX_LABEL_LENGTH)
                    onChange { it.copy(customTails = tails) }
                },
                label = { Text(stringResource(R.string.tails_side)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(12.dp))
        SwitchRow(
            title = stringResource(R.string.sound_effects),
            subtitle = stringResource(R.string.sound_effects_sub),
            checked = settings.sound,
            onCheckedChange = { on -> onChange { it.copy(sound = on) } },
        )
        SwitchRow(
            title = stringResource(R.string.shake_to_flip),
            subtitle = stringResource(R.string.shake_to_flip_sub),
            checked = settings.shake,
            onCheckedChange = { on -> onChange { it.copy(shake = on) } },
        )

        SectionTitle(stringResource(R.string.ads_section))
        if (settings.adsRemoved) {
            Text(
                stringResource(R.string.ads_removed_thanks),
                style = MaterialTheme.typography.bodyMedium,
                color = settings.skin.metal.light,
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.remove_ads), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.remove_ads_sub),
                        style = MaterialTheme.typography.bodySmall,
                        color = VaultColors.TextSecondary,
                    )
                }
                Button(onClick = onRemoveAds, enabled = removeAdsPrice != null) {
                    Text(removeAdsPrice ?: stringResource(R.string.not_available))
                }
            }
        }
        if (showPrivacyOptions) {
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onPrivacyOptions, contentPadding = PaddingValues(0.dp)) {
                Text(stringResource(R.string.privacy_options))
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Spacer(Modifier.height(20.dp))
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = VaultColors.TextSecondary,
        fontWeight = FontWeight.Bold,
    )
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Switch) { onCheckedChange(!checked) }
            .padding(vertical = 10.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = VaultColors.TextSecondary)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
