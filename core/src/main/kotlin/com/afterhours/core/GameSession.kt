package com.afterhours.core

import kotlin.random.Random

object PlayerRules {
    const val MIN_PLAYERS = 2
    const val MAX_PLAYERS = 8
    const val MAX_NAME_LENGTH = 16
    /** Couples-only games are for exactly this many players. */
    const val COUPLE_PLAYERS = 2

    sealed interface Result {
        data class Valid(val names: List<String>) : Result
        data class Invalid(val problem: Problem, val reason: String) : Result
    }

    /** Why a list of players can't start a game. The UI shows it in the player's language. */
    enum class Problem { COUPLE_COUNT, TOO_FEW, TOO_MANY, EMPTY_NAME, NAME_TOO_LONG, DUPLICATE_NAME }

    /** [mode] adds that game's own player rules, such as exactly two players for couples-only games. */
    fun validate(rawNames: List<String>, mode: GameMode? = null): Result {
        val names = rawNames.map { it.trim() }
        if (mode?.couplesOnly == true && names.size != COUPLE_PLAYERS) return Result.Invalid(Problem.COUPLE_COUNT, "${mode.label} is for exactly $COUPLE_PLAYERS players: you and your partner.")
        if (names.size < MIN_PLAYERS) return Result.Invalid(Problem.TOO_FEW, "Add at least $MIN_PLAYERS players.")
        if (names.size > MAX_PLAYERS) return Result.Invalid(Problem.TOO_MANY, "At most $MAX_PLAYERS players can play.")
        if (names.any { it.isEmpty() }) return Result.Invalid(Problem.EMPTY_NAME, "Every player needs a nickname.")
        if (names.any { it.length > MAX_NAME_LENGTH }) return Result.Invalid(Problem.NAME_TOO_LONG, "Nicknames can be up to $MAX_NAME_LENGTH characters.")
        if (names.map { it.lowercase() }.toSet().size != names.size) return Result.Invalid(Problem.DUPLICATE_NAME, "Nicknames must be different.")
        return Result.Valid(names)
    }
}

data class GameState(
    val mode: GameMode,
    val intensity: Intensity,
    val players: List<Player>,
    val currentPlayerIndex: Int,
    /** 1-based number of the round being played. */
    val round: Int,
    val totalRounds: Int,
    val card: Card?,
    /** The card text with `{name}` / `{other}` filled in for the current player. */
    val cardText: String?,
    val finished: Boolean,
    val skips: Int,
    /** A whole-group moment shown before the current turn; null when none is pending. */
    val chaos: ChaosEvent? = null,
    /** Spin-the-wheel game: a wheel picks the player and Truth or Dare each turn. */
    val spin: Boolean = false,
    /** Streak bonuses, skip penalties and double-point final rounds are on. */
    val stakes: Boolean = false,
    /** The last rounds of the game, played for double points when [stakes] is on. */
    val showdownRounds: Int = 0,
    /** What the previous turn scored, for a short call-out on the next card. */
    val lastTurn: TurnResult? = null,
) {
    /** True while this round is one of the double-point final rounds. */
    val showdown: Boolean get() = stakes && !finished && round > totalRounds - showdownRounds
    /** Points the current player would win by completing the current card. */
    val pointsOnOffer: Int
        get() {
            val c = card ?: return 0
            return Stakes.completePoints(c.intensity, currentPlayer.streak + 1, showdown, stakes)
        }
    /** Points a skip would cost the current player right now. */
    val skipCost: Int get() = if (stakes && !mode.couplesOnlyFor(spin)) minOf(Stakes.SKIP_PENALTY, currentPlayer.score) else 0
    val currentPlayer: Player get() = players[currentPlayerIndex]
    val scoreboard: List<Player> get() = players.sortedByDescending { it.score }
    /** Everyone sharing the top score (empty if nobody scored). */
    val winners: List<Player>
        get() {
            val top = players.maxOf { it.score }
            return if (top == 0) emptyList() else players.filter { it.score == top }
        }
}

/** How the previous turn went. [points] is negative for a penalty. */
data class TurnResult(
    val playerName: String,
    val points: Int,
    val streak: Int,
    val timedOut: Boolean = false,
    /** The scoring took this player into sole first place. */
    val tookLead: Boolean = false,
)

/** Scoring rules that raise the stakes. Pure functions so they are easy to test. */
object Stakes {
    const val SKIP_PENALTY = 1
    /** Streak length at which the bonus starts. */
    const val STREAK_START = 2
    const val MAX_STREAK_BONUS = 2

    /** Extra points for completing the [streak]-th card in a row. */
    fun streakBonus(streak: Int): Int = (streak - STREAK_START + 1).coerceIn(0, MAX_STREAK_BONUS)

    fun completePoints(intensity: Intensity, streak: Int, showdown: Boolean, stakes: Boolean): Int {
        if (!stakes) return intensity.points
        val base = intensity.points * (if (showdown) 2 else 1)
        return base + streakBonus(streak)
    }

    /** About a quarter of the game, at least one round and at most one per player. */
    fun showdownRounds(totalRounds: Int, players: Int): Int = (totalRounds / 4).coerceAtMost(players).coerceAtLeast(1)
}

/** Couples games stay free of penalties: either partner can skip anything, no cost. */
internal fun GameMode.couplesOnlyFor(spin: Boolean) = couplesOnly && !spin

/**
 * One pass-the-phone game. Pure logic, no Android dependencies.
 * Completing a card awards the intensity's points; skipping awards and costs nothing.
 * With stakes on, completions in a row earn a streak bonus, the final rounds score double,
 * and skipping or running out of time costs a point (never in couples games).
 */
