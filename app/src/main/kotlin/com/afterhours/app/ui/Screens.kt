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
import com.afterhours.app.SetupError
import com.afterhours.app.SetupState
import com.afterhours.app.ads.AdManager
import com.afterhours.app.ads.BannerAd
import com.afterhours.app.ads.ConsentManager
import com.afterhours.app.billing.BillingManager
import com.afterhours.app.data.Settings
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
                    language = language,
                    adsRemoved = adsRemoved,
                    removeAdsPrice = price,
                    privacyRequired = privacyRequired,
                    onPlay = { feedback.tap(); nav.navigate("games") },
                    onSpin = { feedback.tap(); vm.selectSpin(); nav.navigate("difficulty") },
                    onHowToPlay = { nav.navigate("tutorial") },
                    onSound = vm::setSound,
                    onHaptics = vm::setHaptics,
                    onChaos = vm::setChaos,
                    onLanguage = vm::chooseLanguage,
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
                onComplete = { feedback.tap(); vm.complete() },
                onSkip = { feedback.tap(); vm.skip() },
                onDismissChaos = vm::dismissChaos,
                onReport = { reason ->
                    val card = state.card
                    // Reports go to the developer, so the reason is always sent in English.
                    if (card != null) sendReport(activity, card, EnglishStrings.reportReasons[reason])
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
                    adsRemoved -> S.bonusGame
                    rewardedReady -> S.watchAdBonus
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
        Text("‹  ${LocalStrings.current.back}", color = Stone, style = MaterialTheme.typography.bodyMedium)
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
    onPlay: () -> Unit,
    onSpin: () -> Unit,
    onHowToPlay: () -> Unit,
    onSound: (Boolean) -> Unit,
    onHaptics: (Boolean) -> Unit,
    onChaos: (Boolean) -> Unit,
    onLanguage: (CardLanguage) -> Unit,
    onResetSeen: () -> Unit,
    onPrivacy: () -> Unit,
    onRemoveAds: () -> Unit,
    onRestore: () -> Unit,
) {
    val S = LocalStrings.current
    var showSettings by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    Page(horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(1f))
        Wordmark()
        Spacer(Modifier.height(20.dp))
        Muted(S.homeTagline(games.size - 1), textAlign = TextAlign.Center)
        Spacer(Modifier.weight(1f))
        PrimaryButton(S.games, onPlay, Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        SecondaryButton(S.spinTheWheel, onSpin, Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        SecondaryButton(S.howToPlay, onHowToPlay, Modifier.fillMaxWidth())
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            QuietButton(S.settings, { showSettings = true })
            QuietButton(S.helpSafety, { showHelp = true })
        }
    }
    if (showSettings) SettingsDialog(
        settings, language, adsRemoved, removeAdsPrice, privacyRequired,
        onSound, onHaptics, onChaos, onLanguage, onResetSeen, onPrivacy, onRemoveAds, onRestore,
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
    onLanguage: (CardLanguage) -> Unit,
    onResetSeen: () -> Unit,
    onPrivacy: () -> Unit,
    onRemoveAds: () -> Unit,
    onRestore: () -> Unit,
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
                SwitchRow(S.sound, settings.sound, onSound)
                SwitchRow(S.vibration, settings.haptics, onHaptics)
                SwitchRow(S.chaosMoments, settings.chaos, onChaos, S.chaosHint)
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

private enum class GameGroup { FEATURED, CLASSIC, WHOLE_GROUP, COUPLES }

/** One entry in the Games section. [mode] is null for Spin the Wheel, which mixes Truth and Dare. */
private data class GameEntry(val mode: GameMode?, val group: GameGroup)

private val games = listOf(
    GameEntry(null, GameGroup.FEATURED),
    GameEntry(GameMode.TRUTH, GameGroup.CLASSIC),
    GameEntry(GameMode.DARE, GameGroup.CLASSIC),
    GameEntry(GameMode.WOULD_YOU_RATHER, GameGroup.CLASSIC),
    GameEntry(GameMode.COUPLE, GameGroup.CLASSIC),
    GameEntry(GameMode.NEVER_HAVE_I_EVER, GameGroup.WHOLE_GROUP),
    GameEntry(GameMode.MOST_LIKELY_TO, GameGroup.WHOLE_GROUP),
    GameEntry(GameMode.HOT_SEAT, GameGroup.WHOLE_GROUP),
    GameEntry(GameMode.CLOSER, GameGroup.COUPLES),
)

private fun UiStrings.title(g: GameEntry) = g.mode?.let(mode) ?: spinTheWheel

private fun UiStrings.groupLabel(g: GameGroup) = when (g) {
    GameGroup.FEATURED -> groupFeatured
    GameGroup.CLASSIC -> groupClassic
    GameGroup.WHOLE_GROUP -> groupWholeGroup
    GameGroup.COUPLES -> groupCouples
}

private fun selectedGame(s: SetupState): GameEntry =
    if (s.spin) games.first() else games.firstOrNull { it.mode == s.mode } ?: games.first()

/** The Games section: every game as its own tile. Tapping one opens its difficulty choice. */
@Composable
fun GamesScreen(s: SetupState, onPick: (GameMode?) -> Unit, onBack: () -> Unit) {
    val S = LocalStrings.current
    val current = selectedGame(s)
    var gateFor by remember { mutableStateOf<GameMode?>(null) }
    val pick: (GameMode?) -> Unit = { m -> if (m?.couplesOnly == true) gateFor = m else onPick(m) }
    gateFor?.let { m -> CouplesGate(onAgree = { gateFor = null; onPick(m) }, onCancel = { gateFor = null }) }
    Page {
        BackRow(onBack)
        Text(S.games, style = MaterialTheme.typography.headlineMedium)
        Muted(S.gamesCount(games.size), Modifier.padding(top = 4.dp, bottom = 12.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            games.groupBy { it.group }.forEach { (group, entries) ->
                Eyebrow(S.groupLabel(group), modifier = Modifier.padding(top = 6.dp))
                entries.chunked(2).forEach { pair ->
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        pair.forEach { g -> GameTile(g, g == current, Modifier.weight(1f).fillMaxHeight(), wide = pair.size == 1 && group == GameGroup.FEATURED) { pick(g.mode) } }
                        if (pair.size == 1 && group != GameGroup.FEATURED) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
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

@Composable
private fun GameTile(g: GameEntry, selected: Boolean, modifier: Modifier, wide: Boolean, onClick: () -> Unit) {
    val S = LocalStrings.current
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier.clip(shape)
            .background(if (selected) GraphiteHigh else Graphite)
            .border(1.dp, if (selected || wide) Champagne.copy(alpha = if (selected) 1f else .5f) else Hairline, shape)
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        if (wide) { Eyebrow(S.newBadge); Spacer(Modifier.height(6.dp)) }
        Text(S.title(g), style = MaterialTheme.typography.titleLarge, color = Ivory)
        Spacer(Modifier.height(4.dp))
        Text(S.game(g.mode).blurb, style = MaterialTheme.typography.bodyMedium, color = Stone)
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
    val S = LocalStrings.current
    val g = selectedGame(s)
    Page {
        BackRow(onBack)
        Eyebrow(S.groupLabel(g.group))
        Text(S.title(g), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(S.game(g.mode).howTo, style = MaterialTheme.typography.bodyLarge, color = Stone)
        Spacer(Modifier.height(20.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Eyebrow(S.difficulty)
            Intensity.entries.forEach { i ->
                OptionRow("${S.intensity(i)}  ·  ${S.points(i.points)}", S.intensityDescription(i), i == s.intensity) { onIntensity(i) }
            }
            Spacer(Modifier.height(12.dp))
            Eyebrow(S.cardLanguage)
            // Three per row so every language name fits.
            CardLanguage.entries.chunked(3).forEach { row ->
                Segmented(row, s.language, { it.label }, onLanguage)
            }
        }
        Spacer(Modifier.height(16.dp))
        PrimaryButton(S.nextPlayers, onNext, Modifier.fillMaxWidth())
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
    val S = LocalStrings.current
    Page {
        BackRow(onBack)
        Text(S.whosPlaying, style = MaterialTheme.typography.headlineMedium)
        val couples = !s.spin && s.mode.couplesOnly
        Muted(
            if (couples) S.couplesHint else S.playersHint(s.names.size),
            Modifier.padding(top = 4.dp, bottom = 12.dp),
        )
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(s.names) { i, name ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = name, onValueChange = { onName(i, it) },
                        placeholder = { Text(if (couples) S.partnerN(i + 1) else S.playerN(i + 1), color = Stone) }, singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Champagne, cursorColor = Champagne,
                            unfocusedBorderColor = Hairline,
                            unfocusedContainerColor = Graphite, focusedContainerColor = Graphite,
                        ),
                    )
                    if (s.names.size > PlayerRules.MIN_PLAYERS) TextButton({ onRemove(i) }) { Text(S.remove, color = Stone, style = MaterialTheme.typography.bodyMedium) }
                }
            }
            item {
                if (!couples && s.names.size < PlayerRules.MAX_PLAYERS) TextButton(onAdd, contentPadding = PaddingValues(0.dp)) {
                    Text("+  ${S.addPlayer}", color = Champagne, style = MaterialTheme.typography.bodyLarge)
                }
                Spacer(Modifier.height(16.dp))
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
    onComplete: () -> Unit,
    onSkip: () -> Unit,
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

    Page {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Eyebrow(S.roundOf(g.round, g.totalRounds), Stone)
            QuietButton(S.endGame, { confirmQuit = true })
        }
        if (stage < 2) {
            SpinStage(g, stage, feedback, onNextStage = { stage++ })
            Spacer(Modifier.height(12.dp))
            Scoreline(g)
            return@Page
        }
        Text(g.currentPlayer.name, style = MaterialTheme.typography.headlineMedium)
        Muted(S.yourTurn)
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Tag(S.mode(card.category), Ivory.copy(alpha = .8f))
                    Tag(S.intensity(card.intensity), intensityColor(card.intensity))
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
                }
                card.timerSeconds?.let { secs -> DareTimer(key = g.round, seconds = secs, feedback = feedback) }
                TextButton({ reporting = true }, contentPadding = PaddingValues(0.dp)) {
                    Text(S.reportCard, color = Stone.copy(alpha = .7f), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SecondaryButton(S.skip, onSkip, Modifier.weight(1f))
            PrimaryButton(S.donePoints(card.intensity.points), onComplete, Modifier.weight(1.6f))
        }
        Spacer(Modifier.height(12.dp))
        Scoreline(g)
    }

    g.chaos?.let { event -> ChaosDialog(event, feedback, onDismissChaos) }

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
            left == 0 -> Text(S.time, color = Wine, style = MaterialTheme.typography.labelLarge)
            !running -> TextButton({ running = true }) { Text(S.startTimer, color = Champagne) }
            else -> TextButton({ running = false }) { Text(S.pause, color = Stone) }
        }
    }
}

@Composable
private fun ChaosDialog(event: ChaosEvent, feedback: Feedback, onDismiss: () -> Unit) {
    val S = LocalStrings.current
    val text = S.chaosText(event)
    LaunchedEffect(event.id) { feedback.alert() }
    AlertDialog(
        onDismissRequest = {},
        containerColor = GraphiteHigh,
        title = {
            Column {
                Eyebrow(S.chaosMoment, Wine)
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
        confirmButton = { TextButton(onDismiss) { Text(S.continueLabel, color = Champagne) } },
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
        PrimaryButton(S.playAgain, onAgain, Modifier.fillMaxWidth())
        if (bonusLabel != null) {
            Spacer(Modifier.height(10.dp))
            SecondaryButton(bonusLabel, onBonus, Modifier.fillMaxWidth())
        }
        QuietButton(S.home, onHome)
    }
}
