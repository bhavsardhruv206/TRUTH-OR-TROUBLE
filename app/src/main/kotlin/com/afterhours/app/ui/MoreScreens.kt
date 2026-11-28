package com.afterhours.app.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afterhours.core.*
import com.afterhours.core.i18n.GameText
import com.afterhours.core.i18n.UiStrings

// ---------- Catalog text ----------

/** The name of a game in the catalog, in the app language. */
fun UiStrings.title(g: GameId): String = when (g) {
    GameId.SPIN -> spinTheWheel
    GameId.LOVE_QUIZ -> loveQuiz
    GameId.SPICY_SPIN -> spicySpin
    GameId.SPICY_NEVER -> spicyNever
    GameId.SPICY_COUPLE -> spicyCouple
    else -> mode(g.mode!!)
}

/** Short line and how-to for a game in the catalog. */
fun UiStrings.text(g: GameId): GameText = when (g) {
    GameId.SPIN -> game(null)
    GameId.LOVE_QUIZ -> loveQuizGame
    GameId.SPICY_SPIN -> spicySpinGame
    GameId.SPICY_NEVER -> spicyNeverGame
    GameId.SPICY_COUPLE -> spicyCoupleGame
    else -> game(g.mode)
}

// ---------- Shared pieces ----------

/** A small, clear "18+" label for adult content. */
@Composable
fun AdultBadge(modifier: Modifier = Modifier) {
    Text(
        LocalStrings.current.adultBadge, color = Wine, fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.labelSmall,
        modifier = modifier.border(1.dp, Wine, RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

/** Extra check before 18+ content: everyone holding the phone tonight must be an adult. */
@Composable
fun SpicyGate(onAgree: () -> Unit, onCancel: () -> Unit) {
    val S = LocalStrings.current
    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = Graphite,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AdultBadge()
                Spacer(Modifier.width(8.dp))
                Text(S.spicyGateTitle, style = MaterialTheme.typography.titleLarge)
            }
        },
        text = { Text(S.spicyGateBody, style = MaterialTheme.typography.bodyMedium, color = Stone) },
        confirmButton = { TextButton(onAgree) { Text(S.spicyAgree, color = Champagne) } },
        dismissButton = { TextButton(onCancel) { Text(S.notNow, color = Stone) } },
    )
}

/** One game tile. Dimmed with a note when it can't be played with this many players. */
@Composable
fun CatalogTile(g: GameId, selected: Boolean, modifier: Modifier, locked: Boolean = false, compact: Boolean = false, onClick: () -> Unit) {
    val S = LocalStrings.current
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier.graphicsLayer { alpha = if (locked) .45f else 1f }.clip(shape)
            .background(if (selected) GraphiteHigh else Graphite)
            .border(1.dp, if (selected) Champagne else if (g.spicy) Wine.copy(alpha = .6f) else Hairline, shape)
            .clickable(onClick = onClick)
            .padding(if (compact) 12.dp else 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(g.icon, fontSize = if (compact) 20.sp else 22.sp)
            Spacer(Modifier.weight(1f))
            if (g.spicy) AdultBadge()
        }
        Spacer(Modifier.height(6.dp))
        Text(S.title(g), style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge, color = Ivory)
        Spacer(Modifier.height(4.dp))
        Text(S.text(g).blurb, style = MaterialTheme.typography.bodyMedium, color = Stone, maxLines = if (compact) 2 else 3)
        if (locked) { Spacer(Modifier.height(6.dp)); Text(S.twoPlayersOnly, style = MaterialTheme.typography.labelSmall, color = Champagne) }
    }
}

/** Home's "Suggested tonight" row: tap a game to start it in one tap. */
@Composable
fun TonightRow(picks: List<GameId>, onPick: (GameId) -> Unit, modifier: Modifier = Modifier) {
    val S = LocalStrings.current
    Column(modifier.fillMaxWidth()) {
        Eyebrow(S.suggestedTonight)
        Muted(S.oneTapHint, Modifier.padding(top = 2.dp, bottom = 10.dp))
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            picks.forEach { g ->
                CatalogTile(g, selected = false, modifier = Modifier.weight(1f).fillMaxHeight(), compact = true) { onPick(g) }
            }
        }
    }
}