class GameSession(
    cards: List<Card>,
    mode: GameMode,
    intensity: Intensity,
    playerNames: List<String>,
    totalRounds: Int = DEFAULT_ROUNDS,
    random: Random = Random.Default,
    playedBefore: Set<String> = emptySet(),
    chaosEnabled: Boolean = true,
    language: String = CardLanguage.ENGLISH.code,
    /** Spin the wheel: [mode] is ignored, each turn picks a random player and Truth or Dare. */
    spin: Boolean = false,
    /** Streaks, skip penalties and a double-point showdown. */
    stakes: Boolean = false,
) {
    private val modes = if (spin) SPIN_MODES else listOf(mode)
    private val decks = modes.associateWith { CardDeck(cards, it, intensity, random, language = language, playedBefore = playedBefore) }
    private val picker = if (spin) FairPicker(playerNames.size, random) else null
    // Group chaos moments don't fit a game for two partners.
    private val scheduler = ChaosScheduler(random, enabled = chaosEnabled && !(mode.couplesOnly && !spin))
    private val usedChaos = mutableSetOf<String>()

    /** Ids of cards drawn so far in this game, for remembering between games. */
    val drawnCardIds: Set<String> get() = decks.values.flatMapTo(linkedSetOf()) { it.drawnIds }
    /** Ids of every card this game could draw. */
    val eligibleCardIds: Set<String> get() = decks.values.flatMapTo(mutableSetOf()) { it.eligibleIds }
    private val textRandom = random

    var state: GameState
        private set

    init {
        val valid = PlayerRules.validate(playerNames, if (spin) null else mode)
        require(valid is PlayerRules.Result.Valid) { (valid as PlayerRules.Result.Invalid).reason }
        require(totalRounds > 0) { "totalRounds must be positive" }
        decks.forEach { (m, d) -> require(!d.isEmpty) { "No cards for $m / $intensity / $language" } }
        val players0 = valid.names.mapIndexed { i, n -> Player(i, n) }
        val firstIndex = picker?.next() ?: 0
        val first = drawCard()
        state = GameState(
            mode = mode,
            intensity = intensity,
            players = players0,
            currentPlayerIndex = firstIndex,
            round = 1,
            totalRounds = totalRounds,
            card = first,
            cardText = first?.let { CardText.render(it.text, players0[firstIndex], players0, textRandom) },
            finished = false,
            skips = 0,
            spin = spin,
            stakes = stakes,
            showdownRounds = if (stakes) Stakes.showdownRounds(totalRounds, players0.size) else 0,
        )
    }

    private fun drawCard(): Card? = decks.getValue(if (modes.size == 1) modes[0] else modes.random(textRandom)).draw()

    /** Current player did the truth/dare/choice: award points, move on. */
    fun complete() = advance(Outcome.DONE)

    /** Skip. Costs [GameState.skipCost] when stakes are on. */
    fun skip() = advance(Outcome.SKIP)

    /** Skip that never costs anything, such as after reporting a card. */
    fun skipFree() = advance(Outcome.FREE_SKIP)

    /** The turn clock ran out: counts as a skip. */
    fun timeOut() = advance(Outcome.TIMEOUT)

    /** Closes the pending chaos moment and goes on to the turn. */
    fun dismissChaos() { if (state.chaos != null) state = state.copy(chaos = null) }

    private enum class Outcome { DONE, SKIP, FREE_SKIP, TIMEOUT }

    private fun advance(outcome: Outcome) {
        val s = state
        if (s.finished || s.chaos != null) return
        val card = s.card
        val award = outcome == Outcome.DONE
        val me = s.currentPlayer
        val delta = when {
            award -> if (card != null) s.pointsOnOffer else 0
            outcome == Outcome.FREE_SKIP -> 0
            else -> -s.skipCost
        }
        val streak = if (award) me.streak + 1 else 0
        val updated = me.copy(score = me.score + delta, streak = streak, bestStreak = maxOf(me.bestStreak, streak))
        val players = s.players.map { if (it.id == me.id) updated else it }
        val leaderBefore = s.players.soleLeader()
        val leaderAfter = players.soleLeader()
        val result = TurnResult(
            playerName = me.name,
            points = delta,
            streak = streak,
            timedOut = outcome == Outcome.TIMEOUT,
            tookLead = s.stakes && delta > 0 && leaderAfter == me.id && leaderBefore != me.id,
        ).takeIf { s.stakes }
        val skips = if (award) s.skips else s.skips + 1
        state = if (s.round >= s.totalRounds) {
            s.copy(players = players, card = null, cardText = null, finished = true, skips = skips, lastTurn = result)
        } else {
            val nextIndex = picker?.next() ?: ((s.currentPlayerIndex + 1) % players.size)
            val next = drawCard()
            s.copy(
                players = players,
                currentPlayerIndex = nextIndex,
                round = s.round + 1,
                card = next,
                cardText = next?.let { CardText.render(it.text, players[nextIndex], players, textRandom) },
                chaos = if (scheduler.tick()) ChaosEvents.pick(textRandom, usedChaos).also { usedChaos += it.id } else null,
                skips = skips,
                lastTurn = result,
            )
        }
    }

    private fun List<Player>.soleLeader(): Int? {
        val top = maxOf { it.score }
        if (top == 0) return null
        return filter { it.score == top }.singleOrNull()?.id
    }

    companion object {
        const val DEFAULT_ROUNDS = 10
        val ROUND_OPTIONS = listOf(8, 10, 15, 20)
        /** The wheel chooses between these each turn. */
        val SPIN_MODES = listOf(GameMode.TRUTH, GameMode.DARE)
    }
}
