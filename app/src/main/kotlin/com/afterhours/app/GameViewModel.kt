package com.afterhours.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.afterhours.app.data.Prefs
import com.afterhours.core.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SetupState(
    val mode: GameMode = GameMode.TRUTH,
    val intensity: Intensity = Intensity.CHILL,
    val names: List<String> = listOf("", ""),
    val rounds: Int = GameSession.DEFAULT_ROUNDS,
    val error: String? = null,
)

class GameViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = Prefs(app)
    private val cards by lazy { BundledCardRepository().allCards() }
    private var session: GameSession? = null

    /** null while loading, then true/false. */
    val ageConfirmed: StateFlow<Boolean?> = prefs.ageConfirmed
        .map<Boolean, Boolean?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _setup = MutableStateFlow(SetupState())
    val setup: StateFlow<SetupState> = _setup

    private val _game = MutableStateFlow<GameState?>(null)
    val game: StateFlow<GameState?> = _game

    fun confirmAge() = viewModelScope.launch { prefs.setAgeConfirmed() }

    fun selectMode(m: GameMode) = _setup.update { it.copy(mode = m) }
    fun selectIntensity(i: Intensity) = _setup.update { it.copy(intensity = i) }
    fun selectRounds(r: Int) = _setup.update { it.copy(rounds = r) }

    fun setName(index: Int, name: String) = _setup.update { s ->
        s.copy(names = s.names.toMutableList().also { it[index] = name.take(PlayerRules.MAX_NAME_LENGTH) }, error = null)
    }

    fun addPlayer() = _setup.update { s ->
        if (s.names.size < PlayerRules.MAX_PLAYERS) s.copy(names = s.names + "") else s
    }

    fun removePlayer(index: Int) = _setup.update { s ->
        if (s.names.size > PlayerRules.MIN_PLAYERS) s.copy(names = s.names.filterIndexed { i, _ -> i != index }) else s
    }

    /** Returns true when the game started. */
    fun startGame(): Boolean = startSession(_setup.value.rounds)

    /** Rewarded-ad bonus: same players and settings, with extra rounds. */
    fun playBonus(): Boolean = startSession(_setup.value.rounds + BONUS_ROUNDS)

    private fun startSession(rounds: Int): Boolean {
        val s = _setup.value
        return when (val v = PlayerRules.validate(s.names)) {
            is PlayerRules.Result.Invalid -> { _setup.update { it.copy(error = v.reason) }; false }
            is PlayerRules.Result.Valid -> {
                session = GameSession(cards, s.mode, s.intensity, v.names, rounds)
                _game.value = session!!.state
                true
            }
        }
    }

    fun complete() { session?.let { it.complete(); _game.value = it.state } }
    fun skip() { session?.let { it.skip(); _game.value = it.state } }

    /** Same players and settings, fresh deck and zeroed scores. */
    fun playAgain() { startGame() }

    fun endGame() { session = null; _game.value = null }

    private companion object { const val BONUS_ROUNDS = 5 }
}
