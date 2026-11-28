package com.afterhours.core

import kotlin.random.Random

/**
 * A short whole-group moment dropped between turns. Awards no points and does not
 * use up a round. Everything here is harmless and can be skipped.
 */
data class ChaosEvent(val id: String, val title: String, val text: String, val timerSeconds: Int? = null)

object ChaosEvents {
    val all: List<ChaosEvent> = listOf(
        ChaosEvent("swap-seats", "Seat swap", "Everyone swap seats with the person across from them. Last one seated reads the next card out loud."),
        ChaosEvent("point-vote", "Point and vote", "On three, everyone points at the person most likely to be late to their own wedding. The most-pointed-at person takes a bow."),
        ChaosEvent("silent-round", "Silent round", "Nobody speaks until the timer ends. Anyone who talks has to tell the group a harmless secret.", 30),
        ChaosEvent("fist-five", "Fist to five", "Rate your day from zero to five fingers on three. Lowest rating explains why in one sentence."),
        ChaosEvent("compliment-chain", "Compliment chain", "Starting with the player on the left, each person gives the next one a sincere compliment.", 45),
        ChaosEvent("statue", "Freeze", "Everybody freeze in the pose you are in. Whoever moves first reads the next card in a dramatic voice.", 20),
        ChaosEvent("thumbs", "Thumbs war", "Two players picked by the group have a thumb war. Winner chooses who plays next.", 30),
        ChaosEvent("rhyme-chain", "Rhyme chain", "Go around the circle: each person says a word that rhymes with the last. First to stall or repeat loses.", 40),
        ChaosEvent("never-have", "Never have I ever", "Say one harmless 'never have I ever'. Everyone who has done it raises a hand.", 30),
        ChaosEvent("duel", "Quick-draw duel", "Two players stand back to back and turn on three. First to strike a silly pose wins the duel.", null),
        ChaosEvent("story", "One-word story", "Build a story together, one word each. Whoever ends it picks the next dare giver.", 40),
        ChaosEvent("lightning", "Lightning round", "Go around the circle fast: everyone answers 'What is your biggest pet peeve?' in three words or fewer. Hesitate and you're out.", 20),
        ChaosEvent("staring", "Staring contest", "The two players with the highest scores face off. First to blink or laugh reads the next card out loud.", 30),
        ChaosEvent("hot-potato", "Hot potato", "Pass the phone around the circle until the timer ends. Whoever holds it at zero answers the next truth the group picks.", 15),
        ChaosEvent("hands-up", "Hands up", "Everyone hold both hands up. Lower one when someone says something you relate to. The group decides the topic.", 30),
    )

    fun pick(random: Random, exclude: Set<String> = emptySet()): ChaosEvent =
        (all.filter { it.id !in exclude }.ifEmpty { all }).random(random)
}

/** Decides when the next chaos moment happens: every [minGap]..[maxGap] turns. */
class ChaosScheduler(
    private val random: Random,
    private val minGap: Int = 4,
    private val maxGap: Int = 6,
    private val enabled: Boolean = true,
) {
    private var turnsUntil = nextGap()
    private fun nextGap() = if (maxGap <= minGap) minGap else random.nextInt(minGap, maxGap + 1)

    /** Call once per upcoming turn; returns true when chaos should happen before it. */
    fun tick(): Boolean {
        if (!enabled) return false
        turnsUntil--
        if (turnsUntil <= 0) { turnsUntil = nextGap(); return true }
        return false
    }
}
