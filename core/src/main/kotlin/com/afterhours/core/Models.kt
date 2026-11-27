package com.afterhours.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class GameMode(val label: String) {
    @SerialName("truth") TRUTH("Truth"),
    @SerialName("dare") DARE("Dare"),
    @SerialName("wyr") WOULD_YOU_RATHER("Would You Rather"),
    @SerialName("couple") COUPLE("Couple"),
}

@Serializable
enum class Intensity(val label: String, val points: Int) {
    @SerialName("chill") CHILL("Chill", 1),
    @SerialName("bold") BOLD("Bold", 2),
    @SerialName("wild") WILD("Wild", 3),
}

/** Content safety classification assigned during manual card review. */
@Serializable
enum class SafetyClass {
    @SerialName("safe") SAFE,
    @SerialName("mild_suggestive") MILD_SUGGESTIVE,
}

@Serializable
data class Card(
    val id: String,
    val text: String,
    val category: GameMode,
    val intensity: Intensity,
    val language: String = "en",
    val safety: SafetyClass = SafetyClass.SAFE,
)

data class Player(val id: Int, val name: String, val score: Int = 0)
