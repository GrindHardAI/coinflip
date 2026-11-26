package com.coinflipper.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import android.widget.Toast
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coinflipper.app.CoinFlipViewModel
import com.coinflipper.app.R
import com.coinflipper.app.data.AppSettings
import com.coinflipper.app.data.FlipStats
import com.coinflipper.app.data.LabelPreset
import com.coinflipper.app.data.Side
import com.coinflipper.app.monetization.BannerAd
import com.coinflipper.app.monetization.Monetization
import com.coinflipper.app.monetization.NoMonetization
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val FLIP_DURATION_MS = 1400
private const val MIN_HALF_TURNS = 8

/** The word shown for a side, based on the chosen label preset. */
@Composable
fun sideLabel(side: Side, settings: AppSettings): String {
    val default = stringResource(if (side == Side.HEADS) R.string.heads else R.string.tails)
    return when (settings.labels) {
        LabelPreset.HEADS_TAILS -> default
        LabelPreset.YES_NO -> stringResource(if (side == Side.HEADS) R.string.yes else R.string.no)
        LabelPreset.CUSTOM -> (if (side == Side.HEADS) settings.customHeads else settings.customTails)
            .trim().ifEmpty { default }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinFlipScreen(vm: CoinFlipViewModel = viewModel(), monetization: Monetization = NoMonetization) {
    val stats by vm.stats.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val adsReady by monetization.adsReady.collectAsStateWithLifecycle()
    val rewardedReady by monetization.rewardedReady.collectAsStateWithLifecycle()
    val privacyOptionsRequired by monetization.privacyOptionsRequired.collectAsStateWithLifecycle()
    val removeAdsPrice by monetization.removeAdsPrice.collectAsStateWithLifecycle()
    val metal = settings.skin.metal
    val headsLabel = sideLabel(Side.HEADS, settings)
    val tailsLabel = sideLabel(Side.TAILS, settings)

    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val sounds = remember { CoinSounds(context.applicationContext) }
    DisposableEffect(Unit) { onDispose { sounds.release() } }

    val rotation = remember { Animatable(if (stats.last == Side.TAILS) 180f else 0f) }
    val toss = remember { Animatable(0f) }
    var flipping by remember { mutableStateOf(false) }
    var showReset by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }

    fun flip() {
        if (flipping) return
        flipping = true
        val result = vm.nextResult()
        val withSound = settings.sound
        if (withSound) sounds.playFlip()
        scope.launch {
            val start = rotation.value
            val showingHeads = (start / 180f).roundToInt() % 2 == 0
            val landsOnHeads = result == Side.HEADS
            // Even number of half-turns keeps the same face; odd switches it.
            val halfTurns = MIN_HALF_TURNS + if (showingHeads == landsOnHeads) 0 else 1
            val target = start + halfTurns * 180f

            toss.snapTo(0f)
            val tossJob = launch { toss.animateTo(1f, tween(FLIP_DURATION_MS, easing = LinearEasing)) }
            rotation.animateTo(target, tween(FLIP_DURATION_MS, easing = FastOutSlowInEasing))
            tossJob.join()
            // Keep the angle small so float precision never drifts.
            rotation.snapTo(target % 360f)
            toss.snapTo(0f)

            if (withSound) sounds.playLand()
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            vm.record(result)
            flipping = false
            // Give the player a moment to see the result before any full-screen ad.
            kotlinx.coroutines.delay(900)
            monetization.onFlipCompleted(adsRemoved = settings.adsRemoved)
        }
    }

    ShakeEffect(enabled = settings.shake && !showSettings && !showReset) { flip() }

    // Accent the Material components (switches, chips, fields) with the coin's metal.
    val scheme = MaterialTheme.colorScheme.copy(primary = metal.light, onPrimary = metal.deep)
    MaterialTheme(colorScheme = scheme) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(VaultColors.BackgroundTop, VaultColors.BackgroundBottom)))
                .drawBehind {
                    // Soft spotlight behind the coin, tinted by the coin's metal.
                    val c = Offset(size.width / 2f, size.height * 0.34f)
                    val radius = size.width * 0.8f
                    drawCircle(
                        Brush.radialGradient(listOf(metal.light.copy(alpha = 0.16f), Color.Transparent), center = c, radius = radius),
                        radius = radius,
                        center = c,
                    )
                },
        ) {
            Scaffold(
                containerColor = Color.Transparent,
                bottomBar = {
                    if (adsReady && !settings.adsRemoved) {
                        BannerAd(Modifier.windowInsetsPadding(WindowInsets.navigationBars))
                    }
                },
                topBar = {
                    CenterAlignedTopAppBar(
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = Color.Transparent,
                            navigationIconContentColor = VaultColors.TextSecondary,
                            actionIconContentColor = VaultColors.TextSecondary,
                        ),
                        title = {
                            Text(
                                stringResource(R.string.app_name),
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                color = metal.light,
                                letterSpacing = 1.sp,
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { showReset = true }, enabled = !flipping && stats.total > 0) {
                                Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.reset))
                            }
                        },
                        actions = {
                            IconButton(onClick = { showSettings = true }) {
                                Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings))
                            }
                        },
                    )
                },
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(88.dp)) // headroom for the toss arc

                    val faceName = if (stats.last == Side.TAILS) tailsLabel else headsLabel
                    val coinDescription = stringResource(R.string.coin_description, faceName)
                    CoinView(
                        angle = rotation.value,
                        toss = toss.value,
                        metal = metal,
                        headsLabel = headsLabel,
                        tailsLabel = tailsLabel,
                        modifier = Modifier
                            .size(200.dp)
                            .semantics { contentDescription = coinDescription }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                role = Role.Button,
                                onClick = { flip() },
                            ),
                    )

                    Spacer(Modifier.height(24.dp))
                    ResultText(flipping, stats.last, headsLabel, tailsLabel, settings.shake)
                    Spacer(Modifier.height(16.dp))

                    MetalButton(
                        text = stringResource(R.string.flip),
                        metal = metal,
                        enabled = !flipping,
                        onClick = { flip() },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(Modifier.height(20.dp))
                    StatsCard(stats, metal, headsLabel, tailsLabel)
                    Spacer(Modifier.height(12.dp))
                    HistoryCard(stats.history, metal, headsLabel, tailsLabel)
                }
            }
        }

        if (showSettings) {
            ModalBottomSheet(
                onDismissRequest = { showSettings = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = VaultColors.Surface,
            ) {
                val adNotReady = stringResource(R.string.ad_not_ready)
                val purchaseUnavailable = stringResource(R.string.purchase_unavailable)
                SettingsContent(
                    settings = settings,
                    onChange = vm::updateSettings,
                    rewardedReady = rewardedReady,
                    onUnlockSkin = { skin ->
                        val shown = monetization.showRewarded { vm.unlockAndSelect(skin) }
                        if (!shown) Toast.makeText(context, adNotReady, Toast.LENGTH_SHORT).show()
                    },
                    removeAdsPrice = removeAdsPrice,
                    onRemoveAds = {
                        if (!monetization.buyRemoveAds()) {
                            Toast.makeText(context, purchaseUnavailable, Toast.LENGTH_SHORT).show()
                        }
                    },
                    showPrivacyOptions = privacyOptionsRequired,
                    onPrivacyOptions = monetization::showPrivacyOptions,
                )
            }
        }

        if (showReset) {
            AlertDialog(
                onDismissRequest = { showReset = false },
                title = { Text(stringResource(R.string.reset_title)) },
                text = { Text(stringResource(R.string.reset_message)) },
                confirmButton = {
                    TextButton(onClick = {
                        vm.reset()
                        showReset = false
                    }) { Text(stringResource(R.string.reset)) }
                },
                dismissButton = {
                    TextButton(onClick = { showReset = false }) { Text(stringResource(R.string.cancel)) }
                },
            )
        }
    }
}

