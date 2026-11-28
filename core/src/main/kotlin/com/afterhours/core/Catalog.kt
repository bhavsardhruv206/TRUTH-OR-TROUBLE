package com.afterhours.core

/** The mood sections of the Games screen, in the order they are shown. */
enum class Mood { PARTY, FUN, DEEP, COUPLES, SPICY }

/**
 * Every game the Games screen offers, grouped by [mood].
 * [mode] is the card mode it plays; null for Spin the Wheel (Truth or Dare each turn)
 * and for the love-language quiz, which has its own screen.
 */
enum class GameId(
    val mood: Mood,
    val mode: GameMode?,
    val icon: String,
    /** Spin the Wheel: a wheel picks the player, then Truth or Dare. */
    val spin: Boolean = false,
    /** Fixed intensity: the game plays exactly these cards and skips the difficulty choice. */
    val lockedIntensity: Intensity? = null,
) {
    // Party: loud, quick, everyone involved.
    SPIN(Mood.PARTY, null, "🎡", spin = true),
    DARE(Mood.PARTY, GameMode.DARE, "🔥"),
    CHARADES(Mood.PARTY, GameMode.CHARADES, "🎭"),
    HOT_SEAT(Mood.PARTY, GameMode.HOT_SEAT, "⏱️"),
    MOST_LIKELY_TO(Mood.PARTY, GameMode.MOST_LIKELY_TO, "👉"),

    // Fun: light choices and quick laughs.
    THIS_OR_THAT(Mood.FUN, GameMode.THIS_OR_THAT, "🔀"),
    WOULD_YOU_RATHER(Mood.FUN, GameMode.WOULD_YOU_RATHER, "⚖️"),
    NEVER_HAVE_I_EVER(Mood.FUN, GameMode.NEVER_HAVE_I_EVER, "✋"),

    // Deep talk: get to know each other.
    TRUTH(Mood.DEEP, GameMode.TRUTH, "💬"),
    KNOW_ME(Mood.DEEP, GameMode.KNOW_ME, "🧠"),
    LOVE_QUIZ(Mood.DEEP, null, "💌"),

    // Romance and couples.
    COUPLE(Mood.COUPLES, GameMode.COUPLE, "💞"),
    COUPLE_BINGO(Mood.COUPLES, GameMode.COUPLE_BINGO, "🎯"),
    CLOSER(Mood.COUPLES, GameMode.CLOSER, "🌹"),

    // 18+ Spicy: Wild cards only, behind an extra check that everyone playing is an adult.
    SPICY_SPIN(Mood.SPICY, null, "🌶️", spin = true, lockedIntensity = Intensity.WILD),
    SPICY_NEVER(Mood.SPICY, GameMode.NEVER_HAVE_I_EVER, "😈", lockedIntensity = Intensity.WILD),
    SPICY_COUPLE(Mood.SPICY, GameMode.COUPLE, "🔥", lockedIntensity = Intensity.WILD);

    /** 18+ content: labelled, behind the adults check, and never shown next to an ad. */
    val spicy: Boolean get() = mood == Mood.SPICY

    /** Games for exactly two partners. */
    val couplesOnly: Boolean get() = mode?.couplesOnly == true

    /** The love-language quiz is a short questionnaire, not a card game. */
    val quiz: Boolean get() = this == LOVE_QUIZ

    companion object {
        fun inMood(m: Mood): List<GameId> = entries.filter { it.mood == m }

        /** Games that open with [players] people. Couples games need exactly two. */
        fun playableWith(g: GameId, players: Int): Boolean = !g.couplesOnly || players == PlayerRules.COUPLE_PLAYERS
    }
}

/** Wild cards, and every 18+ Spicy game, count as adult content. */
fun isAdultContent(game: GameId?, intensity: Intensity): Boolean = game?.spicy == true || intensity == Intensity.WILD

/**
 * The "Suggested tonight" row on Home: three games that fit the evening.
 * Weekend nights lean to party games, late nights to deep talk, two players to couples games.
 * Never suggests 18+ games, and the pick is stable for a given evening so it doesn't jump around.
 */
object Tonight {
    const val COUNT = 3

    /**
     * [dayOfWeek] 1 = Monday ... 7 = Sunday (ISO). [hour] 0..23 local time.
     * [players] is the last player count, or null when nobody has played yet.
     * [daySeed] changes once a day (for example the epoch day) to rotate the picks.
     */
    fun suggest(dayOfWeek: Int, hour: Int, players: Int?, daySeed: Long): List<GameId> {
        val weekendNight = (dayOfWeek == 5 || dayOfWeek == 6) && (hour >= 18 || hour < 3)
        val lateNight = hour >= 22 || hour < 5
        val couple = players == PlayerRules.COUPLE_PLAYERS
        val moods = when {
            couple && lateNight -> listOf(Mood.COUPLES, Mood.DEEP, Mood.COUPLES)
            couple -> listOf(Mood.COUPLES, Mood.FUN, Mood.DEEP)
            weekendNight -> listOf(Mood.PARTY, Mood.PARTY, Mood.FUN)
            lateNight -> listOf(Mood.DEEP, Mood.FUN, Mood.PARTY)
            hour < 17 -> listOf(Mood.FUN, Mood.PARTY, Mood.DEEP)
            else -> listOf(Mood.PARTY, Mood.FUN, Mood.DEEP)
        }
        val picked = mutableListOf<GameId>()
        moods.forEachIndexed { slot, mood ->
            val options = GameId.inMood(mood).filter { it !in picked && (players == null || GameId.playableWith(it, players)) }
            if (options.isNotEmpty()) picked += options[Math.floorMod(daySeed + slot * 7, options.size.toLong()).toInt()]
        }
        // Top up from light games if a mood had nothing left.
        GameId.entries.filter { !it.spicy && it !in picked && (players == null || GameId.playableWith(it, players)) }
            .forEach { if (picked.size < COUNT) picked += it }
        return picked.take(COUNT)
    }
}

/** "X% picked this" for voting cards. */
object PickStats {
    /** Percent for the chosen option: [pickA] for the first option, the rest for the second. */
    fun percentFor(pickA: Int, choseFirst: Boolean): Int = if (choseFirst) pickA.coerceIn(0, 100) else 100 - pickA.coerceIn(0, 100)
}
