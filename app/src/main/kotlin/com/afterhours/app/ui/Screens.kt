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
import com.afterhours.app.Avatar
import com.afterhours.app.GameViewModel
import com.afterhours.app.SetupError
import com.afterhours.app.SetupState
import com.afterhours.app.ads.AdManager
import com.afterhours.app.ads.BannerAd
import com.afterhours.app.ads.ConsentManager
import com.afterhours.app.billing.BillingManager
import com.afterhours.app.data.Settings
import com.afterhours.app.data.ThemeMode
import com.afterhours.core.*
import com.afterhours.core.i18n.EnglishStrings
import com.afterhours.core.i18n.UiStrings
import kotlinx.coroutines.delay

@Composable
fun AfterHoursApp(vm: GameViewModel, ads: AdManager, consent: ConsentManager, billing: BillingManager) {
    val age by vm.ageConfirmed.collectAsStateWithLifecycle()
    val tutorial by vm.tutorialSeen.collectAsStateWithLifecycle()
    val languageChosen by vm.languageChosen.collectAsStateWithLifecycle()
    val language by vm.appLanguage.collectAsStateWithLifecycle()
    CompositionLocalProvider(LocalStrings provides UiStrings.of(language)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when {
                age == null || tutorial == null || languageChosen == null -> Unit
                languageChosen == false -> LanguageScreen(initial = language, onChoose = vm::chooseLanguage)
                age == false -> AgeGateScreen(onConfirm = vm::confirmAge)
                else -> AppNav(vm, ads, consent, billing, startWithTutorial = tutorial == false)
            }
        }
    }
}

@Composable
private fun AppNav(vm: GameViewModel, ads: AdManager, consent: ConsentManager, billing: BillingManager, startWithTutorial: Boolean) {
    val nav = rememberNavController()
    val activity = LocalContext.current as Activity
    val S = LocalStrings.current
    val language by vm.appLanguage.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val adsRemoved by vm.adsRemoved.collectAsStateWithLifecycle()
    val adsReady by ads.ready.collectAsStateWithLifecycle()
    val feedback = rememberFeedback(settings.sound, settings.haptics)
    val speaker = rememberSpeaker()
    val showBanner = adsReady && !adsRemoved

    // First launch goes straight to Home so a group can start in one tap; How to play stays one tap away.
    val firstLaunch = remember { startWithTutorial }
    val start = "home"
    val tonight = remember {
        val now = java.util.Calendar.getInstance()
        // Calendar counts Sunday = 1; Tonight wants ISO, Monday = 1.
        val isoDay = (now.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7 + 1
        val players = vm.setup.value.let { st -> st.playerCount.takeIf { st.names.any { it.isNotBlank() } } }
        val day = (now.timeInMillis + now.get(java.util.Calendar.ZONE_OFFSET) + now.get(java.util.Calendar.DST_OFFSET)) / 86_400_000L
        Tonight.suggest(isoDay, now.get(java.util.Calendar.HOUR_OF_DAY), players, day)
    }

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
                    language = language,
                    adsRemoved = adsRemoved,
                    removeAdsPrice = price,
                    privacyRequired = privacyRequired,
                    tonight = tonight,
                    firstLaunch = firstLaunch,
                    onQuickStart = { g ->
                        feedback.tap()
                        if (vm.quickStart(g) { S.playerN(it) }) nav.navigate(routeFor(g))
                    },
                    onPlay = { feedback.tap(); nav.navigate("players") },
                    onHowToPlay = { nav.navigate("tutorial") },
                    onSound = vm::setSound,
                    onHaptics = vm::setHaptics,
                    onChaos = vm::setChaos,
                    onTheme = vm::setTheme,
                    onTurnClock = vm::setTurnClock,
                    onLanguage = vm::chooseLanguage,
                    onReadAloud = vm::setReadAloud,
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
            // No banner here: the 18+ Spicy section is on this screen, and ads never sit next to adult content.
            GamesScreen(
                s,
                spicyConfirmed = { vm.spicyConfirmed },
                onConfirmSpicy = vm::confirmSpicy,
                onPick = { g ->
                    feedback.tap()
                    vm.selectGame(g)
                    nav.navigate(if (g.quiz) "quiz" else "difficulty")
                },
                onBack = { nav.popBackStack() },
            )
        }
        composable("difficulty") {
            val s by vm.setup.collectAsStateWithLifecycle()
            WithBanner(showBanner && !s.adult) {
                DifficultyScreen(
                    s, vm::selectIntensity, vm::selectLanguage,
                    spicyConfirmed = { vm.spicyConfirmed },
                    onConfirmSpicy = vm::confirmSpicy,
                    onNext = {
                        if (s.mode == GameMode.COUPLE_BINGO && !s.spin) { vm.newBingo(); nav.navigate("bingo") } else nav.navigate("setup")
                    },
                    onBack = { nav.popBackStack() },
                )
            }
        }
        composable("bingo") {
            val board by vm.bingo.collectAsStateWithLifecycle()
            BingoScreen(board, vm::toggleBingo, onNewBoard = { feedback.tap(); vm.newBingo() }, onBack = { nav.popBackStack() })
        }
        composable("quiz") {
            QuizScreen(onBack = { nav.popBackStack() })
        }
        composable("setup") {
            val s by vm.setup.collectAsStateWithLifecycle()
            WithBanner(showBanner && !s.adult) {
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
            val s by vm.setup.collectAsStateWithLifecycle()
            val state = g ?: return@composable
            LaunchedEffect(state.finished) {
                if (state.finished) {
                    feedback.alert()
                    nav.navigate("results") { popUpTo("game") { inclusive = true } }
                    // Natural break: the game just ended. Capped interstitial, never mid-card,
                    // and never right after 18+ content.
                    if (!s.adult) ads.onSessionCompleted(activity)
                }
            }
            val setupNow by vm.setup.collectAsStateWithLifecycle()
            if (!state.finished) CompositionLocalProvider(LocalAvatars provides setupNow.avatarsByName()) { GameScreen(
                state, feedback,
                turnClock = settings.turnClock,
                voice = CardVoice(speaker, s.language.code, settings.readAloud),
                onComplete = { feedback.tap(); vm.complete() },
                onCompleteGuesses = { feedback.tap(); vm.completeGuesses(it) },
                onSkip = { feedback.tap(); vm.skip() },
                onTimeout = { feedback.alert(); vm.timeOut() },
                onDismissChaos = vm::dismissChaos,
                onReport = { reason ->
                    val card = state.card
                    // Reports go to the developer, so the reason is always sent in English.
                    if (card != null) sendReport(activity, card, EnglishStrings.reportReasons[reason])
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
                    adsRemoved -> S.bonusGame
                    // No ad offer after 18+ content.
                    rewardedReady && !setupNow.adult -> S.watchAdBonus
                    else -> null
                },
                onBonus = { if (adsRemoved) startBonus() else ads.showRewarded(activity) { startBonus() } },
                onAgain = { vm.playAgain(); nav.navigate("game") { popUpTo("results") { inclusive = true } } },
                onHome = { vm.endGame(); nav.popBackStack("home", false) },
            ) }
        }
    }
}

