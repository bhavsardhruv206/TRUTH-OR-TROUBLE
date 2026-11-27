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
) {
    private val eligible: List<Card> = cards.filter {
        it.category == mode && it.intensity <= intensity && it.language == language
    }
    private val remaining = ArrayDeque<Card>()
    private val seen = mutableSetOf<String>()

    val size: Int get() = eligible.size
    val isEmpty: Boolean get() = eligible.isEmpty()

    /** Next card, or null when no card matches the filters. */
    fun draw(): Card? {
        if (eligible.isEmpty()) return null
        if (remaining.isEmpty()) {
            val lastShown = seen.lastOrNull()
            remaining.addAll(eligible.shuffled(random))
            // avoid showing the same card twice in a row across a reshuffle
            if (remaining.size > 1 && remaining.first().id == lastShown) remaining.addLast(remaining.removeFirst())
            seen.clear()
        }
        val card = remaining.removeFirst()
        seen.add(card.id)
        return card
    }
}
