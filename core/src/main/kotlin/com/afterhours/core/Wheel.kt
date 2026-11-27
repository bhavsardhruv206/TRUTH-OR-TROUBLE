package com.afterhours.core

import kotlin.random.Random

/**
 * Geometry for the spin-the-wheel animation. The wheel has [segments] equal slices drawn
 * clockwise from 12 o'clock, and a fixed pointer sits at 12 o'clock. Rotating the wheel
 * clockwise by R degrees puts the slice at wheel angle (-R mod 360) under the pointer.
 */
object Wheel {
    fun sweep(segments: Int): Float {
        require(segments > 0) { "segments must be positive" }
        return 360f / segments
    }

    /** Index of the slice under the pointer when the wheel has turned [rotation] degrees clockwise. */
    fun indexAt(rotation: Float, segments: Int): Int {
        val underPointer = norm(-rotation)
        return (underPointer / sweep(segments)).toInt().coerceIn(0, segments - 1)
    }

    /**
     * Absolute rotation to animate to from [current] so the wheel spins clockwise at least
     * [fullTurns] whole turns and stops with slice [index] under the pointer.
     * [offset] in -0.5..0.5 shifts the landing point inside the slice so stops look natural.
     */
    fun targetRotation(current: Float, segments: Int, index: Int, fullTurns: Int = 4, offset: Float = 0f): Float {
        require(index in 0 until segments) { "index $index out of 0 until $segments" }
        val sw = sweep(segments)
        val o = offset.coerceIn(-0.4f, 0.4f)
        val landing = norm(-((index + 0.5f + o) * sw))
        val delta = norm(landing - norm(current))
        return current + fullTurns * 360f + delta
    }

    /** A random in-slice offset, kept away from the edges so the pointer never sits on a line. */
    fun randomOffset(random: Random): Float = random.nextFloat() * 0.7f - 0.35f

    private fun norm(deg: Float): Float = ((deg % 360f) + 360f) % 360f
}

/**
 * Picks who plays next in spin mode: a random player among those with the fewest turns so
 * far, never the same player twice in a row, so everyone gets an even share over a game.
 */
class FairPicker(private val playerCount: Int, private val random: Random) {
    private val turns = IntArray(playerCount)
    private var last = -1

    init { require(playerCount > 0) }

    fun next(): Int {
        val candidates = (0 until playerCount).filter { it != last || playerCount == 1 }
        val fewest = candidates.minOf { turns[it] }
        val pick = candidates.filter { turns[it] == fewest }.random(random)
        turns[pick]++
        last = pick
        return pick
    }
}
