package com.afterhours.core

/** The five love languages the quiz sorts answers into. */
enum class LoveLanguage { WORDS, TIME, GIFTS, ACTS, TOUCH }

/** One forced choice: which of two moments makes you feel more loved. */
class QuizQuestion(val first: String, val firstLanguage: LoveLanguage, val second: String, val secondLanguage: LoveLanguage)

/** The quiz in one language. [questions] pairs every love language with every other one once. */
class LoveQuizText(
    val questions: List<QuizQuestion>,
    val names: Map<LoveLanguage, String>,
    val descriptions: Map<LoveLanguage, String>,
) {
    fun name(l: LoveLanguage): String = names.getValue(l)
    fun description(l: LoveLanguage): String = descriptions.getValue(l)
}

/** Scores a finished quiz. Pure logic so it is easy to test. */
object LoveQuiz {
    /** The pairs every language pack must follow, in order, so packs score the same way. */
    val PAIRS: List<Pair<LoveLanguage, LoveLanguage>> = with(LoveLanguage.entries) {
        listOf(
            this[0] to this[1], this[2] to this[3], this[4] to this[0], this[1] to this[2], this[3] to this[4],
            this[0] to this[2], this[1] to this[3], this[2] to this[4], this[3] to this[0], this[4] to this[1],
        )
    }

    /** Builds a question list from ten (first, second) option texts in [PAIRS] order. */
    fun questions(options: List<Pair<String, String>>): List<QuizQuestion> {
        require(options.size == PAIRS.size) { "The quiz has ${PAIRS.size} questions" }
        return options.zip(PAIRS) { (a, b), (la, lb) -> QuizQuestion(a, la, b, lb) }
    }

    class Result(val counts: Map<LoveLanguage, Int>) {
        val total: Int get() = counts.values.sum()
        /** Highest count; on a tie, every tied language, in [LoveLanguage] order. */
        val top: List<LoveLanguage> get() {
            val best = counts.values.maxOrNull() ?: 0
            return LoveLanguage.entries.filter { (counts[it] ?: 0) == best }
        }
        fun percent(l: LoveLanguage): Int = if (total == 0) 0 else Math.round(100f * (counts[l] ?: 0) / total)
        /** Love languages from most to least picked. */
        val ranked: List<LoveLanguage> get() = LoveLanguage.entries.sortedByDescending { counts[it] ?: 0 }
    }

    /** [choseFirst] holds one answer per question: true when the first option was picked. */
    fun score(questions: List<QuizQuestion>, choseFirst: List<Boolean>): Result {
        require(choseFirst.size == questions.size)
        val counts = LoveLanguage.entries.associateWith { 0 }.toMutableMap()
        questions.zip(choseFirst).forEach { (q, first) ->
            val l = if (first) q.firstLanguage else q.secondLanguage
            counts[l] = counts.getValue(l) + 1
        }
        return Result(counts)
    }
}
