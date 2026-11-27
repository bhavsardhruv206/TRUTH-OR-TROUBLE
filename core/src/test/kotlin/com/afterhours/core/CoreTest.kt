package com.afterhours.core

import kotlin.random.Random
import kotlin.test.*

class CoreTest {
    private val cards = BundledCardRepository().allCards()

    @Test fun seedCardsAreValid() {
        assertTrue(cards.size >= 60)
        assertEquals(cards.size, cards.map { it.id }.toSet().size)
        assertTrue(cards.all { it.text.isNotBlank() && it.language == "en" })
        GameMode.entries.forEach { m ->
            Intensity.entries.forEach { i ->
                assertTrue(cards.any { it.category == m && it.intensity == i }, "missing $m/$i")
            }
        }
    }

    @Test fun deckFiltersAndNeverRepeatsUntilExhausted() {
        val deck = CardDeck(cards, GameMode.TRUTH, Intensity.BOLD, Random(1))
        val drawn = List(deck.size) { deck.draw()!! }
        assertEquals(deck.size, drawn.map { it.id }.toSet().size)
        assertTrue(drawn.all { it.category == GameMode.TRUTH && it.intensity <= Intensity.BOLD })
        // after exhaustion it reshuffles and keeps going
        assertNotNull(deck.draw())
    }

    @Test fun chillExcludesBoldAndWild() {
        val deck = CardDeck(cards, GameMode.DARE, Intensity.CHILL, Random(2))
        repeat(deck.size * 2) { assertEquals(Intensity.CHILL, deck.draw()!!.intensity) }
    }

    @Test fun playerValidation() {
        assertIs<PlayerRules.Result.Invalid>(PlayerRules.validate(listOf("A")))
        assertIs<PlayerRules.Result.Invalid>(PlayerRules.validate(List(9) { "P$it" }))
        assertIs<PlayerRules.Result.Invalid>(PlayerRules.validate(listOf("A", "a")))
        assertIs<PlayerRules.Result.Invalid>(PlayerRules.validate(listOf("A", " ")))
        assertIs<PlayerRules.Result.Valid>(PlayerRules.validate(List(8) { "P$it" }))
        assertIs<PlayerRules.Result.Valid>(PlayerRules.validate(listOf("A", "B")))
    }

    @Test fun sessionScoringSkipAndFinish() {
        val s = GameSession(cards, GameMode.DARE, Intensity.CHILL, listOf("Ann", "Bo"), totalRounds = 4, random = Random(3))
        assertEquals("Ann", s.state.currentPlayer.name)
        s.complete()
        assertEquals(1, s.state.players[0].score)
        assertEquals("Bo", s.state.currentPlayer.name)
        s.skip()
        assertEquals(0, s.state.players[1].score) // no penalty, no points
        s.complete(); s.skip()
        assertTrue(s.state.finished)
        assertEquals(2, s.state.players[0].score)
        assertEquals(2, s.state.skips)
        assertEquals(listOf("Ann"), s.state.winners.map { it.name })
        s.complete() // no-op after finish
        assertEquals(2, s.state.players[0].score)
    }

    @Test fun intensityPointsAndNoWinnerWhenAllSkip() {
        val s = GameSession(cards, GameMode.TRUTH, Intensity.WILD, listOf("A", "B", "C"), totalRounds = 3, random = Random(4))
        repeat(3) { s.skip() }
        assertTrue(s.state.finished)
        assertTrue(s.state.winners.isEmpty())
    }

    @Test fun interstitialIsFrequencyCapped() {
        var now = 0L
        val policy = AdPolicy(sessionsPerInterstitial = 2, minIntervalMs = 100, clock = { now })
        assertFalse(policy.shouldShow(AdPlacement.SESSION_END_INTERSTITIAL)) // no session yet
        policy.onSessionCompleted()
        assertFalse(policy.shouldShow(AdPlacement.SESSION_END_INTERSTITIAL)) // 1st session
        policy.onSessionCompleted()
        assertTrue(policy.shouldShow(AdPlacement.SESSION_END_INTERSTITIAL)) // 2nd session
        policy.onInterstitialShown()
        policy.onSessionCompleted(); policy.onSessionCompleted() // 4th session, too soon
        now = 50
        assertFalse(policy.shouldShow(AdPlacement.SESSION_END_INTERSTITIAL))
        now = 150
        assertTrue(policy.shouldShow(AdPlacement.SESSION_END_INTERSTITIAL))
        assertTrue(policy.shouldShow(AdPlacement.HOME_BANNER))
        assertFalse(NoAds.shouldShow(AdPlacement.HOME_BANNER))
    }
}
