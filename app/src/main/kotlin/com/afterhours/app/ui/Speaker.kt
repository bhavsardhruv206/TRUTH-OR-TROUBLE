package com.afterhours.app.ui

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.util.Locale

/**
 * Reads cards out loud with the phone's own text-to-speech voice. Works offline when the
 * phone has the voice installed; nothing is sent anywhere by the app.
 */
class Speaker(context: Context) {
    /** True once the phone's speech engine is ready. Stays false on phones without one. */
    var available by mutableStateOf(false)
        private set

    /** Key of the text being read right now, or null when quiet. */
    var speakingKey by mutableStateOf<String?>(null)
        private set

    private var pending: Triple<String, String, String>? = null

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
            available = true
            pending?.let { (key, text, lang) -> speak(key, text, lang) }
        }
        pending = null
    }.apply {
        setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) = finished(utteranceId)
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) = finished(utteranceId)
            override fun onStop(utteranceId: String?, interrupted: Boolean) = finished(utteranceId)
        })
    }

    // Listener callbacks arrive on a binder thread; snapshot state writes are safe there.
    private fun finished(id: String?) { if (id == speakingKey) speakingKey = null }

    /**
     * Reads [text] in the card language [languageCode] (for example "en" or "hi").
     * [key] identifies the text so the screen can show which card is being read.
     */
    fun speak(key: String, text: String, languageCode: String) {
        if (!available) { pending = Triple(key, text, languageCode); return }
        tts.language = voiceFor(languageCode)
        speakingKey = key
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, key)
    }

    /** Starts reading, or stops if this text is already being read. */
    fun toggle(key: String, text: String, languageCode: String) {
        if (speakingKey == key) stop() else speak(key, text, languageCode)
    }

    fun stop() {
        pending = null
        if (speakingKey != null) { speakingKey = null; tts.stop() }
    }

    fun shutdown() { tts.stop(); tts.shutdown() }

    /** Picks the closest installed voice, falling back to the phone's own language. */
    private fun voiceFor(code: String): Locale {
        val wanted = when (code) {
            "en" -> Locale.forLanguageTag("en-IN").takeIf { usable(it) } ?: Locale.ENGLISH
            else -> Locale.forLanguageTag(code)
        }
        return when {
            usable(wanted) -> wanted
            usable(Locale(wanted.language)) -> Locale(wanted.language)
            else -> Locale.getDefault()
        }
    }

    private fun usable(locale: Locale): Boolean =
        tts.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE
}

@Composable
fun rememberSpeaker(): Speaker {
    val context = LocalContext.current
    val speaker = remember { Speaker(context) }
    DisposableEffect(speaker) { onDispose { speaker.shutdown() } }
    return speaker
}

/** What the game screen needs to read cards: the voice, the card language and the auto-read setting. */
class CardVoice(val speaker: Speaker, val languageCode: String, val autoRead: Boolean)

/** Speaker button on a card. Tap to hear it, tap again to stop. Hidden if the phone has no voice. */
@Composable
fun SpeakerButton(voice: CardVoice, key: String, text: String, languageCode: String = voice.languageCode) {
    val S = LocalStrings.current
    val speaker = voice.speaker
    if (!speaker.available) return
    val speaking = speaker.speakingKey == key
    IconButton(
        { speaker.toggle(key, text, languageCode) },
        Modifier.size(40.dp).semantics { contentDescription = if (speaking) S.stopReading else S.readCardAloud },
    ) {
        SpeakerIcon(if (speaking) Champagne else Stone, waves = if (speaking) 2 else 1)
    }
}

/** A small speaker drawn in code, so no icon library is needed. */
@Composable
private fun SpeakerIcon(color: Color, waves: Int) {
    Canvas(Modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        val body = Path().apply {
            moveTo(w * .08f, h * .38f)
            lineTo(w * .28f, h * .38f)
            lineTo(w * .52f, h * .16f)
            lineTo(w * .52f, h * .84f)
            lineTo(w * .28f, h * .62f)
            lineTo(w * .08f, h * .62f)
            close()
        }
        drawPath(body, color)
        val stroke = Stroke(width = w * .08f, cap = StrokeCap.Round)
        for (i in 1..waves) {
            val r = w * (.14f + .16f * i)
            drawArc(
                color, startAngle = -45f, sweepAngle = 90f, useCenter = false,
                topLeft = Offset(w * .46f - r, h * .5f - r), size = Size(r * 2, r * 2), style = stroke,
            )
        }
    }
}
