package com.afterhours.app.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.Card
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.afterhours.app.AppConfig
import com.afterhours.app.GameViewModel
import com.afterhours.app.SetupState
import com.afterhours.app.ads.AdManager
import com.afterhours.app.ads.BannerAd
import com.afterhours.app.ads.ConsentManager
import com.afterhours.app.billing.BillingManager
import com.afterhours.app.data.Settings
import com.afterhours.core.*
import kotlinx.coroutines.delay

@Composable
fun AfterHoursApp(vm: GameViewModel, ads: AdManager, consent: ConsentManager, billing: BillingManager) {
    val age by vm.ageConfirmed.collectAsStateWithLifecycle()
    val tutorial by vm.tutorialSeen.collectAsStateWithLifecycle()
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when {
            age == null || tutorial == null -> Unit
            age == false -> AgeGateScreen(onConfirm = vm::confirmAge)
            else -> AppNav(vm, ads, consent, billing, startWithTutorial = tutorial == false)
        }
    }
}

@Composable
private fun AppNav(vm: GameViewModel, ads: AdManager, consent: ConsentManager, billing: BillingManager, startWithTutorial: Boolean) {
    val nav = rememberNavController()
    val activity = LocalContext.current as Activity
    val settings by vm.settings.collectAsStateWithLifecycle()
    val adsRemoved by vm.adsRemoved.collectAsStateWithLifecycle()
    val adsReady by ads.ready.collectAsStateWithLifecycle()
    val feedback = rememberFeedback(settings.sound, settings.haptics)
    val speaker = rememberSpeaker()
    val showBanner = adsReady && !adsRemoved

    // Fixed at first composition: the start destination must not change once the graph exists.
    val start = remember { if (startWithTutorial) "tutorial" else "home" }

    NavHost(nav, startDestination = start) {
        composable("tutorial") {
            TutorialScreen(onDone = {
                vm.finishTutorial()
                if (nav.previousBackStackEntry != null) nav.popBackStack()
                else nav.navigate("home") { popUpTo("tutorial") { inclusive = true } }
            })
        }
        composable("home") {
            val privacyRequired by consent.privacyOptionsRequired.collectAsStateWithLifecycle()
            val price by billing.price.collectAsStateWithLifecycle()
            WithBanner(showBanner) {
                HomeScreen(
                    settings = settings,
                    adsRemoved = adsRemoved,
                    removeAdsPrice = price,
                    privacyRequired = privacyRequired,
                    onPlay = { feedback.tap(); nav.navigate("games") },
                    onSpin = { feedback.tap(); vm.selectSpin(); nav.navigate("difficulty") },
                    onHowToPlay = { nav.navigate("tutorial") },
                    onSound = vm::setSound,
                    onHaptics = vm::setHaptics,
                    onChaos = vm::setChaos,
                    onReadAloud = vm::setReadAloud,
                    onResetSeen = vm::resetSeenCards,
                    onPrivacy = { consent.showPrivacyOptions(activity) },
                    onRemoveAds = { billing.buy(activity) },
                    onRestore = { billing.refresh() },
                )
            }
        }
        composable("games") {
            val s by vm.setup.collectAsStateWithLifecycle()
            WithBanner(showBanner) {
                GamesScreen(
                    s,
                    onPick = { m -> feedback.tap(); if (m == null) vm.selectSpin() else vm.selectMode(m); nav.navigate("difficulty") },
                    onBack = { nav.popBackStack() },
                )
            }
        }
        composable("difficulty") {
            val s by vm.setup.collectAsStateWithLifecycle()
            WithBanner(showBanner) {
                DifficultyScreen(s, vm::selectIntensity, vm::selectLanguage, onNext = { nav.navigate("setup") }, onBack = { nav.popBackStack() })
            }
        }
        composable("setup") {
            val s by vm.setup.collectAsStateWithLifecycle()
            WithBanner(showBanner) {
                SetupScreen(
                    s, vm::setName, vm::addPlayer, vm::removePlayer, vm::selectRounds,
                    onStart = { if (vm.startGame()) { feedback.tap(); nav.navigate("game") } },
                    onBack = { nav.popBackStack() },
                )
            }
        }
        composable("game") {
            val g by vm.game.collectAsStateWithLifecycle()
            val s by vm.setup.collectAsStateWithLifecycle()
            val state = g ?: return@composable
            LaunchedEffect(state.finished) {
                if (state.finished) {
                    feedback.alert()
                    nav.navigate("results") { popUpTo("game") { inclusive = true } }
                    // Natural break: the game just ended. Capped interstitial, never mid-card.
                    ads.onSessionCompleted(activity)
                }
            }
            if (!state.finished) GameScreen(
                state, feedback,
                voice = CardVoice(speaker, s.language.code, settings.readAloud),
                onComplete = { feedback.tap(); vm.complete() },
                onSkip = { feedback.tap(); vm.skip() },
                onDismissChaos = vm::dismissChaos,
                onReport = { reason ->
                    val card = state.card
                    if (card != null) sendReport(activity, card, reason)
                    vm.reportCurrentCard()
                },
                onQuit = { vm.endGame(); nav.popBackStack("home", false) },
            )
        }
        composable("results") {
            val g by vm.game.collectAsStateWithLifecycle()
            val state = g ?: return@composable
            val rewardedReady by ads.rewardedReady.collectAsStateWithLifecycle()
            val startBonus: () -> Unit = { if (vm.playBonus()) nav.navigate("game") { popUpTo("results") { inclusive = true } } }
            ResultsScreen(
                state,
                bonusLabel = when {
                    adsRemoved -> "Bonus game, +5 rounds"
                    rewardedReady -> "Watch an ad for a bonus game, +5 rounds"
                    else -> null
                },
                onBonus = { if (adsRemoved) startBonus() else ads.showRewarded(activity) { startBonus() } },
                onAgain = { vm.playAgain(); nav.navigate("game") { popUpTo("results") { inclusive = true } } },
                onHome = { vm.endGame(); nav.popBackStack("home", false) },
            )
        }
    }
}