/** Where a one-tap start lands: the card game, or the board and quiz screens. */
private fun routeFor(g: GameId): String = when {
    g.quiz -> "quiz"
    g.mode == GameMode.COUPLE_BINGO -> "bingo"
    else -> "game"
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
internal fun Page(
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
internal fun Eyebrow(text: String, color: Color = Champagne, modifier: Modifier = Modifier) =
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = color, modifier = modifier)

@Composable
internal fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
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
internal fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick, modifier.height(50.dp), shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, HairlineStrong),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Ivory),
    ) { Text(text, style = MaterialTheme.typography.bodyLarge) }
}

@Composable
internal fun QuietButton(text: String, onClick: () -> Unit, color: Color = Stone) =
    TextButton(onClick) { Text(text, color = color, style = MaterialTheme.typography.bodyMedium) }

@Composable
internal fun Muted(text: String, modifier: Modifier = Modifier, textAlign: TextAlign? = null) =
    Text(text, modifier, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = textAlign, style = MaterialTheme.typography.bodyMedium)

@Composable
internal fun Tag(text: String, color: Color) {
    Text(
        text.uppercase(), color = color, style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.border(1.dp, color.copy(alpha = .5f), RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
internal fun BackRow(onBack: () -> Unit) {
    TextButton(onBack, contentPadding = PaddingValues(0.dp)) {
        Text("‹  ${LocalStrings.current.back}", color = Stone, style = MaterialTheme.typography.bodyMedium)
    }
}

/** A selectable row: title, optional description, champagne edge when selected. */
@Composable
internal fun OptionRow(title: String, description: String?, selected: Boolean, onClick: () -> Unit) {
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

/** A compact row of equal segments, for short choices like rounds or theme. */
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
    val S = LocalStrings.current
    var checked by remember { mutableStateOf(false) }
    Page(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Wordmark()
        Spacer(Modifier.height(40.dp))
        Eyebrow(S.adultsOnly)
        Spacer(Modifier.height(12.dp))
        Text(
            S.ageGateBody,
            style = MaterialTheme.typography.bodyLarge, color = Stone, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { checked = !checked }) {
            Checkbox(
                checked, { checked = it },
                colors = CheckboxDefaults.colors(checkedColor = Champagne, checkmarkColor = Charcoal, uncheckedColor = HairlineStrong),
            )
            Text(S.ageConfirm, style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(Modifier.height(20.dp))
        PrimaryButton(S.enter, onConfirm, Modifier.fillMaxWidth(), enabled = checked)
    }
}

@Composable
fun TutorialScreen(onDone: () -> Unit) {
    val S = LocalStrings.current
    val steps = S.tutorial
    var index by rememberSaveable { mutableIntStateOf(0) }
    val step = steps[index]
    val last = index == steps.lastIndex
    Page {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { QuietButton(S.skip, onDone) }
        Spacer(Modifier.weight(1f))
        Eyebrow("${index + 1} / ${steps.size}")
        Spacer(Modifier.height(12.dp))
        Text(step.title, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Text(step.body, style = MaterialTheme.typography.bodyLarge, color = Stone)
        Spacer(Modifier.weight(1.4f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            steps.indices.forEach { i ->
                Box(Modifier.weight(1f).height(2.dp).background(if (i <= index) Champagne else Hairline))
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (index > 0) SecondaryButton(S.back, { index-- }, Modifier.weight(1f))
            PrimaryButton(if (last) S.startPlaying else S.next, { if (last) onDone() else index++ }, Modifier.weight(1f))
        }
    }
}

@Composable
fun HomeScreen(
    settings: Settings,
    language: CardLanguage,
    adsRemoved: Boolean,
    removeAdsPrice: String?,
    privacyRequired: Boolean,
    tonight: List<GameId>,
    firstLaunch: Boolean,
    onQuickStart: (GameId) -> Unit,
    onPlay: () -> Unit,
    onHowToPlay: () -> Unit,
    onSound: (Boolean) -> Unit,
    onHaptics: (Boolean) -> Unit,
    onChaos: (Boolean) -> Unit,
    onTheme: (ThemeMode) -> Unit,
    onLanguage: (CardLanguage) -> Unit,
    onReadAloud: (Boolean) -> Unit,
    onResetSeen: () -> Unit,
    onPrivacy: () -> Unit,
    onRemoveAds: () -> Unit,
    onRestore: () -> Unit,
    onTurnClock: (Boolean) -> Unit = {},
) {
    val S = LocalStrings.current
    var showSettings by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    Page(horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(.6f))
        Wordmark()
        Spacer(Modifier.height(16.dp))
        Muted(S.homeTagline(GameId.entries.count { !it.spicy && !it.couplesOnly }), textAlign = TextAlign.Center)
        Spacer(Modifier.weight(1f))
        // One tap starts a game: no player count, names or pictures needed first.
        TonightRow(tonight, onQuickStart)
        Spacer(Modifier.height(16.dp))
        PrimaryButton(S.playGame, onPlay, Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        SecondaryButton(if (firstLaunch) S.newHere else S.howToPlay, onHowToPlay, Modifier.fillMaxWidth())
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            QuietButton(S.settings, { showSettings = true })
            QuietButton(S.helpSafety, { showHelp = true })
            if (!adsRemoved && removeAdsPrice != null) QuietButton(S.removeAds(removeAdsPrice), onRemoveAds, Champagne)
        }
    }
    if (showSettings) SettingsDialog(
        settings, language, adsRemoved, removeAdsPrice, privacyRequired,
        onSound, onHaptics, onChaos, onTheme, onLanguage, onReadAloud, onResetSeen, onPrivacy, onRemoveAds, onRestore,
        onTurnClock = onTurnClock,
        onDismiss = { showSettings = false },
    )
    if (showHelp) AlertDialog(
        onDismissRequest = { showHelp = false },
        containerColor = Graphite,
        confirmButton = { TextButton({ showHelp = false }) { Text(S.close, color = Champagne) } },
        title = { Text(S.helpSafety, style = MaterialTheme.typography.titleLarge) },
        text = {
            Text(
                S.helpBody,
                style = MaterialTheme.typography.bodyMedium, color = Stone,
            )
        },
    )
}

@Composable
private fun SettingsDialog(
    settings: Settings,
    language: CardLanguage,
    adsRemoved: Boolean,
    removeAdsPrice: String?,
    privacyRequired: Boolean,
    onSound: (Boolean) -> Unit,
    onHaptics: (Boolean) -> Unit,
    onChaos: (Boolean) -> Unit,
    onTheme: (ThemeMode) -> Unit,
    onLanguage: (CardLanguage) -> Unit,
    onReadAloud: (Boolean) -> Unit,
    onResetSeen: () -> Unit,
    onPrivacy: () -> Unit,
    onRemoveAds: () -> Unit,
    onRestore: () -> Unit,
    onTurnClock: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val S = LocalStrings.current
    var resetDone by remember { mutableStateOf(false) }
    var pickLanguage by remember { mutableStateOf(false) }
    // One dialog at a time: the language list replaces Settings while it is open.
    if (pickLanguage) {
        LanguageDialog(language, onPick = { onLanguage(it); pickLanguage = false }, onDismiss = { pickLanguage = false })
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Graphite,
        confirmButton = { TextButton(onDismiss) { Text(S.done, color = Champagne) } },
        title = { Text(S.settings, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                LanguageRow(language) { pickLanguage = true }
                Text(S.theme, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 8.dp))
                Segmented(ThemeMode.entries, settings.theme, { S.themeLabel(it) }, onTheme)
                Text(
                    S.themeHint,
                    style = MaterialTheme.typography.bodyMedium, color = Stone, modifier = Modifier.padding(top = 6.dp, bottom = 8.dp),
                )
                SwitchRow(S.sound, settings.sound, onSound, S.soundHint)
                SwitchRow(S.readAloud, settings.readAloud, onReadAloud, S.readAloudHint)
                SwitchRow(S.vibration, settings.haptics, onHaptics)
                SwitchRow(S.chaosMoments, settings.chaos, onChaos, S.chaosHint)
                SwitchRow(S.turnClock, settings.turnClock, onTurnClock, S.turnClockHint(TURN_CLOCK_SECONDS))
                HorizontalDivider(Modifier.padding(vertical = 8.dp), color = Hairline)
                TextButton({ onResetSeen(); resetDone = true }, contentPadding = PaddingValues(0.dp)) {
                    Text(if (resetDone) S.playedForgotten else S.forgetPlayed, color = if (resetDone) Stone else Ivory)
                }
                if (privacyRequired) TextButton(onPrivacy, contentPadding = PaddingValues(0.dp)) { Text(S.privacyChoices, color = Ivory) }
                HorizontalDivider(Modifier.padding(vertical = 8.dp), color = Hairline)
                if (adsRemoved) {
                    Text(S.adsRemoved, color = Champagne, style = MaterialTheme.typography.bodyMedium)
                } else {
                    TextButton(onRemoveAds, enabled = removeAdsPrice != null, contentPadding = PaddingValues(0.dp)) {
                        Text(
                            if (removeAdsPrice != null) S.removeAds(removeAdsPrice) else S.removeAdsUnavailable,
                            color = if (removeAdsPrice != null) Champagne else Stone,
                        )
                    }
                    TextButton(onRestore, contentPadding = PaddingValues(0.dp)) { Text(S.restorePurchase, color = Stone) }
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

/** First launch: pick the language for menus and cards. The screen switches as you tap. */
@Composable
fun LanguageScreen(initial: CardLanguage, onChoose: (CardLanguage) -> Unit) {
    var selected by rememberSaveable { mutableStateOf(initial) }
    val S = UiStrings.of(selected)
    CompositionLocalProvider(LocalStrings provides S) {
        Page {
            Spacer(Modifier.height(12.dp))
            Wordmark(Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(28.dp))
            Text(S.languageTitle, style = MaterialTheme.typography.headlineMedium)
            Muted(S.languageBody, Modifier.padding(top = 4.dp, bottom = 16.dp))
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LanguageOptions(selected) { selected = it }
            }
            Spacer(Modifier.height(16.dp))
            PrimaryButton(S.continueLabel, { onChoose(selected) }, Modifier.fillMaxWidth())
        }
    }
}

/** Each language in its own script, with its English name underneath for anyone sharing the phone. */
@Composable
private fun LanguageOptions(selected: CardLanguage, onPick: (CardLanguage) -> Unit) {
    CardLanguage.entries.forEach { l ->
        OptionRow(l.label, l.englishName.takeIf { it != l.label }, l == selected) { onPick(l) }
    }
}

/** The Language line in Settings, showing the current choice. */
@Composable
private fun LanguageRow(language: CardLanguage, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(LocalStrings.current.language, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text("${language.label}  ›", color = Champagne, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun LanguageDialog(current: CardLanguage, onPick: (CardLanguage) -> Unit, onDismiss: () -> Unit) {
    val S = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Graphite,
        title = { Text(S.language, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LanguageOptions(current, onPick)
            }
        },
        confirmButton = { TextButton(onDismiss) { Text(S.cancel, color = Stone) } },
    )
}

/** The Games screen: every game in its mood section. Tapping one opens its difficulty choice. */
@Composable
fun GamesScreen(
    s: SetupState,
    spicyConfirmed: () -> Boolean,
    onConfirmSpicy: () -> Unit,
    onPick: (GameId) -> Unit,
    onBack: () -> Unit,
) {
    val S = LocalStrings.current
    var couplesGateFor by remember { mutableStateOf<GameId?>(null) }
    var spicyGateFor by remember { mutableStateOf<GameId?>(null) }
    var lockedNote by remember { mutableStateOf<String?>(null) }
    // Closer asks for the couples check; 18+ games ask everyone to confirm they're adults.
    val afterSpicy: (GameId) -> Unit = { g -> if (g.mode == GameMode.CLOSER) couplesGateFor = g else onPick(g) }
    val pick: (GameId) -> Unit = { g ->
        when {
            !GameId.playableWith(g, s.playerCount) -> lockedNote = "${S.title(g)} · ${S.twoPlayersOnly}"
            g.spicy && !spicyConfirmed() -> spicyGateFor = g
            else -> afterSpicy(g)
        }
    }
    spicyGateFor?.let { g -> SpicyGate(onAgree = { spicyGateFor = null; onConfirmSpicy(); afterSpicy(g) }, onCancel = { spicyGateFor = null }) }
    couplesGateFor?.let { g -> CouplesGate(onAgree = { couplesGateFor = null; onPick(g) }, onCancel = { couplesGateFor = null }) }
    Page {
        BackRow(onBack)
        Text(S.games, style = MaterialTheme.typography.headlineMedium)
        Muted(S.pickAGame(s.playerCount), Modifier.padding(top = 4.dp, bottom = 4.dp))
        lockedNote?.let { Text(it, color = Champagne, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 4.dp)) }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Mood.entries.forEach { mood ->
                Column(Modifier.padding(top = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(S.moodTitle(mood), style = MaterialTheme.typography.titleLarge, color = if (mood == Mood.SPICY) Wine else Ivory)
                        if (mood == Mood.SPICY) { Spacer(Modifier.width(8.dp)); AdultBadge() }
                    }
                    Muted(S.moodBlurb(mood))
                }
                GameId.inMood(mood).chunked(2).forEach { pair ->
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        pair.forEach { g ->
                            CatalogTile(
                                g, g == s.game, Modifier.weight(1f).fillMaxHeight(),
                                locked = !GameId.playableWith(g, s.playerCount),
                            ) { pick(g) }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** Step after Play game: how many people are playing. Big number with minus and plus. */
@Composable
fun PlayerCountScreen(s: SetupState, onCount: (Int) -> Unit, onNext: () -> Unit, onBack: () -> Unit) {
    val S = LocalStrings.current
    val n = s.playerCount
    Page {
        BackRow(onBack)
        Text(S.howManyPlayers, style = MaterialTheme.typography.headlineMedium)
        Muted(S.passPhone(PlayerRules.MIN_PLAYERS, PlayerRules.MAX_PLAYERS), Modifier.padding(top = 4.dp))
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            CountButton("−", enabled = n > PlayerRules.MIN_PLAYERS) { onCount(n - 1) }
            Text(
                "$n", style = MaterialTheme.typography.displayMedium.copy(fontSize = 88.sp, lineHeight = 96.sp),
                color = Ivory, textAlign = TextAlign.Center, modifier = Modifier.width(140.dp),
            )
            CountButton("+", enabled = n < PlayerRules.MAX_PLAYERS) { onCount(n + 1) }
        }
        Eyebrow(S.playersWord, Stone, Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(28.dp))
        Segmented((PlayerRules.MIN_PLAYERS..PlayerRules.MAX_PLAYERS).toList(), n, { "$it" }, onCount)
        Spacer(Modifier.height(12.dp))
        Muted(
            if (n == PlayerRules.COUPLE_PLAYERS) S.countAllOpen else S.countNoCloser,
            Modifier.align(Alignment.CenterHorizontally), textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(1f))
        PrimaryButton(S.nextChooseGame, onNext, Modifier.fillMaxWidth())
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
    val S = LocalStrings.current
    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = Graphite,
        title = { Text(S.couplesGateTitle, style = MaterialTheme.typography.titleLarge) },
        text = {
            Text(
                S.couplesGateBody,
                style = MaterialTheme.typography.bodyMedium, color = Stone,
            )
        },
        confirmButton = { TextButton(onAgree) { Text(S.couplesAgree, color = Champagne) } },
        dismissButton = { TextButton(onCancel) { Text(S.notNow, color = Stone) } },
    )
}

/** Difficulty and language for the chosen game, with a one-line how-to. */
@Composable
fun DifficultyScreen(
    s: SetupState,
    onIntensity: (Intensity) -> Unit,
    onLanguage: (CardLanguage) -> Unit,
    spicyConfirmed: () -> Boolean,
    onConfirmSpicy: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    val S = LocalStrings.current
    val g = s.game
    var wildGate by remember { mutableStateOf(false) }
    if (wildGate) SpicyGate(
        onAgree = { wildGate = false; onConfirmSpicy(); onIntensity(Intensity.WILD) },
        onCancel = { wildGate = false },
    )
    Page {
        BackRow(onBack)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Eyebrow(S.moodTitle(g.mood), if (g.spicy) Wine else Champagne)
            if (g.spicy) { Spacer(Modifier.width(8.dp)); AdultBadge() }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(g.icon, fontSize = 30.sp)
            Spacer(Modifier.width(10.dp))
            Text(S.title(g), style = MaterialTheme.typography.headlineMedium)
        }
        Spacer(Modifier.height(8.dp))
        Text(S.text(g).howTo, style = MaterialTheme.typography.bodyLarge, color = Stone)
        Spacer(Modifier.height(20.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Eyebrow(S.difficulty)
            // 18+ Spicy games play Wild cards only; every other game offers all three levels.
            (g.lockedIntensity?.let { listOf(it) } ?: Intensity.entries).forEach { i ->
                val adult = i == Intensity.WILD
                OptionRow(
                    "${S.intensity(i)}  ·  ${S.points(i.points)}",
                    if (adult) "${S.intensityDescription(i)}\n${S.wildAdultNote}" else S.intensityDescription(i),
                    i == s.intensity,
                ) { if (adult && !spicyConfirmed() && g.lockedIntensity == null) wildGate = true else onIntensity(i) }
            }
            Spacer(Modifier.height(12.dp))
            Eyebrow(S.cardLanguage)
            // Three per row so every language name fits.
            CardLanguage.entries.chunked(3).forEach { row ->
                Segmented(row, s.language, { it.label }, onLanguage)
            }
        }
        Spacer(Modifier.height(16.dp))
        PrimaryButton(if (g.mode == GameMode.COUPLE_BINGO) S.next else S.nextPlayers, onNext, Modifier.fillMaxWidth())
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
    val S = LocalStrings.current
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
        Text(S.whosPlaying, style = MaterialTheme.typography.headlineMedium)
        val couples = !s.spin && s.mode.couplesOnly
        Muted(
            if (couples) S.couplesHint else S.avatarHint(s.playerCount),
            Modifier.padding(top = 4.dp, bottom = 12.dp),
        )
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(s.names) { i, name ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = name, onValueChange = { onName(i, it) },
                        placeholder = { Text(if (couples) S.partnerN(i + 1) else S.playerN(i + 1), color = Stone) }, singleLine = true,
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
                Eyebrow(S.rounds)
                Spacer(Modifier.height(8.dp))
                Segmented(GameSession.ROUND_OPTIONS, s.rounds, { "$it" }, onRounds)
            }
        }
        s.error?.let { e ->
            val message = when (e) {
                is SetupError.Players -> S.problem(e.problem)
                SetupError.NoCards -> S.noCardsLeft
            }
            Text(message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 8.dp))
        }
        PrimaryButton(S.startGame, onStart, Modifier.fillMaxWidth())
    }
}

@Composable
fun GameScreen(
    g: GameState,
    feedback: Feedback,
    turnClock: Boolean,
    voice: CardVoice,
    onComplete: () -> Unit,
    onCompleteGuesses: (Set<Int>) -> Unit,
    onSkip: () -> Unit,
    onTimeout: () -> Unit,
    onDismissChaos: () -> Unit,
    /** Index into [UiStrings.reportReasons]. */
    onReport: (Int) -> Unit,
    onQuit: () -> Unit,
) {
    val S = LocalStrings.current
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
        PoseLibrary.get(card.pose)?.let { append(S.pose(it).title).append(". ") }
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
            // The card's own language: a game with English-only cards is read in English.
            if (voice.autoRead) voice.speaker.speak(spokenKey, spoken, card.language)
        }
    }

    // The showdown starts with a jolt.
    LaunchedEffect(g.showdown) { if (g.showdown) feedback.alert() }
    val clockSeconds = if (turnClock && clocked(card)) TURN_CLOCK_SECONDS else null
    val clockPaused = g.chaos != null || stage < 2 || confirmQuit || reporting

    Page {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Eyebrow(if (g.showdown) S.showdown else S.roundOf(g.round, g.totalRounds), if (g.showdown) Wine else Stone)
            QuietButton(S.endGame, { confirmQuit = true })
        }
        g.lastTurn?.let { TurnCallout(it, Modifier.padding(bottom = 6.dp)) }
        if (stage < 2) {
            SpinStage(g, stage, feedback, onNextStage = { stage++ })
            Spacer(Modifier.height(12.dp))
            Scoreline(g)
            return@Page
        }
        val guessing = card.category.guessing
        // Guessing games: answer secretly first, then reveal and mark who guessed right.
        var revealed by rememberSaveable(g.round) { mutableStateOf(false) }
        var correct by remember(g.round) { mutableStateOf(setOf<Int>()) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            AvatarBadge(LocalAvatars.current[g.currentPlayer.name], 48.dp, highlighted = true)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(g.currentPlayer.name, style = MaterialTheme.typography.headlineMedium)
                val streak = g.currentPlayer.streak
                if (guessing) Muted(S.keepItSecret)
                else if (card.category == GameMode.CHARADES) Text(S.onlyYouLook(g.currentPlayer.name), color = Champagne, style = MaterialTheme.typography.bodyMedium)
                else if (g.stakes && streak >= 1) {
                    Text(
                        S.streakLine(streak, Stakes.streakBonus(streak + 1)),
                        color = if (Stakes.streakBonus(streak + 1) > 0) Champagne else Stone,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else Muted(S.yourTurn)
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Tag(S.mode(card.category), Ivory.copy(alpha = .8f))
                    Tag(S.intensity(card.intensity), intensityColor(card.intensity))
                    if (card.intensity == Intensity.WILD) AdultBadge()
                    Spacer(Modifier.weight(1f))
                    SpeakerButton(voice, spokenKey, spoken, card.language)
                }
                Column(
                    Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center,
                ) {
                    PoseLibrary.get(card.pose)?.let { pose ->
                        val poseText = S.pose(pose)
                        PoseIllustration(pose, Modifier.padding(vertical = 8.dp))
                        Text(poseText.title, style = MaterialTheme.typography.titleLarge, color = Champagne)
                        Text(poseText.hint, style = MaterialTheme.typography.bodyMedium, color = Stone, modifier = Modifier.padding(top = 2.dp, bottom = 12.dp))
                        Eyebrow(S.whatToDo, Stone)
                        Spacer(Modifier.height(4.dp))
                    }
                    Text(
                        g.cardText ?: card.text,
                        style = MaterialTheme.typography.headlineSmall, color = Ivory,
                    )
                    // This or That and Would You Rather: pick, then see how players usually choose.
                    if (card.isVote) VotePanel(card, key = g.round)
                }
                if (guessing) Text(
                    if (revealed) S.whoGotIt else S.guessThenReveal(g.currentPlayer.name),
                    style = MaterialTheme.typography.bodyMedium, color = Stone, modifier = Modifier.padding(bottom = 4.dp),
                )
                card.timerSeconds?.let { secs -> DareTimer(key = g.round, seconds = secs, feedback = feedback) }
                TextButton({ reporting = true }, contentPadding = PaddingValues(0.dp)) {
                    Text(S.reportCard, color = Stone.copy(alpha = .7f), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        if (guessing && revealed) {
            GuesserPicker(g, correct) { i -> correct = if (i in correct) correct - i else correct + i }
            Spacer(Modifier.height(10.dp))
            PrimaryButton(
                if (correct.isEmpty()) S.nobodyGotIt else S.scoreEach(card.intensity.points * (if (g.showdown) 2 else 1)),
                { onCompleteGuesses(correct) }, Modifier.fillMaxWidth(),
            )
        } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SecondaryButton(if (g.skipCost > 0) S.skipCost(g.skipCost) else S.skip, onSkip, Modifier.weight(1f))
            if (guessing) PrimaryButton(S.revealAnswer, { revealed = true }, Modifier.weight(1.6f))
            else PrimaryButton(S.donePoints(g.pointsOnOffer), onComplete, Modifier.weight(1.6f))
        }
        Spacer(Modifier.height(12.dp))
        Scoreline(g)
    }

    g.chaos?.let { event -> ChaosDialog(event, feedback, voice, onDismissChaos) }

    if (confirmQuit) AlertDialog(
        onDismissRequest = { confirmQuit = false },
        containerColor = Graphite,
        title = { Text(S.endGameTitle, style = MaterialTheme.typography.titleLarge) },
        text = { Text(S.scoresLost, color = Stone) },
        confirmButton = { TextButton(onQuit) { Text(S.endGame, color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton({ confirmQuit = false }) { Text(S.keepPlaying, color = Champagne) } },
    )

    if (reporting) AlertDialog(
        onDismissRequest = { reporting = false },
        containerColor = Graphite,
        title = { Text(S.reportTitle, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                Text(S.reportBody, color = Stone, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                S.reportReasons.forEachIndexed { index, reason ->
                    TextButton({ reporting = false; onReport(index) }, contentPadding = PaddingValues(0.dp)) {
                        Text(reason, color = Ivory)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton({ reporting = false }) { Text(S.cancel, color = Stone) } },
    )
}

/** The two wheels of a spin game: first who plays, then Truth or Dare. */
@Composable
private fun ColumnScope.SpinStage(g: GameState, stage: Int, feedback: Feedback, onNextStage: () -> Unit) {
    val S = LocalStrings.current
    val card = g.card ?: return
    val choosingPlayer = stage == 0
    val labels = if (choosingPlayer) g.players.map { it.name } else GameSession.SPIN_MODES.map { S.mode(it) }
    val target = if (choosingPlayer) g.currentPlayerIndex else GameSession.SPIN_MODES.indexOf(card.category).coerceAtLeast(0)
    var spinKey by remember(g.round, stage) { mutableStateOf<Int?>(null) }
    var landed by remember(g.round, stage) { mutableStateOf(false) }

    Text(
        when {
            choosingPlayer && landed -> g.currentPlayer.name
            choosingPlayer -> S.whosNext
            landed -> S.mode(card.category)
            else -> S.truthOrDare(g.currentPlayer.name)
        },
        style = MaterialTheme.typography.headlineMedium,
        color = if (landed) Champagne else Ivory,
    )
    Muted(if (choosingPlayer) S.spinToPick else S.spinToFind)
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
        spinKey == null -> PrimaryButton(S.spin, { feedback.tap(); spinKey = g.round * 10 + stage }, Modifier.fillMaxWidth())
        !landed -> PrimaryButton(S.spinning, {}, Modifier.fillMaxWidth(), enabled = false)
        else -> PrimaryButton(if (choosingPlayer) S.nextTruthOrDare else S.showMyCard, { feedback.tap(); onNextStage() }, Modifier.fillMaxWidth())
    }
}

/** Optional countdown for timed dares. Starts on tap so the player can get ready. */
@Composable
private fun DareTimer(key: Int, seconds: Int, feedback: Feedback) {
    val S = LocalStrings.current
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
            left == 0 -> Text(S.time, color = Wine, style = MaterialTheme.typography.labelLarge)
            !running -> TextButton({ running = true }) { Text(S.startTimer, color = Champagne) }
            else -> TextButton({ running = false }) { Text(S.pause, color = Stone) }
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
private fun ChaosDialog(event: ChaosEvent, feedback: Feedback, voice: CardVoice, onDismiss: () -> Unit) {
    val S = LocalStrings.current
    val text = S.chaosText(event)
    val spoken = "${text.title}. ${text.text}"
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
                    Eyebrow(S.chaosMoment, Wine, Modifier.weight(1f))
                    SpeakerButton(voice, spokenKey, spoken)
                }
                Spacer(Modifier.height(6.dp))
                Text(text.title, style = MaterialTheme.typography.headlineSmall)
            }
        },
        text = {
            Column {
                Text(text.text, style = MaterialTheme.typography.bodyLarge, color = Ivory)
                event.timerSeconds?.let { DareTimer(key = event.id.hashCode(), seconds = it, feedback = feedback) }
            }
        },
        confirmButton = { TextButton({ voice.speaker.stop(); onDismiss() }) { Text(S.continueLabel, color = Champagne) } },
    )
}

/** Tap every player who guessed the answer right. The answerer isn't listed. */
@Composable
private fun GuesserPicker(g: GameState, correct: Set<Int>, onToggle: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        g.players.forEachIndexed { i, p ->
            if (i == g.currentPlayerIndex) return@forEachIndexed
            val on = i in correct
            val shape = RoundedCornerShape(50)
            Text(
                if (on) "✓  ${p.name}" else p.name,
                color = if (on) Champagne else Ivory,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.clip(shape)
                    .background(if (on) GraphiteHigh else Graphite)
                    .border(1.dp, if (on) Champagne else HairlineStrong, shape)
                    .clickable { onToggle(i) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
    }
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
                "${p.name}  ${LocalStrings.current.points(p.score)}",
                color = if (active) Champagne else Stone,
                fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
fun ResultsScreen(g: GameState, bonusLabel: String?, onBonus: () -> Unit, onAgain: () -> Unit, onHome: () -> Unit) {
    val S = LocalStrings.current
    Page(horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(32.dp))
        Eyebrow(S.gameOver)
        val w = g.winners
        Text(
            if (w.isEmpty()) S.nobodyScored else w.joinToString(" & ") { it.name },
            style = MaterialTheme.typography.displayMedium.copy(fontSize = 36.sp, lineHeight = 42.sp),
            textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 12.dp),
        )
        Muted(if (w.isEmpty()) S.everyoneSafe else if (w.size > 1) S.sharedWin else S.takesNight, textAlign = TextAlign.Center)
        Muted(S.skipped(g.skips), Modifier.padding(top = 4.dp))
        g.players.maxByOrNull { it.bestStreak }?.takeIf { g.stakes && it.bestStreak >= 2 }?.let { p ->
            Muted(S.longestStreak(p.name, p.bestStreak), Modifier.padding(top = 2.dp))
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
                    Text(LocalStrings.current.points(p.score), style = MaterialTheme.typography.bodyLarge, color = if (i == 0 && w.isNotEmpty()) Champagne else Stone)
                }
                HorizontalDivider(color = Hairline)
            }
        }
        Spacer(Modifier.height(16.dp))
        PrimaryButton(S.playAgain, onAgain, Modifier.fillMaxWidth())
        if (bonusLabel != null) {
            Spacer(Modifier.height(10.dp))
            SecondaryButton(bonusLabel, onBonus, Modifier.fillMaxWidth())
        }
        QuietButton(S.home, onHome)
    }
}
