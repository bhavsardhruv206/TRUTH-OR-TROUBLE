package com.afterhours.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.afterhours.app.GameViewModel
import com.afterhours.core.*

@Composable
fun AfterHoursApp(vm: GameViewModel) {
    val age by vm.ageConfirmed.collectAsStateWithLifecycle()
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (age) {
            null -> Unit
            false -> AgeGateScreen(onConfirm = vm::confirmAge)
            true -> AppNav(vm)
        }
    }
}

@Composable
private fun AppNav(vm: GameViewModel) {
    val nav = rememberNavController()
    NavHost(nav, startDestination = "home") {
        composable("home") { HomeScreen(onPlay = { nav.navigate("mode") }) }
        composable("mode") {
            val s by vm.setup.collectAsStateWithLifecycle()
            ModeScreen(s, vm::selectMode, vm::selectIntensity, onNext = { nav.navigate("setup") }, onBack = { nav.popBackStack() })
        }
        composable("setup") {
            val s by vm.setup.collectAsStateWithLifecycle()
            SetupScreen(
                s, vm::setName, vm::addPlayer, vm::removePlayer, vm::selectRounds,
                onStart = { if (vm.startGame()) nav.navigate("game") },
                onBack = { nav.popBackStack() },
            )
        }
        composable("game") {
            val g by vm.game.collectAsStateWithLifecycle()
            val state = g
            if (state == null) return@composable
            LaunchedEffect(state.finished) {
                if (state.finished) nav.navigate("results") { popUpTo("game") { inclusive = true } }
            }
            if (!state.finished) GameScreen(state, vm::complete, vm::skip, onQuit = {
                vm.endGame(); nav.popBackStack("home", false)
            })
        }
        composable("results") {
            val g by vm.game.collectAsStateWithLifecycle()
            val state = g ?: return@composable
            ResultsScreen(
                state,
                onAgain = { vm.playAgain(); nav.navigate("game") { popUpTo("results") { inclusive = true } } },
                onHome = { vm.endGame(); nav.popBackStack("home", false) },
            )
        }
    }
}

@Composable
private fun Page(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp),
        content = content,
    )
}

@Composable
private fun Logo() {
    Text(
        "AfterHours", fontSize = 40.sp, fontWeight = FontWeight.ExtraBold,
        style = MaterialTheme.typography.displayMedium.copy(
            brush = Brush.horizontalGradient(listOf(Magenta, Amber)),
        ),
    )
}

@Composable
fun AgeGateScreen(onConfirm: () -> Unit) {
    var checked by remember { mutableStateOf(false) }
    Page(verticalArrangement = Arrangement.Center, modifier = Modifier) {
        Logo()
        Spacer(Modifier.height(16.dp))
        Text("This game is for adults only.", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "AfterHours contains cheeky questions and dares meant for people aged 18 and over. " +
                "You can skip anything, any time, with no penalty.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked, { checked = it })
            Text("I confirm that I am 18 years or older.")
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onConfirm, enabled = checked, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("Enter")
        }
    }
}

@Composable
fun HomeScreen(onPlay: () -> Unit) {
    var showHelp by remember { mutableStateOf(false) }
    Page(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier) {
        Spacer(Modifier.weight(1f))
        Logo()
        Text("Truth. Dare. Chaos.", style = MaterialTheme.typography.titleMedium, color = Amber)
        Spacer(Modifier.height(8.dp))
        Text("Pass the phone around. 2 to 8 players.", textAlign = TextAlign.Center)
        Spacer(Modifier.weight(1f))
        Button(onClick = onPlay, modifier = Modifier.fillMaxWidth().height(60.dp)) {
            Text("Play", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        TextButton(onClick = { showHelp = true }) { Text("Help & safety") }
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
                        label = { Text(label(item), Modifier.padding(vertical = 8.dp)) },
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
        TextButton(onBack) { Text("‹ Back") }
        Text("Choose a mode", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        ChoiceChips(GameMode.entries, s.mode, { it.label }, onMode)
        Spacer(Modifier.height(24.dp))
        Text("Intensity", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        ChoiceChips(Intensity.entries, s.intensity, { it.label }, onIntensity)
        Text(
            when (s.intensity) {
                Intensity.CHILL -> "Light and silly."
                Intensity.BOLD -> "Chill plus cheekier cards."
                Intensity.WILD -> "Everything, including the boldest cards."
            },
            Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = .7f),
        )
        Spacer(Modifier.weight(1f))
        Button(onNext, Modifier.fillMaxWidth().height(52.dp)) { Text("Next") }
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
        TextButton(onBack) { Text("‹ Back") }
        Text("Who's playing?", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("${s.names.size} players (2 to 8)", color = MaterialTheme.colorScheme.onSurface.copy(alpha = .7f))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(s.names) { i, name ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = name, onValueChange = { onName(i, it) },
                        label = { Text("Player ${i + 1}") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1f),
                    )
                    if (s.names.size > PlayerRules.MIN_PLAYERS) TextButton({ onRemove(i) }) { Text("✕") }
                }
            }
            item {
                if (s.names.size < PlayerRules.MAX_PLAYERS) OutlinedButton(onAdd) { Text("+ Add player") }
                Spacer(Modifier.height(16.dp))
                Text("Rounds", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                ChoiceChips(GameSession.ROUND_OPTIONS, s.rounds, { "$it" }, onRounds)
            }
        }
        s.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 8.dp)) }
        Button(onStart, Modifier.fillMaxWidth().height(52.dp)) { Text("Start game") }
    }
}

@Composable
fun GameScreen(g: GameState, onComplete: () -> Unit, onSkip: () -> Unit, onQuit: () -> Unit) {
    var confirmQuit by remember { mutableStateOf(false) }
    val card = g.card ?: return
    Page {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Round ${g.round} / ${g.totalRounds}", color = Amber, fontWeight = FontWeight.Bold)
            TextButton({ confirmQuit = true }) { Text("Quit") }
        }
        Text("${g.currentPlayer.name}'s turn", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Card(
            Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(2.dp, Brush.linearGradient(listOf(Magenta, Violet))),
        ) {
            Column(
                Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("${card.category.label.uppercase()} · ${card.intensity.label}", color = Amber, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(20.dp))
                Text(card.text, fontSize = 26.sp, lineHeight = 34.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(onComplete, Modifier.fillMaxWidth().height(56.dp)) { Text("Done  +${card.intensity.points}") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onSkip, Modifier.fillMaxWidth().height(48.dp)) { Text("Skip (no penalty)") }
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
                color = if (active) Magenta else MaterialTheme.colorScheme.onSurface.copy(alpha = .7f),
                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
}

@Composable
fun ResultsScreen(g: GameState, onAgain: () -> Unit, onHome: () -> Unit) {
    Page(horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(24.dp))
        Text("Game over", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        val w = g.winners
        Text(
            if (w.isEmpty()) "Nobody scored. Maximum chaos." else "🏆 ${w.joinToString(" & ") { it.name }}",
            fontSize = 24.sp, color = Amber, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 12.dp),
        )
        Text("${g.skips} skipped", color = MaterialTheme.colorScheme.onSurface.copy(alpha = .7f))
        Spacer(Modifier.height(16.dp))
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(g.scoreboard) { i, p ->
                Row(
                    Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp)).padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("${i + 1}. ${p.name}", fontWeight = FontWeight.SemiBold)
                    Text("${p.score}", color = Amber, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(onAgain, Modifier.fillMaxWidth().height(52.dp)) { Text("Play again") }
        TextButton(onHome) { Text("Home") }
    }
}
