package com.afterhours.core

import kotlin.random.Random

/**
 * Couple Bingo: a 3 x 3 board of "we have..." squares. Partners tap every square that is true
 * for them; any full row, column or diagonal is a bingo. Pure logic so it is easy to test.
 */
class BingoBoard private constructor(val squares: List<Card>, val marked: Set<Int>) {
    val lines: List<List<Int>> get() = LINES.filter { line -> line.all { it in marked } }
    val bingos: Int get() = lines.size
    val full: Boolean get() = marked.size == squares.size

    fun toggle(index: Int): BingoBoard {
        require(index in squares.indices)
        return BingoBoard(squares, if (index in marked) marked - index else marked + index)
    }

    companion object {
        const val SIZE = 3
        const val SQUARES = SIZE * SIZE

        /** Rows, columns and both diagonals, as square indices. */
        val LINES: List<List<Int>> = buildList {
            for (r in 0 until SIZE) add(List(SIZE) { r * SIZE + it })
            for (c in 0 until SIZE) add(List(SIZE) { it * SIZE + c })
            add(List(SIZE) { it * SIZE + it })
            add(List(SIZE) { it * SIZE + (SIZE - 1 - it) })
        }

        /**
         * A new board for [intensity] (a ceiling, like card decks) in [language], falling back to
         * English when that language has too few squares. Returns null when there are not enough.
         */
        fun deal(cards: List<Card>, intensity: Intensity, language: String, random: Random = Random.Default): BingoBoard? {
            fun pool(lang: String) = cards.filter { it.category == GameMode.COUPLE_BINGO && it.language == lang && it.intensity <= intensity }
            val own = pool(language)
            val chosen = if (own.size >= SQUARES) own else pool(CardLanguage.ENGLISH.code)
            if (chosen.size < SQUARES) return null
            return BingoBoard(chosen.shuffled(random).take(SQUARES), emptySet())
        }
    }
}
