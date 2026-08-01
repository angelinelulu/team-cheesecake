package com.teamcheesecake.doomscrollpet

import android.content.Context
import android.media.MediaPlayer
import androidx.annotation.RawRes

/**
 * Singleton wrapper around a looping [MediaPlayer] for the app's background music.
 *
 * - [init] once, early (MainActivity.onCreate), with application context.
 * - [play] whenever music should (re)start — e.g. on successful sign-in, or when
 *   the app returns to the foreground. Safe to call repeatedly; it's a no-op if
 *   already playing.
 * - [pause] when the app goes to the background.
 * - [release] when the Activity is truly finishing, to free native resources.
 */
object AudioManager {

    private var mediaPlayer: MediaPlayer? = null
    private var muted = false

    fun init(context: Context, @RawRes resId: Int = R.raw.background_music) {
        if (mediaPlayer != null) return // already initialized, don't leak a second player

        mediaPlayer = MediaPlayer.create(context.applicationContext, resId)?.also {
            it.isLooping = true
            it.setVolume(volumeLevel(), volumeLevel())
        }
    }

    fun play() {
        val player = mediaPlayer ?: return
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

    fun setMuted(isMuted: Boolean) {
        muted = isMuted
        mediaPlayer?.setVolume(volumeLevel(), volumeLevel())
    }

    fun isMuted(): Boolean = muted

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun volumeLevel(): Float = if (muted) 0f else 0.5f
}