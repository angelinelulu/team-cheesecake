package com.teamcheesecake.doomscrollpet

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.util.Log
import androidx.annotation.RawRes

object AudioManager {

    private const val TAG = "AudioManager"

    // --- Background music (looping) ---
    private var mediaPlayer: MediaPlayer? = null
    private var musicMuted = false

    // --- Short sound effects (one-shot) ---
    private var soundPool: SoundPool? = null
    private var buttonTapSoundId: Int? = null
    private var buttonTapLoaded = false

    fun init(context: Context, @RawRes musicResId: Int = R.raw.background_music) {
        initMusic(context, musicResId)
        initSoundEffects(context)
    }

    private fun initMusic(context: Context, @RawRes resId: Int) {
        if (mediaPlayer != null) {
            Log.d(TAG, "initMusic() skipped — already initialized")
            return
        }

        val player = MediaPlayer.create(context.applicationContext, resId)
        if (player == null) {
            Log.e(TAG, "MediaPlayer.create() returned null — resource failed to load/decode")
            return
        }

        player.isLooping = true
        player.setVolume(musicVolumeLevel(), musicVolumeLevel())
        mediaPlayer = player
        Log.d(TAG, "initMusic() succeeded, player ready")
    }

    private fun initSoundEffects(context: Context) {
        if (soundPool != null) {
            Log.d(TAG, "initSoundEffects() skipped — already initialized")
            return
        }

        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val pool = SoundPool.Builder()
            .setMaxStreams(4) // allow a few overlapping taps without cutting each other off
            .setAudioAttributes(attributes)
            .build()

        pool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0 && sampleId == buttonTapSoundId) {
                buttonTapLoaded = true
                Log.d(TAG, "button_tap loaded successfully")
            } else {
                Log.e(TAG, "button_tap failed to load, status=$status")
            }
        }

        soundPool = pool
        buttonTapSoundId = pool.load(context.applicationContext, R.raw.button_tap, 1)
    }

    fun play() {
        val player = mediaPlayer
        if (player == null) {
            Log.w(TAG, "play() called but mediaPlayer is null — was init() called/did it succeed?")
            return
        }
        if (!player.isPlaying) {
            player.start()
        }
    }

    fun pause() {
        val player = mediaPlayer ?: return
        if (player.isPlaying) {
            player.pause()
        }
    }

    fun setMusicMuted(isMuted: Boolean) {
        musicMuted = isMuted
        mediaPlayer?.setVolume(musicVolumeLevel(), musicVolumeLevel())
    }

    fun isMusicMuted(): Boolean = musicMuted

    /** Plays the short button-tap sound effect. Safe to call rapidly/repeatedly. */
    fun playButtonTap() {
        val pool = soundPool
        val soundId = buttonTapSoundId
        if (pool == null || soundId == null || !buttonTapLoaded) {
            Log.w(TAG, "playButtonTap() skipped — not loaded yet")
            return
        }
        pool.play(soundId, 1f, 1f, /* priority = */ 1, /* loop = */ 0, /* rate = */ 1f)
    }

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
        soundPool?.release()
        soundPool = null
        buttonTapLoaded = false
    }

    private fun musicVolumeLevel(): Float = if (musicMuted) 0f else 0.5f
}