/**
 * Voting cards (This or That, Would You Rather): tap a pick, then see how players usually choose.
 * The shares are bundled estimates because the app works offline.
 */
@Composable
fun VotePanel(card: com.afterhours.core.Card, key: Any) {
    val S = LocalStrings.current
    val pickA = card.pickA ?: return
    var chose by rememberSaveable(key) { mutableStateOf<Boolean?>(null) }
    val labels = card.options?.takeIf { it.size == 2 } ?: listOf(S.voteFirst, S.voteSecond)
    Column(Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (chose == null) Text(S.votePrompt, style = MaterialTheme.typography.bodyMedium, color = Stone)
        labels.forEachIndexed { i, label ->
            val first = i == 0
            val percent = PickStats.percentFor(pickA, first)
            val picked = chose == first
            val shape = RoundedCornerShape(12.dp)
            Box(
                Modifier.fillMaxWidth().clip(shape)
                    .background(GraphiteHigh)
                    .border(1.dp, if (picked) Champagne else Hairline, shape)
                    .clickable(enabled = chose == null) { chose = first },
            ) {
                if (chose != null) Box(Modifier.matchParentSize()) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(percent / 100f).background((if (picked) Champagne else Stone).copy(alpha = .18f)))
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(label, style = MaterialTheme.typography.titleMedium, color = Ivory, modifier = Modifier.weight(1f))
                    if (chose != null) Text("$percent%", style = MaterialTheme.typography.titleMedium, color = if (picked) Champagne else Stone)
                }
            }
        }
        chose?.let { first ->
            Text(S.usuallyPick(PickStats.percentFor(pickA, first)), style = MaterialTheme.typography.bodyMedium, color = Champagne)
        }
    }
}

// ---------- Couple Bingo ----------