@Composable
private fun ResultText(flipping: Boolean, last: Side?, headsLabel: String, tailsLabel: String, shakeOn: Boolean) {
    val text = when {
        flipping -> stringResource(R.string.flipping)
        last == null -> stringResource(R.string.tap_to_flip)
        else -> stringResource(R.string.result, if (last == Side.HEADS) headsLabel else tailsLabel)
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineMedium,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            color = VaultColors.TextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (last == null && !flipping && shakeOn) {
            Text(stringResource(R.string.shake_hint), style = MaterialTheme.typography.bodySmall, color = VaultColors.TextSecondary)
        }
    }
}

@Composable
private fun MetalButton(text: String, metal: Metal, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(shape)
            .background(
                Brush.verticalGradient(listOf(metal.highlight, metal.light, metal.base, metal.dark)),
                shape,
                alpha = if (enabled) 1f else 0.4f,
            )
            .border(1.dp, metal.deep.copy(alpha = 0.6f), shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text.uppercase(),
            color = metal.deep,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            letterSpacing = 4.sp,
        )
    }
}

@Composable
private fun VaultCard(content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(VaultColors.Card)
            .border(1.dp, VaultColors.CardBorder, shape)
            .padding(16.dp),
    ) { content() }
}

@Composable
private fun StatsCard(stats: FlipStats, metal: Metal, headsLabel: String, tailsLabel: String) {
    VaultCard {
        Column {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Stat(headsLabel, stats.heads.toString(), stringResource(R.string.percent, stats.percent(Side.HEADS)), metal.light)
                Stat(tailsLabel, stats.tails.toString(), stringResource(R.string.percent, stats.percent(Side.TAILS)), VaultColors.TextPrimary)
                Stat(stringResource(R.string.total), stats.total.toString(), null, VaultColors.TextPrimary)
            }
            Spacer(Modifier.height(12.dp))
            // Heads vs tails split bar
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = 0.1f)),
            ) {
                if (stats.total > 0) {
                    Box(
                        Modifier
                            .fillMaxWidth(stats.heads / stats.total.toFloat())
                            .fillMaxHeight()
                            .background(Brush.horizontalGradient(listOf(metal.light, metal.base))),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            val streakText = stats.streakSide?.let {
                stringResource(R.string.streak_value, stats.streak, if (it == Side.HEADS) headsLabel else tailsLabel)
            } ?: "–"
            LabeledRow(stringResource(R.string.streak), streakText)
            LabeledRow(stringResource(R.string.longest_streak), stats.longestStreak.toString())
        }
    }
}

