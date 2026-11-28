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
import androidx.compose.ui.res.pluralStringResource
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
import com.afterhours.app.Avatar
import com.afterhours.app.GameViewModel
import com.afterhours.app.SetupState
import com.afterhours.app.ads.AdManager
import com.afterhours.app.ads.BannerAd
import com.afterhours.app.ads.ConsentManager
import com.afterhours.app.billing.BillingManager
import com.afterhours.app.data.Settings
import com.afterhours.app.data.ThemeMode
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
                    onPlay = { feedback.tap(); nav.navigate("players") },
                    onHowToPlay = { nav.navigate("tutorial") },
                    onSound = vm::setSound,
                    onHaptics = vm::setHaptics,
                    onChaos = vm::setChaos,
                    onTheme = vm::setTheme,
                    onTurnClock = vm::setTurnClock,
                    onResetSeen = vm::resetSeenCards,
                    onPrivacy = { consent.showPrivacyOptions(activity) },
                    onRemoveAds = { billing.buy(activity) },
                    onRestore = { billing.refresh() },
                )
            }
        }
        composable("players") {
            val s by vm.setup.collectAsStateWithLifecycle()
            WithBanner(showBanner) {
                PlayerCountScreen(
                    s, vm::setPlayerCount,
                    onNext = { feedback.tap(); nav.navigate("games") },
                    onBack = { nav.popBackStack() },
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
                    s, vm::setName, vm::selectRounds,
                    onAvatar = vm::setAvatar,
                    onStart = { if (vm.startGame()) { feedback.tap(); nav.navigate("game") } },
                    onBack = { nav.popBackStack() },
                )
            }
        }
        composable("game") {
            val g by vm.game.collectAsStateWithLifecycle()
            val state = g ?: return@composable
            LaunchedEffect(state.finished) {
                if (state.finished) {
                    feedback.alert()
                    nav.navigate("results") { popUpTo("game") { inclusive = true } }
                    // Natural break: the game just ended. Capped interstitial, never mid-card.
                    ads.onSessionCompleted(activity)
                }
            }
            val setupNow by vm.setup.collectAsStateWithLifecycle()
            if (!state.finished) CompositionLocalProvider(LocalAvatars provides setupNow.avatarsByName()) { GameScreen(
                state, feedback,
                turnClock = settings.turnClock,
                onComplete = { feedback.tap(); vm.complete() },
                onSkip = { feedback.tap(); vm.skip() },
                onTimeout = { feedback.alert(); vm.timeOut() },
                onDismissChaos = vm::dismissChaos,
                onReport = { reason ->
                    val card = state.card
                    if (card != null) sendReport(activity, card, reason)
                    vm.reportCurrentCard()
                },
                onQuit = { vm.endGame(); nav.popBackStack("home", false) },
            ) }
        }
        composable("results") {
            val g by vm.game.collectAsStateWithLifecycle()
            val state = g ?: return@composable
            val rewardedReady by ads.rewardedReady.collectAsStateWithLifecycle()
            val setupNow by vm.setup.collectAsStateWithLifecycle()
            val startBonus: () -> Unit = { if (vm.playBonus()) nav.navigate("game") { popUpTo("results") { inclusive = true } } }
            CompositionLocalProvider(LocalAvatars provides setupNow.avatarsByName()) { ResultsScreen(
                state,
                bonusLabel = when {
                    adsRemoved -> "Bonus game, +5 rounds"
                    rewardedReady -> "Watch an ad for a bonus game, +5 rounds"
                    else -> null
                },
                onBonus = { if (adsRemoved) startBonus() else ads.showRewarded(activity) { startBonus() } },
                onAgain = { vm.playAgain(); nav.navigate("game") { popUpTo("results") { inclusive = true } } },
                onHome = { vm.endGame(); nav.popBackStack("home", false) },
            ) }
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
    TutorialStep("1 / 5", "Choose your players", "Tap Play game and choose how many people are playing, from 2 to 8. Then type everyone's name. On each turn the phone goes to the person named at the top."),
    TutorialStep("2 / 5", "Pick your game", "Tap any game, then choose its difficulty.\nSpin the Wheel: the wheel picks who plays, then Truth or Dare.\nTruth, Dare, Would You Rather, Couple.\nNever Have I Ever, Most Likely To and Hot Seat for the whole group.\nCloser is for couples, so it opens only when 2 are playing."),
    TutorialStep("3 / 5", "Set the heat", "Chill is light. Bold adds cheekier cards. Wild includes everything. Bolder cards score more: 1, 2 or 3 points."),
    TutorialStep("4 / 5", "Skip if you want", "Not comfortable? Skip, no questions asked. In party games a skip costs one point; in Closer skipping is always free. Reporting a card never costs anything, and it never shows again."),
    TutorialStep("5 / 5", "Raise the stakes", "Complete cards in a row for streak bonuses. Skips cost a point, and the final rounds score double. Answers are on the clock, and chaos moments interrupt every few turns. Switch the clock or chaos off in Settings."),
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
    onHowToPlay: () -> Unit,
    onSound: (Boolean) -> Unit,
    onHaptics: (Boolean) -> Unit,
    onChaos: (Boolean) -> Unit,
    onTheme: (ThemeMode) -> Unit,
    onResetSeen: () -> Unit,
    onPrivacy: () -> Unit,
    onRemoveAds: () -> Unit,
    onRestore: () -> Unit,
    onTurnClock: (Boolean) -> Unit = {},
) {
    var showSettings by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    Page(horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(1f))
        Wordmark()
        Spacer(Modifier.height(20.dp))
        Muted("${games.size - 1} games for 2 to 8 adults.\nPlus Closer, just for couples.", textAlign = TextAlign.Center)
        Spacer(Modifier.weight(1f))
        PrimaryButton("Play game", onPlay, Modifier.fillMaxWidth())
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
        onSound, onHaptics, onChaos, onTheme, onResetSeen, onPrivacy, onRemoveAds, onRestore,
        onTurnClock = onTurnClock,
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
    onTheme: (ThemeMode) -> Unit,
    onResetSeen: () -> Unit,
    onPrivacy: () -> Unit,
    onRemoveAds: () -> Unit,
    onRestore: () -> Unit,
    onTurnClock: (Boolean) -> Unit,
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
                Text("Theme", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 8.dp))
                Segmented(ThemeMode.entries, settings.theme, { it.label }, onTheme)
                Text(
                    "Auto follows your phone's light or dark setting.",
                    style = MaterialTheme.typography.bodyMedium, color = Stone, modifier = Modifier.padding(top = 6.dp, bottom = 8.dp),
                )
                SwitchRow("Sound", settings.sound, onSound)
                SwitchRow("Vibration", settings.haptics, onHaptics)
                SwitchRow("Chaos moments", settings.chaos, onChaos, "Group events every few turns")
                SwitchRow("Turn clock", settings.turnClock, onTurnClock, "${TURN_CLOCK_SECONDS} seconds to answer, or it counts as a skip")
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
private data class GameEntry(val key: String, val title: String, val blurb: String, val howTo: String, val mode: GameMode?, val group: String, val icon: String)

