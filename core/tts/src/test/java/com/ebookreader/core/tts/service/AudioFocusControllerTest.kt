package com.ebookreader.core.tts.service

import android.media.AudioManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioFocusControllerTest {

    private fun controller(focus: FakeAudioFocusManager, sink: CallSink) =
        AudioFocusController(
            audioFocus = focus,
            onPause = { sink.pauseCalls++ },
            onResume = { sink.resumeCalls++ }
        )

    @Test
    fun pideFocoAlEmpezarReproduccion() {
        val focus = FakeAudioFocusManager(granted = true)
        val sink = CallSink()
        val c = controller(focus, sink)

        val granted = c.onPlaybackStarting()

        assertTrue(granted)
        assertEquals(1, focus.requestCalls)
    }

    @Test
    fun pausaYReanudaTrasPerdidaTransitoria() {
        val focus = FakeAudioFocusManager(granted = true)
        val sink = CallSink()
        val c = controller(focus, sink)
        c.onPlaybackStarting()

        c.onFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
        c.onFocusChange(AudioManager.AUDIOFOCUS_GAIN)

        assertEquals(1, sink.pauseCalls)
        assertEquals(1, sink.resumeCalls)
    }

    @Test
    fun noReanudaTrasPerdidaPermanente() {
        val focus = FakeAudioFocusManager(granted = true)
        val sink = CallSink()
        val c = controller(focus, sink)
        c.onPlaybackStarting()

        c.onFocusChange(AudioManager.AUDIOFOCUS_LOSS)
        c.onFocusChange(AudioManager.AUDIOFOCUS_GAIN)

        assertEquals(1, sink.pauseCalls)
        assertEquals(0, sink.resumeCalls)
    }

    @Test
    fun abandonaFocoAlParar() {
        val focus = FakeAudioFocusManager(granted = true)
        val sink = CallSink()
        val c = controller(focus, sink)
        c.onPlaybackStarting()

        c.onPlaybackStopping()

        assertEquals(1, focus.abandonCalls)
    }

    private class CallSink {
        var pauseCalls = 0
        var resumeCalls = 0
    }

    private class FakeAudioFocusManager(private val granted: Boolean) : AudioFocusManager {
        var requestCalls = 0
        var abandonCalls = 0
        override fun requestFocus(): Boolean { requestCalls++; return granted }
        override fun abandonFocus() { abandonCalls++ }
    }
}
