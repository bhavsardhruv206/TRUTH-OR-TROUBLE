package com.afterhours.app.ui

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/** Quiet sound and vibration cues. Both can be switched off in Settings. */
class Feedback(private val haptic: HapticFeedback, private val tone: ToneGenerator?) {
    var soundOn = true
    var hapticsOn = true

    /** A new card was revealed. */
    fun reveal() {
        if (hapticsOn) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        if (soundOn) tone?.startTone(ToneGenerator.TONE_PROP_ACK, 60)
    }

    /** The spinning wheel passed a slice. Haptic only, so a long spin stays quiet. */
    fun tick() {
        if (hapticsOn) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    /** A choice was confirmed (done, skip, start). */
    fun tap() {
        if (hapticsOn) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    /** Something big: chaos moment, timer finished, winner. */
    fun alert() {
        if (hapticsOn) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        if (soundOn) tone?.startTone(ToneGenerator.TONE_PROP_BEEP2, 180)
    }
}

@Composable
fun rememberFeedback(soundOn: Boolean, hapticsOn: Boolean): Feedback {
    val haptic = LocalHapticFeedback.current
    val tone = remember {
        try { ToneGenerator(AudioManager.STREAM_MUSIC, 30) } catch (e: RuntimeException) { null }
    }
    DisposableEffect(tone) { onDispose { tone?.release() } }
    val feedback = remember(haptic, tone) { Feedback(haptic, tone) }
    feedback.soundOn = soundOn
    feedback.hapticsOn = hapticsOn
    return feedback
}