/** Opens the mail app with a pre-filled report when a support address is configured. */
private fun sendReport(activity: Activity, card: com.afterhours.core.Card, reason: String) {
    if (AppConfig.SUPPORT_EMAIL.isBlank()) return
    val body = "Card: ${card.id}\nReason: $reason\n\n${card.text}"
    val uri = Uri.parse(
        "mailto:${AppConfig.SUPPORT_EMAIL}?subject=" + Uri.encode("Card report: ${card.id}") + "&body=" + Uri.encode(body),
    )
    try {
        activity.startActivity(Intent(Intent.ACTION_SENDTO, uri))
    } catch (e: android.content.ActivityNotFoundException) {
        // No mail app: the card is still hidden locally.
    }
}

/** Content with an ad banner underneath. Only for non-gameplay screens. */
@Composable
private fun WithBanner(show: Boolean, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) { content() }
        if (show) BannerAd()
    }
}

// ---------- Building blocks ----------

@Composable
private fun Page(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier.fillMaxSize().background(BackdropBrush).statusBarsPadding().navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
        content = content,
    )
}

/** Serif wordmark with a hairline rule. */
@Composable
private fun Wordmark(modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Truth", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.onBackground)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(28.dp).height(1.dp).background(Champagne))
            Text(
                "OR", style = MaterialTheme.typography.labelSmall, color = Champagne,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            Box(Modifier.width(28.dp).height(1.dp).background(Champagne))
        }
        Text("Trouble", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
private fun Eyebrow(text: String, color: Color = Champagne, modifier: Modifier = Modifier) =
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = color, modifier = modifier)

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick, modifier.height(54.dp), enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Champagne, contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = GraphiteHigh, disabledContentColor = Stone,
        ),
    ) { Text(text.uppercase(), style = MaterialTheme.typography.labelLarge) }
}

@Composable
private fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick, modifier.height(50.dp), shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, HairlineStrong),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Ivory),
    ) { Text(text, style = MaterialTheme.typography.bodyLarge) }
}

@Composable
private fun QuietButton(text: String, onClick: () -> Unit, color: Color = Stone) =
    TextButton(onClick) { Text(text, color = color, style = MaterialTheme.typography.bodyMedium) }

@Composable
private fun Muted(text: String, modifier: Modifier = Modifier, textAlign: TextAlign? = null) =
    Text(text, modifier, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = textAlign, style = MaterialTheme.typography.bodyMedium)

@Composable
private fun Tag(text: String, color: Color) {
    Text(
        text.uppercase(), color = color, style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.border(1.dp, color.copy(alpha = .5f), RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
private fun BackRow(onBack: () -> Unit) {
    TextButton(onBack, contentPadding = PaddingValues(0.dp)) {
        Text("‹  Back", color = Stone, style = MaterialTheme.typography.bodyMedium)
    }
}

/** A selectable row: title, optional description, champagne edge when selected. */
@Composable
private fun OptionRow(title: String, description: String?, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape)
            .background(if (selected) GraphiteHigh else Graphite)
            .border(1.dp, if (selected) Champagne else Hairline, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Ivory)
            if (description != null) Text(description, style = MaterialTheme.typography.bodyMedium, color = Stone)
        }
        if (selected) Text("●", color = Champagne, fontSize = 10.sp)
    }
}

