package com.afterhours.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class GameMode(
    val label: String,
    /** Played by exactly two partners: the setup locks to 2 players and group chaos moments are off. */
    val couplesOnly: Boolean = false,
    /** The current player answers secretly and the others guess: points go to each correct guesser. */
    val guessing: Boolean = false,
) {
    @SerialName("truth") TRUTH("Truth"),
    @SerialName("dare") DARE("Dare"),
    @SerialName("wyr") WOULD_YOU_RATHER("Would You Rather"),
    @SerialName("couple") COUPLE("Couple"),
    @SerialName("nhie") NEVER_HAVE_I_EVER("Never Have I Ever"),
    @SerialName("mlt") MOST_LIKELY_TO("Most Likely To"),
    @SerialName("hotseat") HOT_SEAT("Hot Seat"),
    @SerialName("closer") CLOSER("Closer", couplesOnly = true),
    @SerialName("knowme") KNOW_ME("How Well Do You Know Me", guessing = true),
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
    /** Id of a [Pose] illustration shown on the card (couples games). */
    val pose: String? = null,
)

/**
 * Languages for the app and its card packs. Codes are BCP 47.
 * [label] is the language's own name, [englishName] helps people who share a phone.
 */
enum class CardLanguage(val code: String, val label: String, val englishName: String, val resource: String) {
    ENGLISH("en", "English", "English", "cards_en.json"),
    HINDI("hi", "हिन्दी", "Hindi", "cards_hi.json"),
    MARATHI("mr", "मराठी", "Marathi", "cards_mr.json"),
    GUJARATI("gu", "ગુજરાતી", "Gujarati", "cards_gu.json"),
    SPANISH("es", "Español", "Spanish", "cards_es.json");

    companion object {
        fun fromCode(code: String?): CardLanguage? = entries.firstOrNull { it.code == code }

        /** Best match for the phone's language, e.g. "hi" or "gu-IN". English when nothing fits. */
        fun forLocale(languageTag: String): CardLanguage {
            val lang = languageTag.substringBefore('-').substringBefore('_').lowercase()
            return entries.firstOrNull { it.code == lang } ?: ENGLISH
        }
    }
}

data class Player(
    val id: Int,
    val name: String,
    val score: Int = 0,
    /** Cards this player completed in a row; a skip or a timeout resets it. */
    val streak: Int = 0,
    /** Longest [streak] this game. */
    val bestStreak: Int = 0,
)

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
