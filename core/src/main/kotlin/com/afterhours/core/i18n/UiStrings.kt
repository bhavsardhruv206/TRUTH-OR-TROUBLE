package com.afterhours.core.i18n

import com.afterhours.core.CardLanguage
import com.afterhours.core.ChaosEvent
import com.afterhours.core.GameMode
import com.afterhours.core.Intensity
import com.afterhours.core.PlayerRules
import com.afterhours.core.Pose

/** Short line and how-to for one game tile. */
class GameText(val blurb: String, val howTo: String)

class TutorialStep(val title: String, val body: String)

class PoseText(val title: String, val hint: String)

class ChaosText(val title: String, val text: String)

/**
 * Every piece of app text in one language: one language pack per [CardLanguage].
 * Constructor parameters have no defaults on purpose, so a pack that misses a string
 * does not compile. Card text lives in the cards_<code>.json packs instead.
 */
class UiStrings(
    // Common
    val back: String,
    val next: String,
    val skip: String,
    val done: String,
    val close: String,
    val cancel: String,
    val continueLabel: String,
    // Language
    val languageTitle: String,
    val languageBody: String,
    val language: String,
    // Age gate
    val adultsOnly: String,
    val ageGateBody: String,
    val ageConfirm: String,
    val enter: String,
    // Tutorial
    val tutorial: List<TutorialStep>,
    val startPlaying: String,
    // Home
    val homeTagline: (games: Int) -> String,
    val games: String,
    val spinTheWheel: String,
    val howToPlay: String,
    val settings: String,
    val helpSafety: String,
    val helpBody: String,
    // Settings
    val sound: String,
    val vibration: String,
    val chaosMoments: String,
    val chaosHint: String,
    val forgetPlayed: String,
    val playedForgotten: String,
    val privacyChoices: String,
    val adsRemoved: String,
    val removeAds: (price: String) -> String,
    val removeAdsUnavailable: String,
    val restorePurchase: String,
    // Games
    val gamesCount: (count: Int) -> String,
    val groupFeatured: String,
    val groupClassic: String,
    val groupWholeGroup: String,
    val groupCouples: String,
    val newBadge: String,
    /** Spin the Wheel is `null`. */
    val game: (GameMode?) -> GameText,
    // Couples gate
    val couplesGateTitle: String,
    val couplesGateBody: String,
    val couplesAgree: String,
    val notNow: String,
    // Difficulty
    val difficulty: String,
    val cardLanguage: String,
    val intensityDescription: (Intensity) -> String,
    val points: (Int) -> String,
    val nextPlayers: String,
    // Players
    val whosPlaying: String,
    val couplesHint: String,
    val playersHint: (count: Int) -> String,
    val partnerN: (Int) -> String,
    val playerN: (Int) -> String,
    val remove: String,
    val addPlayer: String,
    val rounds: String,
    val startGame: String,
    val problem: (PlayerRules.Problem) -> String,
    val noCardsLeft: String,
    // Game
    val roundOf: (round: Int, total: Int) -> String,
    val endGame: String,
    val yourTurn: String,
    val whatToDo: String,
    val reportCard: String,
    val donePoints: (Int) -> String,
    val endGameTitle: String,
    val scoresLost: String,
    val keepPlaying: String,
    val reportTitle: String,
    val reportBody: String,
    /** Same order as [EnglishStrings.reportReasons]; reports are sent in English. */
    val reportReasons: List<String>,
    // Spin the wheel
    val whosNext: String,
    val truthOrDare: (name: String) -> String,
    val spinToPick: String,
    val spinToFind: String,
    val spin: String,
    val spinning: String,
    val nextTruthOrDare: String,
    val showMyCard: String,
    // Timer
    val time: String,
    val startTimer: String,
    val pause: String,
    // Chaos
    val chaosMoment: String,
    val chaos: Map<String, ChaosText>,
    // Results
    val gameOver: String,
    val nobodyScored: String,
    val everyoneSafe: String,
    val sharedWin: String,
    val takesNight: String,
    val skipped: (Int) -> String,
    val playAgain: String,
    val home: String,
    val bonusGame: String,
    val watchAdBonus: String,
    // Players and count step
    val playGame: String,
    val howManyPlayers: String,
    val passPhone: (min: Int, max: Int) -> String,
    val playersWord: String,
    val countAllOpen: String,
    val countNoCloser: String,
    val nextChooseGame: String,
    val pickAGame: (players: Int) -> String,
    val twoPlayersOnly: String,
    val avatarHint: (players: Int) -> String,
    val profilePicture: String,
    // Theme and turn clock
    val theme: String,
    val themeHint: String,
    val themeAuto: String,
    val themeLight: String,
    val themeDark: String,
    val turnClock: String,
    val turnClockHint: (seconds: Int) -> String,
    // Stakes
    val streakLine: (streak: Int, bonus: Int) -> String,
    val showdown: String,
    val longestStreak: (name: String, streak: Int) -> String,
    val skipCost: (points: Int) -> String,
    // Content labels
    val mode: (GameMode) -> String,
    val intensity: (Intensity) -> String,
    val poses: Map<String, PoseText>,
) {
    fun chaosText(e: ChaosEvent): ChaosText = chaos[e.id] ?: ChaosText(e.title, e.text)
    fun pose(p: Pose): PoseText = poses[p.id] ?: PoseText(p.title, p.hint)

    companion object {
        fun of(language: CardLanguage): UiStrings = when (language) {
            CardLanguage.ENGLISH -> EnglishStrings
            CardLanguage.HINDI -> HindiStrings
            CardLanguage.MARATHI -> MarathiStrings
            CardLanguage.GUJARATI -> GujaratiStrings
            CardLanguage.SPANISH -> SpanishStrings
        }
    }
}
