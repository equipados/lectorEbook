package com.ebookreader.core.tts.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.session.MediaButtonReceiver
import com.ebookreader.core.tts.controller.TtsController
import com.ebookreader.core.tts.model.NowPlayingMetadata
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class TtsPlaybackService : Service() {

    @Inject
    lateinit var ttsController: TtsController

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var wakeLockController: PlaybackWakeLockController? = null
    private var audioFocusController: AudioFocusController? = null
    private lateinit var mediaSession: MediaSessionCompat

    // Caché del bitmap de portada para no decodificarlo en cada cambio de frase.
    private var cachedCoverPath: String? = null
    private var cachedCoverBitmap: Bitmap? = null

    companion object {
        const val CHANNEL_ID = "tts_playback"
        const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        wakeLockController = createWakeLockController()
        audioFocusController = createAudioFocusController()
        createNotificationChannel()
        initMediaSession()
        observeTtsState()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Botones de hardware (cascos bluetooth) llegan como ACTION_MEDIA_BUTTON.
        MediaButtonReceiver.handleIntent(mediaSession, intent)

        val initialState = ttsController.state.value
        startForeground(NOTIFICATION_ID, buildNotification(initialState.isPlaying))
        return START_STICKY
    }

    override fun onDestroy() {
        wakeLockController?.onServiceStopping()
        audioFocusController?.onPlaybackStopping()
        mediaSession.isActive = false
        mediaSession.release()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun initMediaSession() {
        mediaSession = MediaSessionCompat(this, "TtsPlaybackService").apply {
            setCallback(mediaSessionCallback)
            isActive = true
        }
    }

    private val mediaSessionCallback = object : MediaSessionCompat.Callback() {
        override fun onPlay() {
            serviceScope.launch {
                if (audioFocusController?.onPlaybackStarting() != false) {
                    ttsController.play()
                }
            }
        }

        override fun onPause() {
            serviceScope.launch { ttsController.pause() }
        }

        override fun onStop() {
            serviceScope.launch {
                ttsController.stop()
                audioFocusController?.onPlaybackStopping()
                wakeLockController?.onServiceStopping()
                mediaSession.setPlaybackState(
                    PlaybackStateCompat.Builder()
                        .setState(
                            PlaybackStateCompat.STATE_STOPPED,
                            PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
                            1.0f
                        )
                        .build()
                )
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        override fun onSkipToNext() {
            serviceScope.launch { ttsController.nextChapter() }
        }

        override fun onSkipToPrevious() {
            serviceScope.launch { ttsController.previousChapter() }
        }
    }

    private fun observeTtsState() {
        serviceScope.launch {
            ttsController.state.collectLatest { state ->
                wakeLockController?.onPlaybackStateChanged(state.isPlaying)
                updatePlaybackState(state.isPlaying)
                val manager = getSystemService(NotificationManager::class.java)
                manager.notify(NOTIFICATION_ID, buildNotification(state.isPlaying))
            }
        }
        serviceScope.launch {
            ttsController.nowPlaying.collectLatest { meta ->
                updateMetadata(meta)
                val manager = getSystemService(NotificationManager::class.java)
                manager.notify(NOTIFICATION_ID, buildNotification(ttsController.state.value.isPlaying))
            }
        }
    }

    private fun updatePlaybackState(isPlaying: Boolean) {
        val playState = if (isPlaying) {
            PlaybackStateCompat.STATE_PLAYING
        } else {
            PlaybackStateCompat.STATE_PAUSED
        }
        val state = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_PLAY_PAUSE or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackStateCompat.ACTION_STOP
            )
            .setState(playState, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1.0f)
            .build()
        mediaSession.setPlaybackState(state)
    }

    private suspend fun updateMetadata(meta: NowPlayingMetadata) {
        val cover = loadCover(meta.coverPath)
        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, meta.chapterTitle)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, meta.author)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, meta.bookTitle)
            .apply { if (cover != null) putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, cover) }
            .build()
        mediaSession.setMetadata(metadata)
    }

    private suspend fun loadCover(path: String?): Bitmap? {
        if (path.isNullOrBlank()) return null
        if (path == cachedCoverPath) return cachedCoverBitmap
        val bitmap = withContext(Dispatchers.IO) {
            runCatching {
                val file = File(path)
                if (file.exists()) BitmapFactory.decodeFile(path) else null
            }.getOrNull()
        }
        cachedCoverPath = path
        cachedCoverBitmap = bitmap
        return bitmap
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "TTS Playback",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Controls for text-to-speech playback" }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun createWakeLockController(): PlaybackWakeLockController? {
        val powerManager = getSystemService(PowerManager::class.java) ?: return null
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "$packageName:TtsPlaybackCpu"
        ).apply { setReferenceCounted(false) }
        return PlaybackWakeLockController(AndroidCpuWakeLock(wakeLock))
    }

    private fun createAudioFocusController(): AudioFocusController? {
        val audioManager = getSystemService(AudioManager::class.java) ?: return null
        lateinit var controller: AudioFocusController
        val focusManager = AndroidAudioFocusManager(audioManager) { focusChange ->
            controller.onFocusChange(focusChange)
        }
        controller = AudioFocusController(
            audioFocus = focusManager,
            onPause = { serviceScope.launch { ttsController.pause() } },
            onResume = { serviceScope.launch { ttsController.play() } }
        )
        return controller
    }

    private fun buildNotification(isPlaying: Boolean): Notification {
        val meta = ttsController.nowPlaying.value

        val playPauseAction = mediaAction(
            if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
            if (isPlaying) "Pause" else "Play",
            if (isPlaying) PlaybackStateCompat.ACTION_PAUSE else PlaybackStateCompat.ACTION_PLAY
        )
        val prevAction = mediaAction(
            android.R.drawable.ic_media_previous, "Anterior",
            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
        )
        val nextAction = mediaAction(
            android.R.drawable.ic_media_next, "Siguiente",
            PlaybackStateCompat.ACTION_SKIP_TO_NEXT
        )
        val stopIntent = MediaButtonReceiver.buildMediaButtonPendingIntent(
            this, PlaybackStateCompat.ACTION_STOP
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(meta.bookTitle.ifBlank { "Lectura Audible" })
            .setContentText(meta.chapterTitle.ifBlank { if (isPlaying) "Reproduciendo" else "En pausa" })
            .setLargeIcon(cachedCoverBitmap)
            .setOnlyAlertOnce(true)
            .setOngoing(isPlaying)
            .setDeleteIntent(stopIntent)
            .addAction(prevAction)
            .addAction(playPauseAction)
            .addAction(nextAction)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
                    .setShowCancelButton(true)
                    .setCancelButtonIntent(stopIntent)
            )
            .build()
    }

    private fun mediaAction(icon: Int, title: String, action: Long): NotificationCompat.Action {
        val intent = MediaButtonReceiver.buildMediaButtonPendingIntent(this, action)
        return NotificationCompat.Action(icon, title, intent)
    }
}