private val games = listOf(
    GameEntry("spin", "Spin the Wheel", "The wheel decides", "Spin to pick who plays, then spin again for Truth or Dare. Everyone gets an even number of turns.", null, "Featured", "🎡"),
    GameEntry("truth", GameMode.TRUTH.label, "Answer honestly", "Read the question out loud and answer it truthfully. Skip if you'd rather not.", GameMode.TRUTH, "Classic", "💬"),
    GameEntry("dare", GameMode.DARE.label, "Do what the card says", "Do the dare in front of the group. Some dares come with a timer.", GameMode.DARE, "Classic", "🔥"),
    GameEntry("wyr", GameMode.WOULD_YOU_RATHER.label, "Choose, then defend it", "Pick one of the two options and convince the group you chose right.", GameMode.WOULD_YOU_RATHER, "Classic", "⚖️"),
    GameEntry("couple", GameMode.COUPLE.label, "For partners", "Questions and dares made for two people who know each other well.", GameMode.COUPLE, "Classic", "💞"),
    GameEntry("nhie", GameMode.NEVER_HAVE_I_EVER.label, "Hands up if you have", "Read the line out. Everyone who has done it raises a hand and the group can ask for the story.", GameMode.NEVER_HAVE_I_EVER, "Whole group", "✋"),
    GameEntry("mlt", GameMode.MOST_LIKELY_TO.label, "Point on three", "Read the question, count to three and everyone points at who fits best. Most votes explains.", GameMode.MOST_LIKELY_TO, "Whole group", "👉"),
    GameEntry("hot", GameMode.HOT_SEAT.label, "60 seconds of questions", "The named player sits in the hot seat while the group asks rapid-fire questions until the timer ends.", GameMode.HOT_SEAT, "Whole group", "⏱️"),
    GameEntry("closer", GameMode.CLOSER.label, "Just the two of you", "A romantic game for two partners. Each card shows the pose and tells you what to do. Ask before any touch, and skip anything at all.", GameMode.CLOSER, "For couples", "🌹"),
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
    var lockedNote by remember { mutableStateOf<String?>(null) }
    val pick: (GameMode?) -> Unit = { m ->
        when {
            m != null && !playableWith(m, s.playerCount) ->
                lockedNote = "${m.label} is for exactly ${PlayerRules.COUPLE_PLAYERS} players. Go back and choose ${PlayerRules.COUPLE_PLAYERS} to play it."
            m?.couplesOnly == true -> gateFor = m
            else -> onPick(m)
        }
    }
    gateFor?.let { m -> CouplesGate(onAgree = { gateFor = null; onPick(m) }, onCancel = { gateFor = null }) }
    Page {
        BackRow(onBack)
        Text("Games", style = MaterialTheme.typography.headlineMedium)
        Muted("${s.playerCount} players · pick a game", Modifier.padding(top = 4.dp, bottom = 12.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            games.groupBy { it.group }.forEach { (group, entries) ->
                Eyebrow(group, modifier = Modifier.padding(top = 6.dp))
                entries.chunked(2).forEach { pair ->
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        pair.forEach { g -> GameTile(
                            g, g == current, Modifier.weight(1f).fillMaxHeight(),
                            wide = pair.size == 1 && group == "Featured",
                            locked = g.mode != null && !playableWith(g.mode, s.playerCount),
                        ) { pick(g.mode) } }
                        if (pair.size == 1 && group != "Featured") Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
        lockedNote?.let { Muted(it, Modifier.padding(top = 8.dp)) }
    }
}

/** Couples-only games need exactly two players; every other game takes any count. */
private fun playableWith(mode: GameMode, count: Int) = !mode.couplesOnly || count == PlayerRules.COUPLE_PLAYERS

/** Step after Play game: how many people are playing. Big number with minus and plus. */
@Composable
fun PlayerCountScreen(s: SetupState, onCount: (Int) -> Unit, onNext: () -> Unit, onBack: () -> Unit) {
    val n = s.playerCount
    Page {
        BackRow(onBack)
        Text("How many players?", style = MaterialTheme.typography.headlineMedium)
        Muted("Pass one phone around. ${PlayerRules.MIN_PLAYERS} to ${PlayerRules.MAX_PLAYERS} people can play.", Modifier.padding(top = 4.dp))
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            CountButton("−", enabled = n > PlayerRules.MIN_PLAYERS) { onCount(n - 1) }
            Text(
                "$n", style = MaterialTheme.typography.displayMedium.copy(fontSize = 88.sp, lineHeight = 96.sp),
                color = Ivory, textAlign = TextAlign.Center, modifier = Modifier.width(140.dp),
            )
            CountButton("+", enabled = n < PlayerRules.MAX_PLAYERS) { onCount(n + 1) }
        }
        Eyebrow("players", Stone, Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(28.dp))
        Segmented((PlayerRules.MIN_PLAYERS..PlayerRules.MAX_PLAYERS).toList(), n, { "$it" }, onCount)
        Spacer(Modifier.height(12.dp))
        Muted(
            if (n == PlayerRules.COUPLE_PLAYERS) "Every game is open, including Closer for couples."
            else "Every game except Closer, which is just for 2.",
            Modifier.align(Alignment.CenterHorizontally), textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(1f))
        PrimaryButton("Next: choose a game", onNext, Modifier.fillMaxWidth())
    }
}

@Composable
private fun CountButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        Modifier.size(56.dp).clip(shape)
            .border(1.dp, if (enabled) HairlineStrong else Hairline, shape)
            .background(Graphite)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontSize = 28.sp, color = if (enabled) Champagne else Hairline)
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
private fun GameTile(g: GameEntry, selected: Boolean, modifier: Modifier, wide: Boolean, locked: Boolean = false, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier.graphicsLayer { alpha = if (locked) .45f else 1f }.clip(shape)
            .background(if (selected) GraphiteHigh else Graphite)
            .border(1.dp, if (selected || wide) Champagne.copy(alpha = if (selected) 1f else .5f) else Hairline, shape)
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        if (wide) { Eyebrow("New"); Spacer(Modifier.height(6.dp)) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(g.icon, fontSize = 22.sp)
            Spacer(Modifier.width(8.dp))
            Text(g.title, style = MaterialTheme.typography.titleLarge, color = Ivory)
        }
        Spacer(Modifier.height(4.dp))
        Text(g.blurb, style = MaterialTheme.typography.bodyMedium, color = Stone)
        if (locked) { Spacer(Modifier.height(6.dp)); Text("2 players only", style = MaterialTheme.typography.labelSmall, color = Champagne) }
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(g.icon, fontSize = 30.sp)
            Spacer(Modifier.width(10.dp))
            Text(g.title, style = MaterialTheme.typography.headlineMedium)
        }
        Spacer(Modifier.height(8.dp))
        Text(g.howTo, style = MaterialTheme.typography.bodyLarge, color = Stone)
        Spacer(Modifier.height(20.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Eyebrow("Difficulty")
            Intensity.entries.forEach { i ->
                OptionRow("${i.label}  ·  ${pointsLabel(i.points)}", intensityDescription(i), i == s.intensity) { onIntensity(i) }
            }
            Spacer(Modifier.height(12.dp))
            Eyebrow("Card language")
            Segmented(CardLanguage.entries, s.language, { it.label }, onLanguage)
        }
        Spacer(Modifier.height(16.dp))
        PrimaryButton("Add players", onNext, Modifier.fillMaxWidth())
    }
}

@Composable
fun SetupScreen(
    s: SetupState,
    onName: (Int, String) -> Unit,
    onRounds: (Int) -> Unit,
    onAvatar: (Int, Avatar?) -> Unit,
    onStart: () -> Unit,
    onBack: () -> Unit,
) {
    // Which player's picture is being chosen.
    var pickingFor by rememberSaveable { mutableIntStateOf(-1) }
    if (pickingFor in s.names.indices) {
        val i = pickingFor
        AvatarPicker(
            current = s.avatars[i],
            onPick = { onAvatar(i, it); pickingFor = -1 },
            onDismiss = { pickingFor = -1 },
        )
    }
    Page {
        BackRow(onBack)
        Text("Add players", style = MaterialTheme.typography.headlineMedium)
        val couples = !s.spin && s.mode.couplesOnly
        Muted(
            if (couples) "Just the two of you · enter both names" else "${s.playerCount} players · tap a picture to change it",
            Modifier.padding(top = 4.dp, bottom = 12.dp),
        )
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(s.names) { i, name ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = name, onValueChange = { onName(i, it) },
                        placeholder = { Text(if (couples) "Partner ${i + 1}" else "Player ${i + 1}", color = Stone) }, singleLine = true,
                        leadingIcon = {
                            // Tap to choose a profile picture. Until then: heart for partners, a person for everyone else.
                            AvatarBadge(
                                s.avatars[i], 36.dp, Modifier.padding(start = 6.dp),
                                partner = couples, highlighted = name.isNotBlank(),
                            ) { pickingFor = i }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Champagne, cursorColor = Champagne,
                            unfocusedBorderColor = Hairline,
                            unfocusedContainerColor = Graphite, focusedContainerColor = Graphite,
                        ),
                    )
                }
            }
            item {
                Spacer(Modifier.height(8.dp))
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
    turnClock: Boolean,
    onComplete: () -> Unit,
    onSkip: () -> Unit,
    onTimeout: () -> Unit,
    onDismissChaos: () -> Unit,
    onReport: (String) -> Unit,
    onQuit: () -> Unit,
) {
    var confirmQuit by remember { mutableStateOf(false) }
    var reporting by remember { mutableStateOf(false) }
    val card = g.card ?: return
    // The system back gesture must never silently throw away a game.
    BackHandler { confirmQuit = true }

    // Flip the card in for every new turn.
    val density = LocalDensity.current.density
    val flip = remember(g.round) { Animatable(90f) }
    // Spin games show two wheels before the card: 0 = who plays, 1 = truth or dare, 2 = card.
    var stage by rememberSaveable(g.round) { mutableIntStateOf(if (g.spin) 0 else 2) }
    LaunchedEffect(g.round, g.chaos == null, stage) {
        if (g.chaos == null && stage == 2) {
            flip.animateTo(0f, tween(420, easing = FastOutSlowInEasing))
            feedback.reveal()
        }
    }

    // The showdown starts with a jolt.
    LaunchedEffect(g.showdown) { if (g.showdown) feedback.alert() }
    val clockSeconds = if (turnClock && clocked(card)) TURN_CLOCK_SECONDS else null
    val clockPaused = g.chaos != null || stage < 2 || confirmQuit || reporting

    Page {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Eyebrow(if (g.showdown) "Showdown · double points" else "Round ${g.round} of ${g.totalRounds}", if (g.showdown) Wine else Stone)
            QuietButton("End game", { confirmQuit = true })
        }
        g.lastTurn?.let { TurnCallout(it, Modifier.padding(bottom = 6.dp)) }
        if (stage < 2) {
            SpinStage(g, stage, feedback, onNextStage = { stage++ })
            Spacer(Modifier.height(12.dp))
            Scoreline(g)
            return@Page
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            AvatarBadge(LocalAvatars.current[g.currentPlayer.name], 48.dp, highlighted = true)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(g.currentPlayer.name, style = MaterialTheme.typography.headlineMedium)
                val streak = g.currentPlayer.streak
                if (g.stakes && streak >= 1) {
                    Text(
                        "$streak in a row · next one +${Stakes.streakBonus(streak + 1)} bonus",
                        color = if (Stakes.streakBonus(streak + 1) > 0) Champagne else Stone,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else Muted("your turn")
            }
        }
        Spacer(Modifier.height(16.dp))
        Card(
            Modifier.fillMaxWidth().weight(1f).graphicsLayer {
                rotationY = flip.value
                cameraDistance = 14f * density
            },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Graphite),
            border = BorderStroke(if (g.showdown) 2.dp else 1.dp, if (g.showdown) Wine else Hairline),
        ) {
            Column(Modifier.fillMaxSize().padding(24.dp)) {
                clockSeconds?.let { secs ->
                    TurnClock(key = g.round, seconds = secs, paused = clockPaused, feedback = feedback, onTimeout = onTimeout)
                    Spacer(Modifier.height(12.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Tag(card.category.label, Ivory.copy(alpha = .8f))
                    Tag(card.intensity.label, intensityColor(card.intensity))
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
            SecondaryButton(if (g.skipCost > 0) "Skip  −${pointsLabel(g.skipCost)}" else "Skip", onSkip, Modifier.weight(1f))
            PrimaryButton("Done  +${pointsLabel(g.pointsOnOffer)}", onComplete, Modifier.weight(1.6f))
        }
        Spacer(Modifier.height(12.dp))
        Scoreline(g)
    }

    g.chaos?.let { event -> ChaosDialog(event, feedback, onDismissChaos) }

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
        while (left > 0) {
            delay(1000); left--
            if (left in 1..HEARTBEAT_SECONDS) feedback.heartbeat()
        }
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

/** Seconds the turn clock gives to answer a card. */
const val TURN_CLOCK_SECONDS = 30
/** The last seconds of any countdown pulse with a heartbeat. */
private const val HEARTBEAT_SECONDS = 5

/** Answer-style cards are on the clock. Dares, timed cards and couples cards are not: they take as long as they take. */
private fun clocked(card: com.afterhours.core.Card): Boolean =
    card.timerSeconds == null && card.category in setOf(
        GameMode.TRUTH, GameMode.WOULD_YOU_RATHER, GameMode.COUPLE, GameMode.NEVER_HAVE_I_EVER, GameMode.MOST_LIKELY_TO,
    )

/** Countdown that starts as soon as the card is shown. Running out counts as a skip. */
@Composable
private fun TurnClock(key: Int, seconds: Int, paused: Boolean, feedback: Feedback, onTimeout: () -> Unit) {
    var left by remember(key) { mutableIntStateOf(seconds) }
    val timeout by rememberUpdatedState(onTimeout)
    LaunchedEffect(key, paused) {
        if (paused) return@LaunchedEffect
        while (left > 0) {
            delay(1000); left--
            if (left in 1..HEARTBEAT_SECONDS) feedback.heartbeat()
        }
        timeout()
    }
    val urgent = left <= HEARTBEAT_SECONDS
    val pulse = remember(key) { Animatable(1f) }
    LaunchedEffect(left) {
        if (urgent && left > 0) { pulse.snapTo(1.15f); pulse.animateTo(1f, tween(400)) }
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        LinearProgressIndicator(
            progress = { left / seconds.toFloat() },
            modifier = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)),
            color = if (urgent) Wine else Champagne,
            trackColor = Hairline,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            "$left",
            color = if (urgent) Wine else Stone,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (urgent) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.graphicsLayer { scaleX = pulse.value; scaleY = pulse.value },
        )
    }
}

/** One line on how the previous turn went: points, streaks, penalties, a new leader. */
@Composable
private fun TurnCallout(t: TurnResult, modifier: Modifier = Modifier) {
    val text = when {
        t.timedOut -> "${t.playerName} ran out of time" + if (t.points < 0) "  ${t.points}" else ""
        t.points < 0 -> "${t.playerName} chickened out  ${t.points}"
        t.points == 0 -> "${t.playerName} skipped"
        t.tookLead -> "${t.playerName} takes the lead  +${t.points}"
        t.streak >= 3 -> "${t.playerName} is on fire, ${t.streak} in a row  +${t.points}"
        else -> "${t.playerName}  +${t.points}"
    }
    val color = when {
        t.points < 0 || t.timedOut -> Wine
        t.tookLead || t.streak >= 3 -> Champagne
        else -> Stone
    }
    Text(text, color = color, style = MaterialTheme.typography.labelLarge, modifier = modifier)
}

@Composable
private fun ChaosDialog(event: ChaosEvent, feedback: Feedback, onDismiss: () -> Unit) {
    LaunchedEffect(event.id) { feedback.alert() }
    AlertDialog(
        onDismissRequest = {},
        containerColor = GraphiteHigh,
        title = {
            Column {
                Eyebrow("Chaos moment", Wine)
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
        confirmButton = { TextButton(onDismiss) { Text("Continue", color = Champagne) } },
    )
}

@Composable
private fun Scoreline(g: GameState) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        g.players.forEachIndexed { i, p ->
            val active = i == g.currentPlayerIndex
            LocalAvatars.current[p.name]?.let { AvatarBadge(it, 22.dp, highlighted = active) }
            Text(
                "${p.name}  ${pointsLabel(p.score)}",
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
        g.players.maxByOrNull { it.bestStreak }?.takeIf { g.stakes && it.bestStreak >= 2 }?.let { p ->
            Muted("Longest streak: ${p.name}, ${p.bestStreak} in a row", Modifier.padding(top = 2.dp))
        }
        Spacer(Modifier.height(24.dp))
        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
            itemsIndexed(g.scoreboard) { i, p ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${i + 1}", style = MaterialTheme.typography.bodyLarge, color = Stone)
                        Spacer(Modifier.width(12.dp))
                        AvatarBadge(LocalAvatars.current[p.name], 32.dp, highlighted = i == 0 && w.isNotEmpty())
                        Spacer(Modifier.width(12.dp))
                        Text(p.name, style = MaterialTheme.typography.bodyLarge, color = if (i == 0 && w.isNotEmpty()) Champagne else Ivory)
                    }
                    Text(pointsLabel(p.score), style = MaterialTheme.typography.bodyLarge, color = if (i == 0 && w.isNotEmpty()) Champagne else Stone)
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

/** "1 Point", "2 Points" — from the `points` plurals resource so translations get correct plural forms. */
@Composable
private fun pointsLabel(n: Int): String = pluralStringResource(com.afterhours.app.R.plurals.points, n, n)
