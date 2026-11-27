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
        data class Invalid(val reason: String) : Result
    }

    /** [mode] adds that game's own player rules, such as exactly two players for couples-only games. */
    fun validate(rawNames: List<String>, mode: GameMode? = null): Result {
        val names = rawNames.map { it.trim() }
        if (mode?.couplesOnly == true && names.size != COUPLE_PLAYERS) return Result.Invalid("${mode.label} is for exactly $COUPLE_PLAYERS players: you and your partner.")
        if (names.size < MIN_PLAYERS) return Result.Invalid("Add at least $MIN_PLAYERS players.")
        if (names.size > MAX_PLAYERS) return Result.Invalid("At most $MAX_PLAYERS players can play.")
        if (names.any { it.isEmpty() }) return Result.Invalid("Every player needs a nickname.")
        if (names.any { it.length > MAX_NAME_LENGTH }) return Result.Invalid("Nicknames can be up to $MAX_NAME_LENGTH characters.")
        if (names.map { it.lowercase() }.toSet().size != names.size) return Result.Invalid("Nicknames must be different.")
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
) {
    val currentPlayer: Player get() = players[currentPlayerIndex]
    val scoreboard: List<Player> get() = players.sortedByDescending { it.score }
    /** Everyone sharing the top score (empty if nobody scored). */
    val winners: List<Player>
        get() {
            val top = players.maxOf { it.score }
            return if (top == 0) emptyList() else players.filter { it.score == top }
        }
}

/**
 * One pass-the-phone game. Pure logic, no Android dependencies.
 * Completing a card awards the intensity's points; skipping awards and costs nothing.
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
        )
    }

    private fun drawCard(): Card? = decks.getValue(if (modes.size == 1) modes[0] else modes.random(textRandom)).draw()

    /** Current player did the truth/dare/choice: award points, move on. */
    fun complete() = advance(award = true)

    /** Skip without any penalty. */
    fun skip() = advance(award = false)

    /** Closes the pending chaos moment and goes on to the turn. */
    fun dismissChaos() { if (state.chaos != null) state = state.copy(chaos = null) }

    private fun advance(award: Boolean) {
        val s = state
        if (s.finished || s.chaos != null) return
        val card = s.card
        val players = if (award && card != null) {
            s.players.mapIndexed { i, p ->
                if (i == s.currentPlayerIndex) p.copy(score = p.score + card.intensity.points) else p
            }
        } else s.players
        val skips = if (award) s.skips else s.skips + 1
        state = if (s.round >= s.totalRounds) {
            s.copy(players = players, card = null, cardText = null, finished = true, skips = skips)
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
            )
        }
    }

    companion object {
        const val DEFAULT_ROUNDS = 10
        val ROUND_OPTIONS = listOf(8, 10, 15, 20)
        /** The wheel chooses between these each turn. */
        val SPIN_MODES = listOf(GameMode.TRUTH, GameMode.DARE)
    }
}
