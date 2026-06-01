package com.ebookreader.core.tts.service

import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager

/** Abstracción del foco de audio para poder testear la lógica sin el framework. */
internal interface AudioFocusManager {
    fun requestFocus(): Boolean
    fun abandonFocus()
}

/** Implementación real basada en AudioManager (minSdk 26, API de AudioFocusRequest). */
internal class AndroidAudioFocusManager(
    private val audioManager: AudioManager,
    private val listener: AudioManager.OnAudioFocusChangeListener
) : AudioFocusManager {

    private val request: AudioFocusRequest =
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setOnAudioFocusChangeListener(listener)
            .build()

    override fun requestFocus(): Boolean =
        audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED

    override fun abandonFocus() {
        audioManager.abandonAudioFocusRequest(request)
    }
}

/**
 * Decide pausar/reanudar la narración según el foco de audio.
 * - Pérdida permanente (LOSS): pausa definitiva, no se marca para reanudar.
 * - Pérdida transitoria (LOSS_TRANSIENT / _CAN_DUCK): pausa y marca para reanudar.
 *   No se hace ducking: bajar el volumen de una narración no tiene sentido.
 * - GAIN: reanuda solo si la pausa fue transitoria.
 */
internal class AudioFocusController(
    private val audioFocus: AudioFocusManager,
    private val onPause: () -> Unit,
    private val onResume: () -> Unit
) {
    private var pausedByFocusLoss = false

    fun onPlaybackStarting(): Boolean = audioFocus.requestFocus()

    fun onPlaybackStopping() {
        pausedByFocusLoss = false
        audioFocus.abandonFocus()
    }

    fun onFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                pausedByFocusLoss = false
                onPause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                pausedByFocusLoss = true
                onPause()
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (pausedByFocusLoss) {
                    pausedByFocusLoss = false
                    onResume()
                }
            }
        }
    }
}