/** A compact row of equal segments, for short choices like rounds or language. */
@Composable
private fun <T> Segmented(items: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(Modifier.fillMaxWidth().clip(shape).border(1.dp, Hairline, shape).background(Graphite)) {
        items.forEach { item ->
            val on = item == selected
            Box(
                Modifier.weight(1f).background(if (on) GraphiteHigh else Color.Transparent)
                    .clickable { onSelect(item) }.padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label(item), color = if (on) Champagne else Stone, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

// ---------- Screens ----------

@Composable
fun AgeGateScreen(onConfirm: () -> Unit) {
    var checked by remember { mutableStateOf(false) }
    Page(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Wordmark()
        Spacer(Modifier.height(40.dp))
        Eyebrow("Adults only · 18+")
        Spacer(Modifier.height(12.dp))
        Text(
            "Cheeky questions and dares for grown-ups. Skip anything, any time, with no penalty.",
            style = MaterialTheme.typography.bodyLarge, color = Stone, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { checked = !checked }) {
            Checkbox(
                checked, { checked = it },
                colors = CheckboxDefaults.colors(checkedColor = Champagne, checkmarkColor = Charcoal, uncheckedColor = HairlineStrong),
            )
            Text("I am 18 years or older.", style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(Modifier.height(20.dp))
        PrimaryButton("Enter", onConfirm, Modifier.fillMaxWidth(), enabled = checked)
    }
}

private data class TutorialStep(val eyebrow: String, val title: String, val body: String)

private val tutorialSteps = listOf(
    TutorialStep("1 / 5", "Pass the phone", "Add 2 to 8 players. On each turn the phone goes to the person named at the top. Cards often call players by name."),
    TutorialStep("2 / 5", "Pick your game", "Open Games and tap any game, then choose its difficulty.\nSpin the Wheel: the wheel picks who plays, then Truth or Dare.\nTruth, Dare, Would You Rather, Couple.\nNever Have I Ever, Most Likely To and Hot Seat for the whole group."),
    TutorialStep("3 / 5", "Set the heat", "Chill is light. Bold adds cheekier cards. Wild includes everything. Bolder cards score more: 1, 2 or 3 points."),
    TutorialStep("4 / 5", "Skipping is free", "Not comfortable? Skip. No points lost, no questions asked. You can also report a card and it never shows again."),
    TutorialStep("5 / 5", "Expect chaos", "Every few turns a group moment interrupts the game. Some dares come with a timer. Switch chaos off in Settings if you prefer."),
)

@Composable
fun TutorialScreen(onDone: () -> Unit) {
    var index by rememberSaveable { mutableIntStateOf(0) }
    val step = tutorialSteps[index]
    val last = index == tutorialSteps.lastIndex
    Page {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { QuietButton("Skip", onDone) }
        Spacer(Modifier.weight(1f))
        Eyebrow(step.eyebrow)
        Spacer(Modifier.height(12.dp))
        Text(step.title, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Text(step.body, style = MaterialTheme.typography.bodyLarge, color = Stone)
        Spacer(Modifier.weight(1.4f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            tutorialSteps.indices.forEach { i ->
                Box(Modifier.weight(1f).height(2.dp).background(if (i <= index) Champagne else Hairline))
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (index > 0) SecondaryButton("Back", { index-- }, Modifier.weight(1f))
            PrimaryButton(if (last) "Start playing" else "Next", { if (last) onDone() else index++ }, Modifier.weight(1f))
        }
    }
}

@Composable
fun HomeScreen(
    settings: Settings,
    adsRemoved: Boolean,
    removeAdsPrice: String?,
    privacyRequired: Boolean,
    onPlay: () -> Unit,
    onSpin: () -> Unit,
    onHowToPlay: () -> Unit,
    onSound: (Boolean) -> Unit,
    onHaptics: (Boolean) -> Unit,
    onChaos: (Boolean) -> Unit,
    onReadAloud: (Boolean) -> Unit,
    onResetSeen: () -> Unit,
    onPrivacy: () -> Unit,
    onRemoveAds: () -> Unit,
    onRestore: () -> Unit,
) {
    var showSettings by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    Page(horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(1f))
        Wordmark()
        Spacer(Modifier.height(20.dp))
        Muted("${games.size - 1} games for 2 to 8 adults.\nPlus Closer, just for couples.", textAlign = TextAlign.Center)
        Spacer(Modifier.weight(1f))
        PrimaryButton("Games", onPlay, Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        SecondaryButton("Spin the wheel", onSpin, Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        SecondaryButton("How to play", onHowToPlay, Modifier.fillMaxWidth())
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            QuietButton("Settings", { showSettings = true })
            QuietButton("Help & safety", { showHelp = true })
        }
    }
    if (showSettings) SettingsDialog(
        settings, adsRemoved, removeAdsPrice, privacyRequired,
        onSound, onHaptics, onChaos, onReadAloud, onResetSeen, onPrivacy, onRemoveAds, onRestore,
        onDismiss = { showSettings = false },
    )
    if (showHelp) AlertDialog(
        onDismissRequest = { showHelp = false },
        containerColor = Graphite,
        confirmButton = { TextButton({ showHelp = false }) { Text("Close", color = Champagne) } },
        title = { Text("Help & safety", style = MaterialTheme.typography.titleLarge) },
        text = {
            Text(
                "• Skip any card, any time, with no penalty.\n" +
                    "• Never share private info, contact strangers, share intimate media, drink anything you don't want to, or do anything unsafe.\n" +
                    "• Closer is for two consenting adult partners. Ask before any touch or kiss, and either of you can stop at any time.\n" +
                    "• Use Report on a card to hide it for good.\n" +
                    "• Scores are just for fun and have no value.",
                style = MaterialTheme.typography.bodyMedium, color = Stone,
            )
        },
    )
}

@Composable
private fun SettingsDialog(
    settings: Settings,
    adsRemoved: Boolean,
    removeAdsPrice: String?,
    privacyRequired: Boolean,
    onSound: (Boolean) -> Unit,
    onHaptics: (Boolean) -> Unit,
    onChaos: (Boolean) -> Unit,
    onReadAloud: (Boolean) -> Unit,
    onResetSeen: () -> Unit,
    onPrivacy: () -> Unit,
    onRemoveAds: () -> Unit,
    onRestore: () -> Unit,
    onDismiss: () -> Unit,
) {
    var resetDone by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Graphite,
        confirmButton = { TextButton(onDismiss) { Text("Done", color = Champagne) } },
        title = { Text("Settings", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                SwitchRow("Sound effects", settings.sound, onSound, "Beeps on new cards and timers")
                SwitchRow("Read cards aloud", settings.readAloud, onReadAloud, "Or tap the speaker on any card")
                SwitchRow("Vibration", settings.haptics, onHaptics)
                SwitchRow("Chaos moments", settings.chaos, onChaos, "Group events every few turns")
                HorizontalDivider(Modifier.padding(vertical = 8.dp), color = Hairline)
                TextButton({ onResetSeen(); resetDone = true }, contentPadding = PaddingValues(0.dp)) {
                    Text(if (resetDone) "Played cards forgotten" else "Forget played cards", color = if (resetDone) Stone else Ivory)
                }
                if (privacyRequired) TextButton(onPrivacy, contentPadding = PaddingValues(0.dp)) { Text("Privacy choices", color = Ivory) }
                HorizontalDivider(Modifier.padding(vertical = 8.dp), color = Hairline)
                if (adsRemoved) {
                    Text("Ads removed. Thank you.", color = Champagne, style = MaterialTheme.typography.bodyMedium)
                } else {
                    TextButton(onRemoveAds, enabled = removeAdsPrice != null, contentPadding = PaddingValues(0.dp)) {
                        Text(
                            if (removeAdsPrice != null) "Remove ads · $removeAdsPrice" else "Remove ads (unavailable)",
                            color = if (removeAdsPrice != null) Champagne else Stone,
                        )
                    }
                    TextButton(onRestore, contentPadding = PaddingValues(0.dp)) { Text("Restore purchase", color = Stone) }
                }
            }
        },
    )
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit, hint: String? = null) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (hint != null) Text(hint, style = MaterialTheme.typography.bodyMedium, color = Stone)
        }
        Switch(
            checked, onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Charcoal, checkedTrackColor = Champagne,
                uncheckedThumbColor = Stone, uncheckedTrackColor = GraphiteHigh, uncheckedBorderColor = HairlineStrong,
            ),
        )
    }
}

/** One entry in the Games section. [mode] is null for Spin the Wheel, which mixes Truth and Dare. */
private data class GameEntry(val key: String, val title: String, val blurb: String, val howTo: String, val mode: GameMode?, val group: String)

private val games = listOf(
    GameEntry("spin", "Spin the Wheel", "The wheel decides", "Spin to pick who plays, then spin again for Truth or Dare. Everyone gets an even number of turns.", null, "Featured"),
    GameEntry("truth", GameMode.TRUTH.label, "Answer honestly", "Read the question out loud and answer it truthfully. Skip if you'd rather not.", GameMode.TRUTH, "Classic"),
    GameEntry("dare", GameMode.DARE.label, "Do what the card says", "Do the dare in front of the group. Some dares come with a timer.", GameMode.DARE, "Classic"),
    GameEntry("wyr", GameMode.WOULD_YOU_RATHER.label, "Choose, then defend it", "Pick one of the two options and convince the group you chose right.", GameMode.WOULD_YOU_RATHER, "Classic"),
    GameEntry("couple", GameMode.COUPLE.label, "For partners", "Questions and dares made for two people who know each other well.", GameMode.COUPLE, "Classic"),
    GameEntry("nhie", GameMode.NEVER_HAVE_I_EVER.label, "Hands up if you have", "Read the line out. Everyone who has done it raises a hand and the group can ask for the story.", GameMode.NEVER_HAVE_I_EVER, "Whole group"),
    GameEntry("mlt", GameMode.MOST_LIKELY_TO.label, "Point on three", "Read the question, count to three and everyone points at who fits best. Most votes explains.", GameMode.MOST_LIKELY_TO, "Whole group"),
    GameEntry("hot", GameMode.HOT_SEAT.label, "60 seconds of questions", "The named player sits in the hot seat while the group asks rapid-fire questions until the timer ends.", GameMode.HOT_SEAT, "Whole group"),
    GameEntry("closer", GameMode.CLOSER.label, "Just the two of you", "A romantic game for two partners. Each card shows the pose and tells you what to do. Ask before any touch, and skip anything at all.", GameMode.CLOSER, "For couples"),
)

private fun selectedGame(s: SetupState): GameEntry =
    if (s.spin) games.first() else games.firstOrNull { it.mode == s.mode } ?: games.first()

private fun intensityDescription(i: Intensity) = when (i) {
    Intensity.CHILL -> "Light and easy"
    Intensity.BOLD -> "Cheekier, more personal"
    Intensity.WILD -> "Everything, including the boldest"
}

/** The Games section: every game as its own tile. Tapping one opens its difficulty choice. */
@Composable
fun GamesScreen(s: SetupState, onPick: (GameMode?) -> Unit, onBack: () -> Unit) {
    val current = selectedGame(s)
    var gateFor by remember { mutableStateOf<GameMode?>(null) }
    val pick: (GameMode?) -> Unit = { m -> if (m?.couplesOnly == true) gateFor = m else onPick(m) }
    gateFor?.let { m -> CouplesGate(onAgree = { gateFor = null; onPick(m) }, onCancel = { gateFor = null }) }
    Page {
        BackRow(onBack)
        Text("Games", style = MaterialTheme.typography.headlineMedium)
        Muted("${games.size} games · pick one to play", Modifier.padding(top = 4.dp, bottom = 12.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            games.groupBy { it.group }.forEach { (group, entries) ->
                Eyebrow(group, modifier = Modifier.padding(top = 6.dp))
                entries.chunked(2).forEach { pair ->
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        pair.forEach { g -> GameTile(g, g == current, Modifier.weight(1f).fillMaxHeight(), wide = pair.size == 1 && group == "Featured") { pick(g.mode) } }
                        if (pair.size == 1 && group != "Featured") Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/** Short "for consenting couples" check before the couples-only game. */
@Composable
private fun CouplesGate(onAgree: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = Graphite,
        title = { Text("For consenting couples", style = MaterialTheme.typography.titleLarge) },
        text = {
            Text(
                "Closer is for two adult partners who both want to play.\n\n" +
                    "• Ask before any touch or kiss.\n" +
                    "• Either of you can skip a card or stop at any time, no reason needed.\n" +
                    "• Nothing here involves undressing. Keep it sweet and romantic.",
                style = MaterialTheme.typography.bodyMedium, color = Stone,
            )
        },
        confirmButton = { TextButton(onAgree) { Text("We're both in", color = Champagne) } },
        dismissButton = { TextButton(onCancel) { Text("Not now", color = Stone) } },
    )
}

@Composable
private fun GameTile(g: GameEntry, selected: Boolean, modifier: Modifier, wide: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier.clip(shape)
            .background(if (selected) GraphiteHigh else Graphite)
            .border(1.dp, if (selected || wide) Champagne.copy(alpha = if (selected) 1f else .5f) else Hairline, shape)
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        if (wide) { Eyebrow("New"); Spacer(Modifier.height(6.dp)) }
        Text(g.title, style = MaterialTheme.typography.titleLarge, color = Ivory)
        Spacer(Modifier.height(4.dp))
        Text(g.blurb, style = MaterialTheme.typography.bodyMedium, color = Stone)
    }
}

/** Difficulty and language for the chosen game, with a one-line how-to. */
@Composable
fun DifficultyScreen(
    s: SetupState,
    onIntensity: (Intensity) -> Unit,
    onLanguage: (CardLanguage) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    val g = selectedGame(s)
    Page {
        BackRow(onBack)
        Eyebrow(g.group)
        Text(g.title, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(g.howTo, style = MaterialTheme.typography.bodyLarge, color = Stone)
        Spacer(Modifier.height(20.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Eyebrow("Difficulty")
            Intensity.entries.forEach { i ->
                OptionRow("${i.label}  ·  ${i.points} pt", intensityDescription(i), i == s.intensity) { onIntensity(i) }
            }
            Spacer(Modifier.height(12.dp))
            Eyebrow("Card language")
            Segmented(CardLanguage.entries, s.language, { it.label }, onLanguage)
        }
        Spacer(Modifier.height(16.dp))
        PrimaryButton("Next: players", onNext, Modifier.fillMaxWidth())
    }
}

@Composable
fun SetupScreen(
    s: SetupState,
    onName: (Int, String) -> Unit,
    onAdd: () -> Unit,
    onRemove: (Int) -> Unit,
    onRounds: (Int) -> Unit,
    onStart: () -> Unit,
    onBack: () -> Unit,
) {
    Page {
        BackRow(onBack)
        Text("Who's playing?", style = MaterialTheme.typography.headlineMedium)
        val couples = !s.spin && s.mode.couplesOnly
        Muted(
            if (couples) "Just the two of you · enter both names" else "${s.names.size} players · names are saved for next time",
            Modifier.padding(top = 4.dp, bottom = 12.dp),
        )
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(s.names) { i, name ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = name, onValueChange = { onName(i, it) },
                        placeholder = { Text(if (couples) "Partner ${i + 1}" else "Player ${i + 1}", color = Stone) }, singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Champagne, cursorColor = Champagne,
                            unfocusedBorderColor = Hairline,
                            unfocusedContainerColor = Graphite, focusedContainerColor = Graphite,
                        ),
                    )
                    if (s.names.size > PlayerRules.MIN_PLAYERS) TextButton({ onRemove(i) }) { Text("Remove", color = Stone, style = MaterialTheme.typography.bodyMedium) }
                }
            }
            item {
                if (!couples && s.names.size < PlayerRules.MAX_PLAYERS) TextButton(onAdd, contentPadding = PaddingValues(0.dp)) {
                    Text("+  Add player", color = Champagne, style = MaterialTheme.typography.bodyLarge)
                }
                Spacer(Modifier.height(16.dp))
                Eyebrow("Rounds")
                Spacer(Modifier.height(8.dp))
                Segmented(GameSession.ROUND_OPTIONS, s.rounds, { "$it" }, onRounds)
            }
        }
        s.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 8.dp)) }
        PrimaryButton("Start game", onStart, Modifier.fillMaxWidth())
    }
}

private val reportReasons = listOf("Offensive or hurtful", "Unsafe to do", "Too explicit", "Doesn't make sense")

@Composable
fun GameScreen(
    g: GameState,
    feedback: Feedback,
    voice: CardVoice,
    onComplete: () -> Unit,
    onSkip: () -> Unit,
    onDismissChaos: () -> Unit,
    onReport: (String) -> Unit,
    onQuit: () -> Unit,
) {
    var confirmQuit by remember { mutableStateOf(false) }
    var reporting by remember { mutableStateOf(false) }
    val card = g.card ?: return
    // The system back gesture must never silently throw away a game.
    BackHandler { confirmQuit = true }

    // Never keep reading a card that is gone, or after leaving the game.
    DisposableEffect(Unit) { onDispose { voice.speaker.stop() } }
    LaunchedEffect(g.round) { voice.speaker.stop() }
    val spoken = buildString {
        append(g.currentPlayer.name).append(". ")
        PoseLibrary.get(card.pose)?.let { append(it.title).append(". ") }
        append(g.cardText ?: card.text)
    }
    val spokenKey = "card-${g.round}-${card.id}"

    // Flip the card in for every new turn.
    val density = LocalDensity.current.density
    val flip = remember(g.round) { Animatable(90f) }
    // Spin games show two wheels before the card: 0 = who plays, 1 = truth or dare, 2 = card.
    var stage by rememberSaveable(g.round) { mutableIntStateOf(if (g.spin) 0 else 2) }
    LaunchedEffect(g.round, g.chaos == null, stage) {
        if (g.chaos == null && stage == 2) {
            flip.animateTo(0f, tween(420, easing = FastOutSlowInEasing))
            feedback.reveal()
            if (voice.autoRead) voice.speaker.speak(spokenKey, spoken, voice.languageCode)
        }
    }

    Page {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Eyebrow("Round ${g.round} of ${g.totalRounds}", Stone)
            QuietButton("End game", { confirmQuit = true })
        }
        if (stage < 2) {
            SpinStage(g, stage, feedback, onNextStage = { stage++ })
            Spacer(Modifier.height(12.dp))
            Scoreline(g)
            return@Page
        }
        Text(g.currentPlayer.name, style = MaterialTheme.typography.headlineMedium)
        Muted("your turn")
        Spacer(Modifier.height(16.dp))
        Card(
            Modifier.fillMaxWidth().weight(1f).graphicsLayer {
                rotationY = flip.value
                cameraDistance = 14f * density
            },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Graphite),
            border = BorderStroke(1.dp, Hairline),
        ) {
            Column(Modifier.fillMaxSize().padding(24.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Tag(card.category.label, Ivory.copy(alpha = .8f))
                    Tag(card.intensity.label, intensityColor(card.intensity))
                    Spacer(Modifier.weight(1f))
                    SpeakerButton(voice, spokenKey, spoken)
                }
                Column(
                    Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center,
                ) {
                    PoseLibrary.get(card.pose)?.let { pose ->
                        PoseIllustration(pose, Modifier.padding(vertical = 8.dp))
                        Text(pose.title, style = MaterialTheme.typography.titleLarge, color = Champagne)
                        Text(pose.hint, style = MaterialTheme.typography.bodyMedium, color = Stone, modifier = Modifier.padding(top = 2.dp, bottom = 12.dp))
                        Eyebrow("What to do", Stone)
                        Spacer(Modifier.height(4.dp))
                    }
                    Text(
                        g.cardText ?: card.text,
                        style = MaterialTheme.typography.headlineSmall, color = Ivory,
                    )
                }
                card.timerSeconds?.let { secs -> DareTimer(key = g.round, seconds = secs, feedback = feedback) }
                TextButton({ reporting = true }, contentPadding = PaddingValues(0.dp)) {
                    Text("Report card", color = Stone.copy(alpha = .7f), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SecondaryButton("Skip", onSkip, Modifier.weight(1f))
            PrimaryButton("Done  +${card.intensity.points}", onComplete, Modifier.weight(1.6f))
        }
        Spacer(Modifier.height(12.dp))
        Scoreline(g)
    }

    g.chaos?.let { event -> ChaosDialog(event, feedback, voice, onDismissChaos) }

    if (confirmQuit) AlertDialog(
        onDismissRequest = { confirmQuit = false },
        containerColor = Graphite,
        title = { Text("End this game?", style = MaterialTheme.typography.titleLarge) },
        text = { Text("Scores will be lost.", color = Stone) },
        confirmButton = { TextButton(onQuit) { Text("End game", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton({ confirmQuit = false }) { Text("Keep playing", color = Champagne) } },
    )

    if (reporting) AlertDialog(
        onDismissRequest = { reporting = false },
        containerColor = Graphite,
        title = { Text("Report this card", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                Text("It will be skipped and never shown on this phone again.", color = Stone, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                reportReasons.forEach { reason ->
                    TextButton({ reporting = false; onReport(reason) }, contentPadding = PaddingValues(0.dp)) {
                        Text(reason, color = Ivory)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton({ reporting = false }) { Text("Cancel", color = Stone) } },
    )
}

/** The two wheels of a spin game: first who plays, then Truth or Dare. */
@Composable
private fun ColumnScope.SpinStage(g: GameState, stage: Int, feedback: Feedback, onNextStage: () -> Unit) {
    val card = g.card ?: return
    val choosingPlayer = stage == 0
    val labels = if (choosingPlayer) g.players.map { it.name } else GameSession.SPIN_MODES.map { it.label }
    val target = if (choosingPlayer) g.currentPlayerIndex else GameSession.SPIN_MODES.indexOf(card.category).coerceAtLeast(0)
    var spinKey by remember(g.round, stage) { mutableStateOf<Int?>(null) }
    var landed by remember(g.round, stage) { mutableStateOf(false) }

    Text(
        when {
            choosingPlayer && landed -> g.currentPlayer.name
            choosingPlayer -> "Who's next?"
            landed -> card.category.label
            else -> "${g.currentPlayer.name}, truth or dare?"
        },
        style = MaterialTheme.typography.headlineMedium,
        color = if (landed) Champagne else Ivory,
    )
    Muted(if (choosingPlayer) "Spin to pick a player" else "Spin to find out")
    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
        // key() gives each wheel its own rotation state so the second wheel starts at rest.
        key(stage) {
            SpinWheel(
                labels = labels,
                target = target,
                spinKey = spinKey,
                onTick = feedback::tick,
                onLanded = { landed = true; feedback.reveal() },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    Spacer(Modifier.height(16.dp))
    when {
        spinKey == null -> PrimaryButton("Spin", { feedback.tap(); spinKey = g.round * 10 + stage }, Modifier.fillMaxWidth())
        !landed -> PrimaryButton("Spinning…", {}, Modifier.fillMaxWidth(), enabled = false)
        else -> PrimaryButton(if (choosingPlayer) "Next: truth or dare" else "Show my card", { feedback.tap(); onNextStage() }, Modifier.fillMaxWidth())
    }
}

/** Optional countdown for timed dares. Starts on tap so the player can get ready. */
@Composable
private fun DareTimer(key: Int, seconds: Int, feedback: Feedback) {
    var running by remember(key) { mutableStateOf(false) }
    var left by remember(key) { mutableIntStateOf(seconds) }
    LaunchedEffect(key, running) {
        if (!running) return@LaunchedEffect
        while (left > 0) { delay(1000); left-- }
        running = false
        feedback.alert()
    }
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "%d:%02d".format(left / 60, left % 60),
            style = MaterialTheme.typography.titleLarge,
            color = if (left == 0) Wine else Champagne,
            modifier = Modifier.weight(1f),
        )
        when {
            left == 0 -> Text("Time", color = Wine, style = MaterialTheme.typography.labelLarge)
            !running -> TextButton({ running = true }) { Text("Start timer", color = Champagne) }
            else -> TextButton({ running = false }) { Text("Pause", color = Stone) }
        }
    }
}

@Composable
private fun ChaosDialog(event: ChaosEvent, feedback: Feedback, voice: CardVoice, onDismiss: () -> Unit) {
    val spoken = "${event.title}. ${event.text}"
    val spokenKey = "chaos-${event.id}"
    LaunchedEffect(event.id) {
        feedback.alert()
        if (voice.autoRead) voice.speaker.speak(spokenKey, spoken, voice.languageCode)
    }
    AlertDialog(
        onDismissRequest = {},
        containerColor = GraphiteHigh,
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Eyebrow("Chaos moment", Wine, Modifier.weight(1f))
                    SpeakerButton(voice, spokenKey, spoken)
                }
                Spacer(Modifier.height(6.dp))
                Text(event.title, style = MaterialTheme.typography.headlineSmall)
            }
        },
        text = {
            Column {
                Text(event.text, style = MaterialTheme.typography.bodyLarge, color = Ivory)
                event.timerSeconds?.let { DareTimer(key = event.id.hashCode(), seconds = it, feedback = feedback) }
            }
        },
        confirmButton = { TextButton({ voice.speaker.stop(); onDismiss() }) { Text("Continue", color = Champagne) } },
    )
}

@Composable
private fun Scoreline(g: GameState) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        g.players.forEachIndexed { i, p ->
            val active = i == g.currentPlayerIndex
            Text(
                "${p.name}  ${p.score}",
                color = if (active) Champagne else Stone,
                fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
fun ResultsScreen(g: GameState, bonusLabel: String?, onBonus: () -> Unit, onAgain: () -> Unit, onHome: () -> Unit) {
    Page(horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(32.dp))
        Eyebrow("Game over")
        val w = g.winners
        Text(
            if (w.isEmpty()) "Nobody scored" else w.joinToString(" & ") { it.name },
            style = MaterialTheme.typography.displayMedium.copy(fontSize = 36.sp, lineHeight = 42.sp),
            textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 12.dp),
        )
        Muted(if (w.isEmpty()) "Everyone played it safe." else if (w.size > 1) "Shared the win" else "takes the night", textAlign = TextAlign.Center)
        Muted("${g.skips} skipped", Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(24.dp))
        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
            itemsIndexed(g.scoreboard) { i, p ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("${i + 1}   ${p.name}", style = MaterialTheme.typography.bodyLarge, color = if (i == 0 && w.isNotEmpty()) Champagne else Ivory)
                    Text("${p.score}", style = MaterialTheme.typography.bodyLarge, color = if (i == 0 && w.isNotEmpty()) Champagne else Stone)
                }
                HorizontalDivider(color = Hairline)
            }
        }
        Spacer(Modifier.height(16.dp))
        PrimaryButton("Play again", onAgain, Modifier.fillMaxWidth())
        if (bonusLabel != null) {
            Spacer(Modifier.height(10.dp))
            SecondaryButton(bonusLabel, onBonus, Modifier.fillMaxWidth())
        }
        QuietButton("Home", onHome)
    }
}
