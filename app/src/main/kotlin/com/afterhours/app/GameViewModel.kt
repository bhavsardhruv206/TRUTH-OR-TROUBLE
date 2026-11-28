package com.afterhours.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.afterhours.app.data.Prefs
import com.afterhours.app.data.Settings
import com.afterhours.core.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SetupState(
    val mode: GameMode = GameMode.TRUTH,
    /** Spin the wheel: a wheel picks the player and Truth or Dare each turn. */
    val spin: Boolean = false,
    val intensity: Intensity = Intensity.CHILL,
    val language: CardLanguage = CardLanguage.ENGLISH,
    val names: List<String> = listOf("", ""),
    val rounds: Int = GameSession.DEFAULT_ROUNDS,
    val error: String? = null,
)

class GameViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = Prefs(app)
    private val cards by lazy { BundledCardRepository().allCards() }
    private var session: GameSession? = null
    private var reported: Set<String> = emptySet()
    private var seen: Set<String> = emptySet()
    private var rememberedSession: GameSession? = null

    /** null while loading, then true/false. */
    val ageConfirmed: StateFlow<Boolean?> = prefs.ageConfirmed
        .map<Boolean, Boolean?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** null while loading, then true/false. */
    val tutorialSeen: StateFlow<Boolean?> = prefs.tutorialSeen
        .map<Boolean, Boolean?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val settings: StateFlow<Settings> = prefs.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, Settings())

    /** Mirrors the "Remove ads" purchase; cached so it also holds offline. */
    val adsRemoved: StateFlow<Boolean> = prefs.adsRemoved
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setAdsRemoved(owned: Boolean) = viewModelScope.launch { prefs.setAdsRemoved(owned) }

    private val _setup = MutableStateFlow(SetupState())
    val setup: StateFlow<SetupState> = _setup

    private val _game = MutableStateFlow<GameState?>(null)
    val game: StateFlow<GameState?> = _game

    init {
        viewModelScope.launch {
            val names = prefs.savedNames()
            val lang = CardLanguage.entries.firstOrNull { it.code == prefs.language() } ?: CardLanguage.ENGLISH
            seen = prefs.seenCards()
            _setup.update { s ->
                s.copy(
                    names = if (names.size >= PlayerRules.MIN_PLAYERS && s.names.all { it.isBlank() }) names.take(PlayerRules.MAX_PLAYERS) else s.names,
                    language = lang,
                )
            }
        }
        viewModelScope.launch { prefs.reportedCards.collect { reported = it } }
    }

    fun confirmAge() = viewModelScope.launch { prefs.setAgeConfirmed() }
    fun finishTutorial() = viewModelScope.launch { prefs.setTutorialSeen() }

    fun setSound(on: Boolean) = viewModelScope.launch { prefs.setSound(on) }
    fun setHaptics(on: Boolean) = viewModelScope.launch { prefs.setHaptics(on) }
    fun setChaos(on: Boolean) = viewModelScope.launch { prefs.setChaos(on) }
    fun resetSeenCards() = viewModelScope.launch { seen = emptySet(); prefs.clearSeenCards() }

    fun selectMode(m: GameMode) = _setup.update {
        // Couples-only games are for exactly two partners: keep the first two names.
        val names = if (m.couplesOnly) (it.names + List(PlayerRules.COUPLE_PLAYERS) { "" }).take(PlayerRules.COUPLE_PLAYERS) else it.names
        it.copy(mode = m, spin = false, names = names, error = null)
    }
    private val couplesOnly: Boolean get() = _setup.value.let { !it.spin && it.mode.couplesOnly }
    fun selectSpin() = _setup.update { it.copy(spin = true) }
    fun selectIntensity(i: Intensity) = _setup.update { it.copy(intensity = i) }
    fun selectRounds(r: Int) = _setup.update { it.copy(rounds = r) }
    fun selectLanguage(l: CardLanguage) {
        _setup.update { it.copy(language = l) }
        viewModelScope.launch { prefs.setLanguage(l.code) }
    }

    fun setName(index: Int, name: String) = _setup.update { s ->
        s.copy(names = s.names.toMutableList().also { it[index] = name.take(PlayerRules.MAX_NAME_LENGTH) }, error = null)
    }

    fun addPlayer() = _setup.update { s ->
        if (!couplesOnly && s.names.size < PlayerRules.MAX_PLAYERS) s.copy(names = s.names + "") else s
    }

    fun removePlayer(index: Int) = _setup.update { s ->
        if (s.names.size > PlayerRules.MIN_PLAYERS) s.copy(names = s.names.filterIndexed { i, _ -> i != index }) else s
    }

    /** Returns true when the game started. */
    fun startGame(): Boolean = startSession(_setup.value.rounds)

    /** Bonus game: same players and settings, with extra rounds. */
    fun playBonus(): Boolean = startSession(_setup.value.rounds + BONUS_ROUNDS)

    private fun startSession(rounds: Int): Boolean {
        rememberSeen()
        val s = _setup.value
        return when (val v = PlayerRules.validate(s.names, if (s.spin) null else s.mode)) {
            is PlayerRules.Result.Invalid -> { _setup.update { it.copy(error = v.reason) }; false }
            is PlayerRules.Result.Valid -> {
                val pool = cards.filter { it.id !in reported }
                session = try {
                    GameSession(
                        pool, s.mode, s.intensity, v.names, rounds,
                        playedBefore = seen,
                        chaosEnabled = settings.value.chaos,
                        language = s.language.code,
                        spin = s.spin,
                    )
                } catch (e: IllegalArgumentException) {
                    _setup.update { it.copy(error = "No cards left for this mode and intensity.") }
                    return false
                }
                _game.value = session!!.state
                viewModelScope.launch { prefs.saveNames(v.names) }
                true
            }
        }
    }

    fun complete() { session?.let { it.complete(); publish(it) } }
    fun completeGuesses(correct: Set<Int>) { session?.let { it.completeGuesses(correct); publish(it) } }
    fun skip() { session?.let { it.skip(); publish(it) } }
    fun dismissChaos() { session?.let { it.dismissChaos(); _game.value = it.state } }

    /** Hides the current card on this device for good and skips it without penalty. */
    fun reportCurrentCard() {
        val id = session?.state?.card?.id ?: return
        reported = reported + id
        viewModelScope.launch { prefs.addReportedCard(id) }
        skip()
    }

    private fun publish(s: GameSession) {
        _game.value = s.state
        if (s.state.finished) rememberSeen()
    }

    /** Folds the cards of the current game into the cross-game memory. */
    private fun rememberSeen() {
        val s = session ?: return
        if (rememberedSession === s) return
        rememberedSession = s
        val updated = SeenCardMemory.update(seen, s.drawnCardIds, s.eligibleCardIds)
        if (updated == seen) return
        val added = updated - seen
        val forgotten = seen - updated
        seen = updated
        viewModelScope.launch {
            if (forgotten.isNotEmpty()) { prefs.clearSeenCards(); prefs.addSeenCards(updated) } else prefs.addSeenCards(added)
        }
    }

    /** Same players and settings, fresh deck and zeroed scores. */
    fun playAgain() { startGame() }

    fun endGame() { rememberSeen(); session = null; _game.value = null }

    private companion object { const val BONUS_ROUNDS = 5 }
}
