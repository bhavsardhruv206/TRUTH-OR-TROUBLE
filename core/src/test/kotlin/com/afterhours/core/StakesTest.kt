package com.afterhours.core

import kotlin.random.Random
import kotlin.test.*

class StakesTest {
    private val cards = BundledCardRepository().allCards().filter { it.language == "en" }

    private fun session(mode: GameMode = GameMode.TRUTH, rounds: Int = 8, names: List<String> = listOf("Ann", "Bo")) =
        GameSession(cards, mode, Intensity.CHILL, names, totalRounds = rounds, random = Random(7), chaosEnabled = false, stakes = true)

    @Test fun streakBonusGrowsAndCaps() {
        assertEquals(listOf(0, 0, 1, 2, 2, 2), (0..5).map { Stakes.streakBonus(it) })
        assertEquals(1, Stakes.completePoints(Intensity.CHILL, 1, showdown = false, stakes = true))
        assertEquals(4, Stakes.completePoints(Intensity.WILD, 2, showdown = false, stakes = true))
        assertEquals(8, Stakes.completePoints(Intensity.WILD, 3, showdown = true, stakes = true))
        assertEquals(3, Stakes.completePoints(Intensity.WILD, 3, showdown = true, stakes = false))
    }

    @Test fun completingInARowEarnsBonusAndSkipBreaksIt() {
        val s = session(rounds = 12)
        // Ann completes three of her turns in a row, Bo skips.
        s.complete(); s.skip()          // Ann 1, Bo 0
        s.complete(); s.skip()          // Ann 1+2 = 3
        s.complete()                    // Ann 3+3 = 6
        val ann = s.state.players[0]
        assertEquals(6, ann.score)
        assertEquals(3, ann.streak)
        assertEquals(0, s.state.players[1].score, "skip never goes below zero")
        s.skip()                        // Bo skips
        s.skip()                        // Ann skips: -1, streak reset
        assertEquals(5, s.state.players[0].score)
        assertEquals(0, s.state.players[0].streak)
        assertEquals(3, s.state.players[0].bestStreak)
        assertEquals(-1, s.state.lastTurn!!.points)
    }

    @Test fun finalRoundsScoreDouble() {
        val s = session(rounds = 8)
        assertEquals(2, s.state.showdownRounds)
        val showdownFlags = mutableListOf<Boolean>()
        while (!s.state.finished) { showdownFlags += s.state.showdown; s.skipFree() }
        assertEquals(listOf(false, false, false, false, false, false, true, true), showdownFlags)
        val t = session(rounds = 4)
        repeat(3) { t.skipFree() }
        assertTrue(t.state.showdown)
        assertEquals(2, t.state.pointsOnOffer)
    }

    @Test fun timeoutCostsAPointAndIsMarked() {
        val s = session()
        s.complete(); s.complete()     // Ann 1, Bo 1
        s.timeOut()                    // Ann 0
        assertEquals(0, s.state.players[0].score)
        assertTrue(s.state.lastTurn!!.timedOut)
        assertEquals(1, s.state.skips)
    }

    @Test fun freeSkipAndCouplesGamesNeverCost() {
        val s = session()
        s.complete(); s.complete()
        s.skipFree()
        assertEquals(1, s.state.players[0].score)
        val c = session(mode = GameMode.CLOSER)
        c.complete(); c.complete()
        assertEquals(0, c.state.skipCost)
        c.skip()
        assertEquals(1, c.state.players[0].score)
    }

    @Test fun takingTheLeadIsCalledOut() {
        val s = session()
        s.complete()
        assertTrue(s.state.lastTurn!!.tookLead)
        s.complete()                   // Bo ties, not a sole lead
        assertFalse(s.state.lastTurn!!.tookLead)
        s.complete()                   // Ann streak 2: +2
        assertTrue(s.state.lastTurn!!.tookLead)
    }

    @Test fun stakesOffKeepsClassicScoring() {
        val s = GameSession(cards, GameMode.TRUTH, Intensity.CHILL, listOf("A", "B"), totalRounds = 6, random = Random(1), chaosEnabled = false)
        s.complete(); s.skip(); s.complete(); s.skip(); s.complete()
        assertEquals(3, s.state.players[0].score)
        assertNull(s.state.lastTurn)
        assertFalse(s.state.showdown)
    }
}
