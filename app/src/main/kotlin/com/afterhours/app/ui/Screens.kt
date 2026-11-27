package com.afterhours.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.platform.LocalContext
import com.afterhours.app.GameViewModel
import com.afterhours.app.ads.AdManager
import com.afterhours.app.ads.BannerAd
import com.afterhours.core.*

@Composable
fun AfterHoursApp(vm: GameViewModel, ads: AdManager) {
    val age by vm.ageConfirmed.collectAsStateWithLifecycle()
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (age) {
            null -> Unit
            false -> AgeGateScreen(onConfirm = vm::confirmAge)
            true -> AppNav(vm, ads)
        }
    }
}

@Composable
private fun AppNav(vm: GameViewModel, ads: AdManager) {
    val nav = rememberNavController()
    val activity = LocalContext.current as android.app.Activity
    NavHost(nav, startDestination = "home") {
        composable("home") { WithBanner { HomeScreen(onPlay = { nav.navigate("mode") }) } }
        composable("mode") {
            val s by vm.setup.collectAsStateWithLifecycle()
            WithBanner {
                ModeScreen(s, vm::selectMode, vm::selectIntensity, onNext = { nav.navigate("setup") }, onBack = { nav.popBackStack() })
            }
        }
        composable("setup") {
            val s by vm.setup.collectAsStateWithLifecycle()
            WithBanner {
                SetupScreen(
                    s, vm::setName, vm::addPlayer, vm::removePlayer, vm::selectRounds,
                    onStart = { if (vm.startGame()) nav.navigate("game") },
                    onBack = { nav.popBackStack() },
                )
            }
        }
        composable("game") {
            val g by vm.game.collectAsStateWithLifecycle()
            val state = g
            if (state == null) return@composable
            LaunchedEffect(state.finished) {
                if (state.finished) {
                    nav.navigate("results") { popUpTo("game") { inclusive = true } }
                    // Natural break: the game just ended. Capped interstitial, never mid-card.
                    ads.onSessionCompleted(activity)
                }
            }
            if (!state.finished) GameScreen(state, vm::complete, vm::skip, onQuit = {
                vm.endGame(); nav.popBackStack("home", false)
            })
        }
        composable("results") {
            val g by vm.game.collectAsStateWithLifecycle()
            val state = g ?: return@composable
            val rewardedReady by ads.rewardedReady.collectAsStateWithLifecycle()
            ResultsScreen(
                state,
                onBonus = if (rewardedReady) ({
                    ads.showRewarded(activity) {
                        if (vm.playBonus()) nav.navigate("game") { popUpTo("results") { inclusive = true } }
                    }
                }) else null,
                onAgain = { vm.playAgain(); nav.navigate("game") { popUpTo("results") { inclusive = true } } },
                onHome = { vm.endGame(); nav.popBackStack("home", false) },
            )
        }
    }
}

/** Content with an ad banner underneath. Only for non-gameplay screens. */
@Composable
private fun WithBanner(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) { content() }
        BannerAd()
    }
}

@Composable
private fun Page(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier.fillMaxSize().background(NightBrush).statusBarsPadding().navigationBarsPadding().padding(24.dp),
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
        content = content,
    )
}

