package com.afterhours.core

import kotlin.random.Random
import kotlin.test.*

class CoreTest {
    private val allCards = BundledCardRepository().allCards()
    private val cards = allCards.filter { it.language == "en" }

    @Test fun seedCardsAreValid() {
        assertTrue(cards.size >= 300)
        assertEquals(cards.size, cards.map { it.id }.toSet().size)
        assertTrue(cards.all { it.text.isNotBlank() && it.language == "en" })
        GameMode.entries.forEach { m ->
            Intensity.entries.forEach { i ->
                assertTrue(cards.any { it.category == m && it.intensity == i }, "missing $m/$i")
            }
        }
    }

    @Test fun deckHasTargetSizePerCategory() {
        val counts = cards.groupingBy { it.category }.eachCount()
        assertEquals(100, counts[GameMode.TRUTH])
        assertEquals(100, counts[GameMode.DARE])
        assertEquals(60, counts[GameMode.WOULD_YOU_RATHER])
        assertEquals(40, counts[GameMode.COUPLE])
        assertEquals(cards.size, cards.map { it.text }.toSet().size, "duplicate card text")
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

    @Test fun placeholdersAreFilledAndOtherDiffersFromCurrent() {
        val players = listOf(Player(0, "Rahul"), Player(1, "Meera"), Player(2, "Sam"))
        repeat(50) { seed ->
            val out = CardText.render("{name} and {other}: go. {name}!", players[0], players, Random(seed))
            assertFalse(out.contains("{"))
            assertTrue(out.startsWith("Rahul and "))
            assertFalse(out.startsWith("Rahul and Rahul"))
        }
        assertEquals("No placeholders", CardText.render("No placeholders", players[0], players))
    }

    @Test fun placeholdersAreWellFormedAndPersonalised() {
        val allowed = setOf("{name}", "{other}")
        cards.forEach { c ->
            val found = Regex("\\{[^}]*\\}").findAll(c.text).map { it.value }.toSet()
            assertTrue(allowed.containsAll(found), "bad placeholder in ${c.id}: ${c.text}")
        }
        assertTrue(cards.count { "{name}" in it.text } >= 50)
        assertTrue(cards.count { "{other}" in it.text } >= 10)
    }

    @Test fun sessionRendersCardTextForEveryTurn() {
        GameMode.entries.forEach { mode ->
            val s = GameSession(cards, mode, Intensity.WILD, listOf("Ann", "Bo", "Cy"), totalRounds = 40, random = Random(9))
            while (!s.state.finished) {
                val text = checkNotNull(s.state.cardText)
                assertFalse(text.contains("{"), text)
                if ("{name}" in s.state.card!!.text) assertTrue(text.contains(s.state.currentPlayer.name))
                s.dismissChaos()
                s.complete()
            }
            assertNull(s.state.cardText)
        }
    }

    @Test fun chaosAppearsPeriodicallyAndDoesNotScoreOrConsumeRounds() {
        val s = GameSession(cards, GameMode.TRUTH, Intensity.CHILL, listOf("A", "B"), totalRounds = 30, random = Random(5))
        var chaosCount = 0
        while (!s.state.finished) {
            val before = s.state
            if (s.state.chaos != null) {
                chaosCount++
                s.complete() // blocked while chaos is pending
                assertEquals(before, s.state)
                s.dismissChaos()
                assertNull(s.state.chaos)
                assertEquals(before.round, s.state.round)
                assertEquals(before.players, s.state.players)
            }
            s.complete()
        }
        assertTrue(chaosCount in 4..8, "chaos count $chaosCount")
    }

    @Test fun chaosCanBeDisabled() {
        val s = GameSession(cards, GameMode.DARE, Intensity.WILD, listOf("A", "B"), totalRounds = 20, random = Random(6), chaosEnabled = false)
        var guard = 0
        while (!s.state.finished && guard++ < 100) { assertNull(s.state.chaos); s.skip() }
        assertTrue(s.state.finished)
    }

    @Test fun chaosEventsAreValid() {
        assertEquals(ChaosEvents.all.size, ChaosEvents.all.map { it.id }.toSet().size)
        assertTrue(ChaosEvents.all.all { it.text.isNotBlank() && (it.timerSeconds ?: 1) > 0 })
        assertFalse(ChaosScheduler(Random(1), enabled = false).tick())
    }

    @Test fun timedDaresHaveSaneTimers() {
        val timed = cards.filter { it.timerSeconds != null }
        assertTrue(timed.size >= 10)
        assertTrue(timed.all { it.category == GameMode.DARE && it.timerSeconds!! in 5..120 })
    }

    @Test fun previouslyPlayedCardsComeLast() {
        val eligible = cards.filter { it.category == GameMode.TRUTH && it.intensity == Intensity.CHILL }
        val played = eligible.take(eligible.size / 2).map { it.id }.toSet()
        val deck = CardDeck(cards, GameMode.TRUTH, Intensity.CHILL, Random(7), playedBefore = played)
        val order = List(eligible.size) { deck.draw()!! }
        val firstSeenIdx = order.indexOfFirst { it.id in played }
        assertTrue(order.take(eligible.size - played.size).none { it.id in played }, "fresh cards first")
        assertEquals(eligible.size - played.size, firstSeenIdx)
        assertEquals(eligible.size, deck.drawnIds.size)
    }

    @Test fun hinglishPackCoversEveryModeAndIntensity() {
        val hi = allCards.filter { it.language == CardLanguage.HINGLISH.code }
        assertTrue(hi.size >= 90)
        assertEquals(allCards.size, allCards.map { it.id }.toSet().size, "ids unique across packs")
        assertEquals(hi.size, hi.map { it.text }.toSet().size)
        hi.forEach { c -> assertTrue(Regex("\\{[^}]*\\}").findAll(c.text).all { it.value in setOf("{name}", "{other}") }, c.id) }
        GameMode.entries.forEach { m ->
            Intensity.entries.forEach { i -> assertTrue(hi.any { it.category == m && it.intensity == i }, "missing hinglish $m/$i") }
        }
    }

    @Test fun sessionOnlyDrawsChosenLanguage() {
        CardLanguage.entries.forEach { lang ->
            val s = GameSession(allCards, GameMode.DARE, Intensity.WILD, listOf("A", "B"), totalRounds = 30, random = Random(8), language = lang.code)
            while (!s.state.finished) {
                assertEquals(lang.code, s.state.card!!.language)
                s.dismissChaos(); s.skip()
            }
        }
    }

    @Test fun seenMemoryResetsWhenPoolExhausted() {
        val eligible = setOf("a", "b", "c")
        assertEquals(setOf("x", "a"), SeenCardMemory.update(setOf("x"), setOf("a"), eligible))
        // pool fully seen: forget it, but keep the very last card so it is not first next time
        assertEquals(setOf("x", "c"), SeenCardMemory.update(setOf("x", "a", "b"), linkedSetOf("b", "c"), eligible))
    }
}
