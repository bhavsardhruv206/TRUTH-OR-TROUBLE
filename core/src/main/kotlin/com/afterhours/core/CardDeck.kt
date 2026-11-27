package com.afterhours.core

import kotlin.random.Random

/**
 * Draws random cards for one mode and intensity ceiling without repeating
 * until every eligible card has been shown (then the deck reshuffles).
 *
 * Intensity is a ceiling: Chill = Chill only, Bold = Chill + Bold, Wild = all.
 */
class CardDeck(
    cards: List<Card>,
    val mode: GameMode,
    val intensity: Intensity,
    private val random: Random = Random.Default,
    val language: String = "en",
    /** Card ids shown in earlier games; unseen cards are served first. */
    private val playedBefore: Set<String> = emptySet(),
) {
    private val eligible: List<Card> = cards.filter {
        it.category == mode && it.intensity <= intensity && it.language == language
    }
    private val remaining = ArrayDeque<Card>()
    private val seen = mutableSetOf<String>()
    private var firstFill = true
    private val drawn = linkedSetOf<String>()

    /** Ids drawn by this deck so far (this game only). */
    val drawnIds: Set<String> get() = drawn

    val size: Int get() = eligible.size
    val eligibleIds: Set<String> by lazy { eligible.map { it.id }.toSet() }
    val isEmpty: Boolean get() = eligible.isEmpty()

    /** Next card, or null when no card matches the filters. */
    fun draw(): Card? {
        if (eligible.isEmpty()) return null
        if (remaining.isEmpty()) {
            val lastShown = seen.lastOrNull()
            if (firstFill && playedBefore.isNotEmpty()) {
                val (old, fresh) = eligible.partition { it.id in playedBefore }
                remaining.addAll(fresh.shuffled(random))
                remaining.addAll(old.shuffled(random))
            } else {
                remaining.addAll(eligible.shuffled(random))
            }
            firstFill = false
            // avoid showing the same card twice in a row across a reshuffle
            if (remaining.size > 1 && remaining.first().id == lastShown) remaining.addLast(remaining.removeFirst())
            seen.clear()
        }
        val card = remaining.removeFirst()
        seen.add(card.id)
        drawn.add(card.id)
        return card
    }
}

/** Remembers which cards a group has already seen, across games. */
object SeenCardMemory {
    /**
     * Adds this game's [drawn] cards to [previous]. Once every card in the [eligible]
     * pool has been seen, that pool is forgotten so the cycle starts fresh.
     */
    fun update(previous: Set<String>, drawn: Set<String>, eligible: Set<String>): Set<String> {
        val merged = previous + drawn
        return if (eligible.isNotEmpty() && merged.containsAll(eligible)) merged - eligible + drawn.takeLast1() else merged
    }

    private fun Set<String>.takeLast1(): Set<String> = lastOrNull()?.let { setOf(it) } ?: emptySet()
}