/** Two-tone wordmark: cyan "TRUTH", small "or", hot-gradient "TROUBLE". */
@Composable
private fun Logo(modifier: Modifier = Modifier, horizontalAlignment: Alignment.Horizontal = Alignment.Start) {
    Column(modifier, horizontalAlignment = horizontalAlignment) {
        Text("TRUTH", style = MaterialTheme.typography.displayMedium, color = Cyan)
        Text(
            "or", fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "TROUBLE",
            style = MaterialTheme.typography.displayMedium.copy(brush = HotBrush),
        )
    }
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, height: Int = 56) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier.height(height.dp).clip(shape)
            .background(if (enabled) HotBrush else Brush.horizontalGradient(listOf(PlumHigh, PlumHigh)))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text, style = MaterialTheme.typography.labelLarge, fontSize = 17.sp,
            color = if (enabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Muted(text: String, modifier: Modifier = Modifier, textAlign: TextAlign? = null) =
    Text(text, modifier, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = textAlign)

@Composable
private fun Pill(text: String, color: Color) {
    Text(
        text, color = color, style = MaterialTheme.typography.labelLarge, fontSize = 12.sp,
        modifier = Modifier.border(1.dp, color.copy(alpha = .7f), RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 5.dp),
    )
}

@Composable
fun AgeGateScreen(onConfirm: () -> Unit) {
    var checked by remember { mutableStateOf(false) }
    Page(verticalArrangement = Arrangement.Center, modifier = Modifier) {
        Logo()
        Spacer(Modifier.height(24.dp))
        Pill("18+ ONLY", Magenta)
        Spacer(Modifier.height(12.dp))
        Text("This game is for adults only.", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "Truth or Trouble contains cheeky questions and dares meant for people aged 18 and over. " +
                "You can skip anything, any time, with no penalty.",
            style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked, { checked = it },
                colors = CheckboxDefaults.colors(checkedColor = Magenta, uncheckedColor = MaterialTheme.colorScheme.outline),
            )
            Text("I confirm that I am 18 years or older.")
        }
        Spacer(Modifier.height(16.dp))
        PrimaryButton("ENTER", onConfirm, Modifier.fillMaxWidth(), enabled = checked)
    }
}