@Composable
fun BingoScreen(board: BingoBoard?, onToggle: (Int) -> Unit, onNewBoard: () -> Unit, onBack: () -> Unit) {
    val S = LocalStrings.current
    BackHandler(onBack = onBack)
    Page {
        BackRow(onBack)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(GameId.COUPLE_BINGO.icon, fontSize = 28.sp)
            Spacer(Modifier.width(10.dp))
            Text(S.mode(GameMode.COUPLE_BINGO), style = MaterialTheme.typography.headlineMedium)
        }
        Muted(S.bingoHint, Modifier.padding(top = 4.dp, bottom = 16.dp))
        if (board == null) {
            Muted(S.noCardsLeft)
            Spacer(Modifier.weight(1f))
        } else {
            val inLine = board.lines.flatten().toSet()
            Column(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                (0 until BingoBoard.SIZE).forEach { r ->
                    Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (0 until BingoBoard.SIZE).forEach { c ->
                            val i = r * BingoBoard.SIZE + c
                            val on = i in board.marked
                            val shape = RoundedCornerShape(14.dp)
                            Box(
                                Modifier.weight(1f).fillMaxHeight().clip(shape)
                                    .background(if (i in inLine) Champagne.copy(alpha = .22f) else if (on) GraphiteHigh else Graphite)
                                    .border(if (on) 2.dp else 1.dp, if (on) Champagne else Hairline, shape)
                                    .clickable { onToggle(i) }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    (if (on) "✓ " else "") + board.squares[i].text,
                                    style = MaterialTheme.typography.bodyMedium, color = if (on) Ivory else Stone,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                when {
                    board.full -> S.bingoFull
                    board.bingos > 0 -> S.bingoCount(board.bingos)
                    else -> S.bingoNone
                },
                style = MaterialTheme.typography.titleLarge, color = if (board.bingos > 0) Champagne else Stone,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
        Spacer(Modifier.height(12.dp))
        PrimaryButton(S.newBoard, onNewBoard, Modifier.fillMaxWidth())
    }
}

// ---------- Love-language quiz ----------

@Composable
fun QuizScreen(onBack: () -> Unit) {
    val S = LocalStrings.current
    val quiz = S.loveQuizText
    val activity = LocalContext.current as Activity
    // -1 = intro, 0..9 = question, 10 = result
    var step by remember { mutableIntStateOf(-1) }
    var answers by remember { mutableStateOf(listOf<Boolean>()) }
    val total = quiz.questions.size
    BackHandler {
        when {
            step > 0 && step < total -> { step--; answers = answers.dropLast(1) }
            else -> onBack()
        }
    }
    Page {
        BackRow(onBack)
        when {
            step < 0 -> {
                Text(GameId.LOVE_QUIZ.icon, fontSize = 36.sp)
                Spacer(Modifier.height(8.dp))
                Text(S.loveQuiz, style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(12.dp))
                Text(S.quizIntro, style = MaterialTheme.typography.bodyLarge, color = Stone)
                Spacer(Modifier.weight(1f))
                PrimaryButton(S.quizStart, { answers = emptyList(); step = 0 }, Modifier.fillMaxWidth())
            }
            step < total -> {
                val q = quiz.questions[step]
                Eyebrow(S.quizQuestion(step + 1, total))
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(total) { i -> Box(Modifier.weight(1f).height(2.dp).background(if (i <= step) Champagne else Hairline)) }
                }
                Spacer(Modifier.height(20.dp))
                Text(S.quizPrompt, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(20.dp))
                listOf(q.first to true, q.second to false).forEach { (text, first) ->
                    OptionRow(text, null, selected = false) {
                        answers = answers + first
                        step++
                    }
                    Spacer(Modifier.height(10.dp))
                }
                Spacer(Modifier.weight(1f))
            }
            else -> {
                val result = LoveQuiz.score(quiz.questions, answers)
                val top = result.top
                val name = if (top.size > 1) S.quizTie(quiz.name(top[0]), quiz.name(top[1])) else quiz.name(top[0])
                val percent = result.percent(top[0])
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    QuizResultCard(S.quizResultEyebrow, name, quiz.description(top[0]), S.quizMix, result.ranked.map { quiz.name(it) to result.percent(it) })
                }
                Spacer(Modifier.height(12.dp))
                PrimaryButton(S.shareResult, {
                    ResultShare.shareLoveLanguage(
                        activity,
                        eyebrow = S.quizResultEyebrow,
                        result = name,
                        description = quiz.description(top[0]),
                        mixTitle = S.quizMix,
                        mix = result.ranked.map { quiz.name(it) to result.percent(it) },
                        shareText = S.quizShareText(name, percent),
                        chooserTitle = S.shareResult,
                    )
                }, Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                SecondaryButton(S.nextPerson, { answers = emptyList(); step = 0 }, Modifier.fillMaxWidth())
            }
        }
    }
}

/** The on-screen result card. The shared image is drawn to match it. */
@Composable
private fun QuizResultCard(eyebrow: String, result: String, description: String, mixTitle: String, mix: List<Pair<String, Int>>) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(Graphite).border(1.dp, Champagne.copy(alpha = .6f), shape).padding(24.dp),
    ) {
        Eyebrow(eyebrow)
        Spacer(Modifier.height(8.dp))
        Text(result, style = MaterialTheme.typography.headlineMedium, color = Champagne)
        Spacer(Modifier.height(10.dp))
        Text(description, style = MaterialTheme.typography.bodyLarge, color = Ivory)
        Spacer(Modifier.height(20.dp))
        Eyebrow(mixTitle, Stone)
        Spacer(Modifier.height(8.dp))
        mix.forEach { (label, pct) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.bodyMedium, color = Ivory, modifier = Modifier.weight(1f))
                Text("$pct%", style = MaterialTheme.typography.bodyMedium, color = Stone)
            }
            Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Hairline)) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(pct / 100f).background(Champagne))
            }
        }
    }
}
