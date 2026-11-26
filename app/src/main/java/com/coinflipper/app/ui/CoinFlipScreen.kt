package com.coinflipper.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coinflipper.app.CoinFlipViewModel
import com.coinflipper.app.R
import com.coinflipper.app.data.FlipStats
import com.coinflipper.app.data.Side
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val FLIP_DURATION_MS = 1400
private const val MIN_HALF_TURNS = 8

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinFlipScreen(vm: CoinFlipViewModel = viewModel()) {
    val stats by vm.stats.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    val rotation = remember { Animatable(if (stats.last == Side.TAILS) 180f else 0f) }
    val toss = remember { Animatable(0f) }
    var flipping by remember { mutableStateOf(false) }
    var showReset by rememberSaveable { mutableStateOf(false) }

    fun flip() {
        if (flipping) return
        flipping = true
        val result = vm.nextResult()
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

            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            vm.record(result)
            flipping = false
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    TextButton(onClick = { showReset = true }, enabled = !flipping && stats.total > 0) {
                        Text(stringResource(R.string.reset))
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
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(96.dp)) // headroom for the toss arc

            val faceName = stringResource(if (stats.last == Side.TAILS) R.string.tails else R.string.heads)
            val coinDescription = stringResource(R.string.coin_description, faceName)
            CoinView(
                angle = rotation.value,
                toss = toss.value,
                modifier = Modifier
                    .size(184.dp)
                    .semantics { contentDescription = coinDescription }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClick = { flip() },
                    ),
            )

            Spacer(Modifier.height(24.dp))
            ResultText(flipping = flipping, last = stats.last)
            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { flip() },
                enabled = !flipping,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Text(stringResource(R.string.flip), style = MaterialTheme.typography.titleMedium)
            }

            Spacer(Modifier.height(24.dp))
            StatsCard(stats)
            Spacer(Modifier.height(12.dp))
            HistoryCard(stats.history)
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

@Composable
private fun ResultText(flipping: Boolean, last: Side?) {
    val text = when {
        flipping -> stringResource(R.string.flipping)
        last == null -> stringResource(R.string.tap_to_flip)
        else -> stringResource(R.string.result, stringResource(if (last == Side.HEADS) R.string.heads else R.string.tails))
    }
    Text(
        text = text,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun StatsCard(stats: FlipStats) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Stat(stringResource(R.string.heads), stats.heads.toString(), stringResource(R.string.percent, stats.percent(Side.HEADS)))
                Stat(stringResource(R.string.tails), stats.tails.toString(), stringResource(R.string.percent, stats.percent(Side.TAILS)))
                Stat(stringResource(R.string.total), stats.total.toString(), null)
            }
            Spacer(Modifier.height(12.dp))
            val streakText = stats.streakSide?.let {
                stringResource(R.string.streak_value, stats.streak, stringResource(if (it == Side.HEADS) R.string.heads else R.string.tails))
            } ?: "–"
            LabeledRow(stringResource(R.string.streak), streakText)
            LabeledRow(stringResource(R.string.longest_streak), stats.longestStreak.toString())
        }
    }
}

@Composable
private fun Stat(label: String, value: String, sub: String?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun LabeledRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun HistoryCard(history: List<Side>) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.history), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            if (history.isEmpty()) {
                Text(stringResource(R.string.no_history), style = MaterialTheme.typography.bodySmall)
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    itemsIndexed(history) { _, side -> HistoryChip(side) }
                }
            }
        }
    }
}

@Composable
private fun HistoryChip(side: Side) {
    val heads = side == Side.HEADS
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(if (heads) Color(0xFFF5C542) else Color(0xFFC9CED6), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            stringResource(if (heads) R.string.heads_short else R.string.tails_short),
            color = if (heads) Color(0xFF6B4E00) else Color(0xFF3A4048),
            fontWeight = FontWeight.Bold,
        )
    }
}