@Composable
fun HomeScreen(onPlay: () -> Unit) {
    var showHelp by remember { mutableStateOf(false) }
    Page(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier) {
        Spacer(Modifier.weight(1f))
        Logo(horizontalAlignment = Alignment.CenterHorizontally)
        Spacer(Modifier.height(20.dp))
        Text("Truth. Dare. Chaos.", style = MaterialTheme.typography.titleMedium, color = Amber)
        Spacer(Modifier.height(8.dp))
        Muted("Pass the phone around. 2 to 8 players.", textAlign = TextAlign.Center)
        Spacer(Modifier.weight(1f))
        PrimaryButton("PLAY", onPlay, Modifier.fillMaxWidth(), height = 64)
        TextButton(onClick = { showHelp = true }) { Text("Help & safety", color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
    if (showHelp) AlertDialog(
        onDismissRequest = { showHelp = false },
        confirmButton = { TextButton({ showHelp = false }) { Text("Got it") } },
        title = { Text("Help & safety") },
        text = {
            Text(
                "• Skip any card, any time, with no penalty.\n" +
                    "• Never share private info, contact strangers, share intimate media, drink anything you don't want to, or do anything unsafe.\n" +
                    "• Scores are just for fun and have no value.\n" +
                    "• Contact and in-app card reporting are coming in a later version."
            )
        },
    )
}

@Composable
private fun <T> ChoiceChips(items: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { item ->
                    FilterChip(
                        selected = item == selected,
                        onClick = { onSelect(item) },
                        label = { Text(label(item), Modifier.padding(vertical = 8.dp), fontWeight = FontWeight.SemiBold) },
                        shape = RoundedCornerShape(14.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = MaterialTheme.colorScheme.onSurface,
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = Color.White,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true, selected = item == selected,
                            borderColor = MaterialTheme.colorScheme.outlineVariant,
                            selectedBorderColor = Magenta, borderWidth = 1.dp, selectedBorderWidth = 2.dp,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
fun ModeScreen(
    s: com.afterhours.app.SetupState,
    onMode: (GameMode) -> Unit,
    onIntensity: (Intensity) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    Page {
        TextButton(onBack) { Text("‹ Back", color = Amber) }
        Text("Choose a mode", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        ChoiceChips(GameMode.entries, s.mode, { it.label }, onMode)
        Spacer(Modifier.height(24.dp))
        Text("INTENSITY", style = MaterialTheme.typography.titleMedium, color = Amber)
        Spacer(Modifier.height(8.dp))
        ChoiceChips(Intensity.entries, s.intensity, { it.label }, onIntensity)
        Text(
            when (s.intensity) {
                Intensity.CHILL -> "Light and silly."
                Intensity.BOLD -> "Chill plus cheekier cards."
                Intensity.WILD -> "Everything, including the boldest cards."
            },
            Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        PrimaryButton("NEXT", onNext, Modifier.fillMaxWidth())
    }
}

@Composable
fun SetupScreen(
    s: com.afterhours.app.SetupState,
    onName: (Int, String) -> Unit,
    onAdd: () -> Unit,
    onRemove: (Int) -> Unit,
    onRounds: (Int) -> Unit,
    onStart: () -> Unit,
    onBack: () -> Unit,
) {
    Page {
        TextButton(onBack) { Text("‹ Back", color = Amber) }
        Text("Who's playing?", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Muted("${s.names.size} players (2 to 8)", Modifier.padding(bottom = 8.dp))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(s.names) { i, name ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = name, onValueChange = { onName(i, it) },
                        label = { Text("Player ${i + 1}") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Magenta, focusedLabelColor = Magenta, cursorColor = Magenta,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                        ),
                    )
                    if (s.names.size > PlayerRules.MIN_PLAYERS) TextButton({ onRemove(i) }) { Text("✕") }
                }
            }
            item {
                if (s.names.size < PlayerRules.MAX_PLAYERS) OutlinedButton(onAdd, border = BorderStroke(1.dp, Cyan)) { Text("+ Add player", color = Cyan) }
                Spacer(Modifier.height(16.dp))
                Text("ROUNDS", style = MaterialTheme.typography.titleMedium, color = Amber)
                Spacer(Modifier.height(8.dp))
                ChoiceChips(GameSession.ROUND_OPTIONS, s.rounds, { "$it" }, onRounds)
            }
        }
        s.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 8.dp)) }
        PrimaryButton("START GAME", onStart, Modifier.fillMaxWidth())
    }
}

@Composable
fun GameScreen(g: GameState, onComplete: () -> Unit, onSkip: () -> Unit, onQuit: () -> Unit) {
    var confirmQuit by remember { mutableStateOf(false) }
    val card = g.card ?: return
    Page {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Pill("ROUND ${g.round} / ${g.totalRounds}", Amber)
            TextButton({ confirmQuit = true }) { Text("Quit", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Text("${g.currentPlayer.name}'s turn", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Card(
            Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(2.dp, NeonBrush),
        ) {
            Column(
                Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Pill("${card.category.label.uppercase()} · ${card.intensity.label.uppercase()}", Cyan)
                Spacer(Modifier.height(20.dp))
                Text(card.text, fontSize = 26.sp, lineHeight = 34.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(16.dp))
        PrimaryButton("DONE  +${card.intensity.points}", onComplete, Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onSkip, Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) { Text("Skip (no penalty)", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Spacer(Modifier.height(12.dp))
        Scoreline(g)
    }
    if (confirmQuit) AlertDialog(
        onDismissRequest = { confirmQuit = false },
        title = { Text("Quit this game?") },
        text = { Text("Scores will be lost.") },
        confirmButton = { TextButton(onQuit) { Text("Quit") } },
        dismissButton = { TextButton({ confirmQuit = false }) { Text("Keep playing") } },
    )
}

@Composable
private fun Scoreline(g: GameState) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        g.players.forEachIndexed { i, p ->
            val active = i == g.currentPlayerIndex
            Text(
                "${p.name} ${p.score}",
                color = if (active) Magenta else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
}

@Composable
fun ResultsScreen(g: GameState, onAgain: () -> Unit, onHome: () -> Unit, onBonus: (() -> Unit)? = null) {
    Page(horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(24.dp))
        Text("GAME OVER", style = MaterialTheme.typography.headlineMedium, color = Cyan)
        val w = g.winners
        Text(
            if (w.isEmpty()) "Nobody scored. Maximum chaos." else "🏆 ${w.joinToString(" & ") { it.name }}",
            fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Amber, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 12.dp),
        )
        Muted("${g.skips} skipped")
        Spacer(Modifier.height(16.dp))
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(g.scoreboard) { i, p ->
                Row(
                    Modifier.fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                        .border(if (i == 0) 2.dp else 1.dp, if (i == 0) Amber else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("${i + 1}. ${p.name}", fontWeight = FontWeight.SemiBold)
                    Text("${p.score}", color = Amber, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        PrimaryButton("PLAY AGAIN", onAgain, Modifier.fillMaxWidth())
        if (onBonus != null) OutlinedButton(onBonus, Modifier.fillMaxWidth().height(48.dp)) {
            Text("Watch an ad: bonus game +5 rounds")
        }
        TextButton(onHome) { Text("Home", color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
