package com.afterhours.core

import kotlin.random.Random

object PlayerRules {
    const val MIN_PLAYERS = 2
    const val MAX_PLAYERS = 8
    const val MAX_NAME_LENGTH = 16

    sealed interface Result {
        data class Valid(val names: List<String>) : Result
        data class Invalid(val reason: String) : Result
    }

    fun validate(rawNames: List<String>): Result {
        val names = rawNames.map { it.trim() }
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
) {
    private val deck = CardDeck(cards, mode, intensity, random)
    private val textRandom = random

    var state: GameState
        private set

    init {
        val valid = PlayerRules.validate(playerNames)
        require(valid is PlayerRules.Result.Valid) { (valid as PlayerRules.Result.Invalid).reason }
        require(totalRounds > 0) { "totalRounds must be positive" }
        require(!deck.isEmpty) { "No cards for $mode / $intensity" }
        val players0 = valid.names.mapIndexed { i, n -> Player(i, n) }
        val first = deck.draw()
        state = GameState(
            mode = mode,
            intensity = intensity,
            players = players0,
            currentPlayerIndex = 0,
            round = 1,
            totalRounds = totalRounds,
            card = first,
            cardText = first?.let { CardText.render(it.text, players0[0], players0, textRandom) },
            finished = false,
            skips = 0,
        )
    }

    /** Current player did the truth/dare/choice: award points, move on. */
    fun complete() = advance(award = true)

    /** Skip without any penalty. */
    fun skip() = advance(award = false)

    private fun advance(award: Boolean) {
        val s = state
        if (s.finished) return
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
            val nextIndex = (s.currentPlayerIndex + 1) % players.size
            val next = deck.draw()
            s.copy(
                players = players,
                currentPlayerIndex = nextIndex,
                round = s.round + 1,
                card = next,
                cardText = next?.let { CardText.render(it.text, players[nextIndex], players, textRandom) },
                skips = skips,
            )
        }
    }

    companion object {
        const val DEFAULT_ROUNDS = 10
        val ROUND_OPTIONS = listOf(8, 10, 15, 20)
    }
}
