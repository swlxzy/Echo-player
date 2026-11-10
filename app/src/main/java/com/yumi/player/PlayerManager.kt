
package com.yumi.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class PlayerManager(
    context: Context,
    private val playerView: PlayerView
) {

    private val player = ExoPlayer.Builder(
        context.applicationContext
    ).build()

    init {
        playerView.player = player
    }

    fun play(uri: String) {
        val mediaItem = MediaItem.fromUri(uri)

        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    fun pause() {
        player.pause()
    }

    fun resume() {
        player.play()
    }

    fun isPlaying(): Boolean {
        return player.isPlaying
    }

    fun getCurrentPosition(): Long {
        return player.currentPosition
    }

    fun getDuration(): Long {
        return player.duration.coerceAtLeast(0L)
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs.coerceAtLeast(0L))
    }

    fun setSpeed(speed: Float) {
        player.playbackParameters =
            PlaybackParameters(speed.coerceIn(0.5f, 2f))
    }

    fun getSpeed(): Float {
        return player.playbackParameters.speed
    }

    fun seekForward(seconds: Long) {
        player.seekTo(
            player.currentPosition + seconds * 1000
        )
    }

    fun seekBackward(seconds: Long) {
        player.seekTo(
            (player.currentPosition - seconds * 1000)
                .coerceAtLeast(0L)
        )
    }

    fun release() {
        playerView.player = null
        player.release()
    }
}