@Composable
private fun Stat(label: String, value: String, sub: String?, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = VaultColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = valueColor)
        if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall, color = VaultColors.TextSecondary)
    }
}

@Composable
private fun LabeledRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = VaultColors.TextSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = VaultColors.TextPrimary)
    }
}

@Composable
private fun HistoryCard(history: List<Side>, metal: Metal, headsLabel: String, tailsLabel: String) {
    VaultCard {
        Column {
            Text(stringResource(R.string.history), style = MaterialTheme.typography.labelLarge, color = VaultColors.TextSecondary)
            Spacer(Modifier.height(8.dp))
            if (history.isEmpty()) {
                Text(stringResource(R.string.no_history), style = MaterialTheme.typography.bodySmall, color = VaultColors.TextSecondary)
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(history) { side -> HistoryChip(side, metal, if (side == Side.HEADS) headsLabel else tailsLabel) }
                }
            }
        }
    }
}

@Composable
private fun HistoryChip(side: Side, metal: Metal, label: String) {
    val heads = side == Side.HEADS
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .then(
                if (heads) {
                    Modifier.background(Brush.linearGradient(listOf(metal.highlight, metal.base, metal.dark)))
                } else {
                    Modifier
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(1.5.dp, metal.base, CircleShape)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label.take(1).uppercase(),
            color = if (heads) metal.deep else metal.light,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
        )
    }
}
