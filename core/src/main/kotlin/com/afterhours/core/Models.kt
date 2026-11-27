package com.afterhours.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class GameMode(val label: String) {
    @SerialName("truth") TRUTH("Truth"),
    @SerialName("dare") DARE("Dare"),
    @SerialName("wyr") WOULD_YOU_RATHER("Would You Rather"),
    @SerialName("couple") COUPLE("Couple"),
}

@Serializable
enum class Intensity(val label: String, val points: Int) {
    @SerialName("chill") CHILL("Chill", 1),
    @SerialName("bold") BOLD("Bold", 2),
    @SerialName("wild") WILD("Wild", 3),
}

/** Content safety classification assigned during manual card review. */
@Serializable
enum class SafetyClass {
    @SerialName("safe") SAFE,
    @SerialName("mild_suggestive") MILD_SUGGESTIVE,
}

@Serializable
data class Card(
    val id: String,
    val text: String,
    val category: GameMode,
    val intensity: Intensity,
    val language: String = "en",
    val safety: SafetyClass = SafetyClass.SAFE,
    /** When set, the card is a timed dare and the UI offers a countdown. */
    val timerSeconds: Int? = null,
)

/** Card languages. Codes are BCP 47; Hinglish is Hindi written in Latin script. */
enum class CardLanguage(val code: String, val label: String) {
    ENGLISH("en", "English"),
    HINGLISH("hi-Latn", "Hinglish"),
}

data class Player(val id: Int, val name: String, val score: Int = 0)

/**
 * Fills the personalisation placeholders in a card:
 * `{name}` is the player whose turn it is, `{other}` is a different, randomly chosen player.
 */
object CardText {
    const val NAME = "{name}"
    const val OTHER = "{other}"

    fun render(text: String, current: Player, players: List<Player>, random: kotlin.random.Random = kotlin.random.Random.Default): String {
        var out = text.replace(NAME, current.name)
        if (OTHER in out) {
            val others = players.filter { it.id != current.id }
            val other = if (others.isEmpty()) "someone" else others[random.nextInt(others.size)].name
            out = out.replace(OTHER, other)
        }
        return out
    }
}
