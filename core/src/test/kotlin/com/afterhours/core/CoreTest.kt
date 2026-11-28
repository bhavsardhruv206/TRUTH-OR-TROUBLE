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
        assertEquals(30, counts[GameMode.NEVER_HAVE_I_EVER])
        assertEquals(30, counts[GameMode.MOST_LIKELY_TO])
        assertEquals(30, counts[GameMode.HOT_SEAT])
        assertEquals(60, counts[GameMode.CLOSER])
        assertEquals(45, counts[GameMode.KNOW_ME])
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
            val names = if (mode.couplesOnly) listOf("Ann", "Bo") else listOf("Ann", "Bo", "Cy")
            val s = GameSession(cards, mode, Intensity.WILD, names, totalRounds = 40, random = Random(9))
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
        assertTrue(timed.all { it.category in setOf(GameMode.DARE, GameMode.HOT_SEAT, GameMode.CLOSER) && it.timerSeconds!! in 5..120 })
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

    @Test fun newModesFollowTheirFormat() {
        allCards.filter { it.category == GameMode.NEVER_HAVE_I_EVER }.forEach { assertTrue(it.text.startsWith("Never have I ever"), it.id) }
        allCards.filter { it.category == GameMode.MOST_LIKELY_TO && it.language == "en" }.forEach { assertTrue(it.text.startsWith("Who is most likely to"), it.id) }
        allCards.filter { it.category == GameMode.HOT_SEAT }.forEach {
            assertTrue(it.text.startsWith("Hot seat: {name}"), it.id)
            assertEquals(60, it.timerSeconds, it.id)
        }
    }

    @Test fun knowMeCardsAreAboutTheAnswerer() {
        val knowMe = allCards.filter { it.category == GameMode.KNOW_ME }
        knowMe.forEach { assertTrue("{name}" in it.text && "{other}" !in it.text && it.timerSeconds == null, it.id) }
        Intensity.entries.forEach { i -> assertEquals(15, knowMe.count { it.language == "en" && it.intensity == i }, "en $i") }
    }

    @Test fun knowMePointsGoToEachCorrectGuesser() {
        val s = GameSession(cards, GameMode.KNOW_ME, Intensity.BOLD, listOf("Ann", "Bo", "Cy"), totalRounds = 3, random = Random(5), chaosEnabled = false)
        assertEquals(GameMode.KNOW_ME, s.state.card!!.category)
        val pts = s.state.card!!.intensity.points
        // Ann answers; Bo and Cy guess right. Ann's own index is ignored.
        s.completeGuesses(setOf(0, 1, 2))
        assertEquals(listOf(0, pts, pts), s.state.players.map { it.score })
        assertEquals(0, s.state.skips)
        // Bo answers; nobody gets it: no points, but not a skip.
        s.completeGuesses(emptySet())
        assertEquals(listOf(0, pts, pts), s.state.players.map { it.score })
        assertEquals(0, s.state.skips)
        // Cy answers; Ann guesses right.
        val p3 = s.state.card!!.intensity.points
        s.completeGuesses(setOf(0))
        assertEquals(p3, s.state.players[0].score)
        assertTrue(s.state.finished)
    }

    @Test fun wheelLandsOnRequestedSlice() {
        val random = Random(11)
        for (segments in 1..8) {
            var rotation = 0f
            repeat(50) {
                val target = random.nextInt(segments)
                val next = Wheel.targetRotation(rotation, segments, target, fullTurns = 3, offset = Wheel.randomOffset(random))
                assertTrue(next - rotation >= 3 * 360f, "spins at least the full turns")
                assertTrue(next - rotation < 4 * 360f, "never more than one extra turn")
                assertEquals(target, Wheel.indexAt(next, segments), "segments=$segments")
                rotation = next
            }
        }
        assertEquals(0, Wheel.indexAt(0f, 4)) // slice 0 starts at the pointer
        assertEquals(3, Wheel.indexAt(10f, 4)) // turning clockwise brings the last slice under it
    }

    @Test fun fairPickerSpreadsTurnsAndNeverRepeats() {
        val picker = FairPicker(5, Random(4))
        val picks = List(50) { picker.next() }
        picks.zipWithNext().forEach { (a, b) -> assertNotEquals(a, b) }
        assertTrue(picks.groupingBy { it }.eachCount().values.all { it == 10 })
        assertEquals(listOf(0, 0, 0), FairPicker(1, Random(1)).let { p -> List(3) { p.next() } })
    }

    @Test fun spinSessionMixesTruthAndDareAndPicksPlayersFairly() {
        val names = listOf("Ann", "Bo", "Cy")
        val s = GameSession(cards, GameMode.COUPLE, Intensity.WILD, names, totalRounds = 30, random = Random(5), chaosEnabled = false, spin = true)
        assertTrue(s.state.spin)
        val categories = mutableListOf<GameMode>()
        val turns = mutableListOf<Int>()
        while (!s.state.finished) {
            categories += s.state.card!!.category
            turns += s.state.currentPlayerIndex
            s.complete()
        }
        assertEquals(setOf(GameMode.TRUTH, GameMode.DARE), categories.toSet())
        turns.zipWithNext().forEach { (a, b) -> assertNotEquals(a, b) }
        assertEquals(listOf(10, 10, 10), names.indices.map { i -> turns.count { it == i } })
        // points went to whoever the wheel picked
        assertTrue(s.state.players.all { it.score > 0 })
        assertTrue(s.drawnCardIds.all { id -> cards.first { it.id == id }.category in GameSession.SPIN_MODES })
    }

    @Test fun closerIsForExactlyTwoPartners() {
        assertEquals(listOf(GameMode.CLOSER), GameMode.entries.filter { it.couplesOnly })
        assertIs<PlayerRules.Result.Invalid>(PlayerRules.validate(listOf("A", "B", "C"), GameMode.CLOSER))
        assertIs<PlayerRules.Result.Invalid>(PlayerRules.validate(listOf("A"), GameMode.CLOSER))
        assertIs<PlayerRules.Result.Valid>(PlayerRules.validate(listOf("A", "B"), GameMode.CLOSER))
        assertIs<PlayerRules.Result.Valid>(PlayerRules.validate(listOf("A", "B", "C"), GameMode.TRUTH))
        assertFailsWith<IllegalArgumentException> {
            GameSession(cards, GameMode.CLOSER, Intensity.CHILL, listOf("A", "B", "C"))
        }
    }

    @Test fun closerSessionAlternatesPartnersWithoutGroupChaos() {
        CardLanguage.entries.forEach { lang ->
            Intensity.entries.forEach { level ->
                val s = GameSession(allCards, GameMode.CLOSER, level, listOf("Ann", "Bo"), totalRounds = 20, random = Random(3), language = lang.code)
                var turn = 0
                while (!s.state.finished) {
                    assertNull(s.state.chaos, "no group chaos in a couples game")
                    val card = s.state.card!!
                    assertEquals(GameMode.CLOSER, card.category)
                    assertTrue(card.intensity <= level)
                    assertEquals(lang.code, card.language)
                    assertEquals(turn % 2, s.state.currentPlayerIndex)
                    assertFalse(s.state.cardText!!.contains("{"))
                    s.complete(); turn++
                }
            }
        }
    }

    @Test fun closerCardsArePerIntensityAndMarkedSuggestiveAboveChill() {
        val closer = allCards.filter { it.category == GameMode.CLOSER }
        Intensity.entries.forEach { i -> assertEquals(20, closer.count { it.language == "en" && it.intensity == i }, "en $i") }
        assertTrue(closer.filter { it.intensity == Intensity.CHILL }.all { it.safety == SafetyClass.SAFE })
        assertTrue(closer.filter { it.intensity != Intensity.CHILL }.all { it.safety == SafetyClass.MILD_SUGGESTIVE })
    }

    @Test fun everyCloserCardHasAValidPoseIllustration() {
        val poses = PoseLibrary.all
        assertEquals(poses.size, poses.map { it.id }.toSet().size)
        poses.forEach { pose ->
            assertTrue(pose.title.isNotBlank() && pose.hint.isNotBlank(), pose.id)
            listOf(pose.pink, pose.blue).forEach { fig ->
                assertEquals(Pose.JOINTS.toSet(), fig.keys, pose.id)
                fig.values.forEach { (x, y) -> assertTrue(x in 0f..Pose.WIDTH && y in 0f..Pose.HEIGHT, "${pose.id} out of bounds") }
            }
        }
        allCards.filter { it.category == GameMode.CLOSER }.forEach { assertNotNull(PoseLibrary.get(it.pose), "${it.id} pose ${it.pose}") }
        assertTrue(allCards.filter { it.category != GameMode.CLOSER }.all { it.pose == null })
    }
}
