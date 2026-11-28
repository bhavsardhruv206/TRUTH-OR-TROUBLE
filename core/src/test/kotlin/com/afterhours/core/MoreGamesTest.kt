package com.afterhours.core

import com.afterhours.core.i18n.UiStrings
import kotlin.random.Random
import kotlin.test.*

class MoreGamesTest {
    private val allCards = BundledCardRepository().allCards()

    @Test fun everyCardModeIsInTheCatalogOnce() {
        val modes = GameId.entries.filter { !it.spicy }.mapNotNull { it.mode }
        assertEquals(GameMode.entries.toSet(), modes.toSet())
        assertEquals(modes.size, modes.toSet().size, "a mode appears twice outside 18+")
        assertEquals(Mood.entries.toSet(), GameId.entries.map { it.mood }.toSet(), "every mood has games")
        assertTrue(GameId.entries.filter { it.spicy }.all { it.lockedIntensity == Intensity.WILD })
        assertTrue(GameId.entries.filter { !it.spicy }.all { it.lockedIntensity == null })
        assertTrue(GameId.entries.filter { it.mode == null }.all { it.spin || it.quiz })
    }

    @Test fun adultContentIsSpicyGamesAndWildCards() {
        assertTrue(isAdultContent(GameId.SPICY_COUPLE, Intensity.WILD))
        assertTrue(isAdultContent(GameId.TRUTH, Intensity.WILD))
        assertFalse(isAdultContent(GameId.TRUTH, Intensity.BOLD))
        assertFalse(isAdultContent(null, Intensity.CHILL))
    }

    @Test fun tonightSuggestsThreeSafeDistinctPlayableGames() {
        for (day in 1..7) for (hour in 0..23) for (players in listOf(null, 2, 3, 6)) for (seed in 0L..6L) {
            val picks = Tonight.suggest(day, hour, players, seed)
            assertEquals(Tonight.COUNT, picks.size, "$day $hour $players")
            assertEquals(picks.size, picks.toSet().size)
            assertTrue(picks.none { it.spicy })
            if (players != null) assertTrue(picks.all { GameId.playableWith(it, players) }, "$picks for $players")
        }
        assertTrue(Tonight.suggest(6, 21, 5, 0).first().mood == Mood.PARTY, "Saturday night leads with a party game")
        assertTrue(Tonight.suggest(2, 20, 2, 0).first().mood == Mood.COUPLES, "two players lead with a couples game")
    }

    @Test fun spicyDecksOnlyDealWildCards() {
        val s = GameSession(allCards, GameMode.NEVER_HAVE_I_EVER, Intensity.WILD, listOf("A", "B", "C"), totalRounds = 20,
            random = Random(3), chaosEnabled = false, exactIntensity = true)
        repeat(19) { assertEquals(Intensity.WILD, s.state.card!!.intensity); s.complete() }
        val spin = GameSession(allCards, GameMode.TRUTH, Intensity.WILD, listOf("A", "B"), random = Random(1), spin = true,
            language = CardLanguage.GUJARATI.code, exactIntensity = true)
        assertEquals(Intensity.WILD, spin.state.card!!.intensity)
        assertEquals("gu", spin.state.card!!.language)
    }

    @Test fun votingCardsCarryPlausibleStats() {
        val votes = allCards.filter { it.category == GameMode.THIS_OR_THAT || it.category == GameMode.WOULD_YOU_RATHER }
        assertTrue(votes.isNotEmpty())
        votes.forEach { c -> assertTrue(c.isVote && c.pickA!! in 1..99, c.id) }
        allCards.filter { it.category == GameMode.THIS_OR_THAT }.forEach { c ->
            assertEquals(2, c.options?.size, c.id)
            c.options!!.forEach { o -> assertTrue(o.isNotBlank() && o in c.text, c.id) }
        }
        assertTrue(allCards.filter { it.category !in setOf(GameMode.THIS_OR_THAT, GameMode.WOULD_YOU_RATHER) }.none { it.isVote })
        assertEquals(62, PickStats.percentFor(62, choseFirst = true))
        assertEquals(38, PickStats.percentFor(62, choseFirst = false))
    }

    @Test fun charadesAreTimedActingCards() {
        allCards.filter { it.category == GameMode.CHARADES }.forEach { assertEquals(60, it.timerSeconds, it.id) }
    }

    @Test fun bingoDealsAFullBoardInEveryLanguage() {
        CardLanguage.entries.forEach { lang ->
            Intensity.entries.forEach { i ->
                val b = BingoBoard.deal(allCards, i, lang.code, Random(7))
                assertNotNull(b, "${lang.code} $i")
                assertEquals(BingoBoard.SQUARES, b.squares.size)
                assertEquals(BingoBoard.SQUARES, b.squares.map { it.id }.toSet().size)
                assertTrue(b.squares.all { it.language == lang.code && it.intensity <= i }, "${lang.code} $i")
            }
        }
    }

    @Test fun bingoLinesCount() {
        var b = BingoBoard.deal(allCards, Intensity.WILD, "en", Random(1))!!
        assertEquals(8, BingoBoard.LINES.size)
        b = b.toggle(0).toggle(1)
        assertEquals(0, b.bingos)
        b = b.toggle(2)
        assertEquals(1, b.bingos)
        b = b.toggle(4).toggle(8)
        assertEquals(2, b.bingos, "top row and a diagonal")
        b = b.toggle(4)
        assertEquals(1, b.bingos, "unticking breaks the diagonal")
        assertFalse(b.full)
    }

    @Test fun loveQuizPairsEveryLanguageEvenly() {
        val pairs = LoveQuiz.PAIRS.map { setOf(it.first, it.second) }
        assertEquals(10, pairs.toSet().size)
        LoveLanguage.entries.forEach { l -> assertEquals(4, LoveQuiz.PAIRS.count { it.first == l || it.second == l }) }
        CardLanguage.entries.forEach { lang ->
            val q = UiStrings.of(lang).loveQuizText
            assertEquals(10, q.questions.size)
            LoveLanguage.entries.forEach { assertTrue(q.name(it).isNotBlank() && q.description(it).isNotBlank(), lang.code) }
            q.questions.forEach { assertTrue(it.first.isNotBlank() && it.second.isNotBlank() && it.first != it.second) }
        }
    }

    @Test fun loveQuizScores() {
        val q = UiStrings.of(CardLanguage.ENGLISH).loveQuizText.questions
        // Always the first option: WORDS appears first in 3 pairs.
        val r = LoveQuiz.score(q, List(10) { true })
        assertEquals(10, r.total)
        assertEquals(LoveQuiz.PAIRS.count { it.first == LoveLanguage.WORDS }, r.counts[LoveLanguage.WORDS])
        // Every TIME option wherever TIME appears.
        val time = LoveQuiz.score(q, q.map { it.firstLanguage == LoveLanguage.TIME })
        assertEquals(listOf(LoveLanguage.TIME), time.top)
        assertEquals(40, time.percent(LoveLanguage.TIME))
        assertEquals(LoveLanguage.TIME, time.ranked.first())
    }
}
