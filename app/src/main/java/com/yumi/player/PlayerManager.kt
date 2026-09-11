package com.yumi.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class PlayerManager(
    private val context: Context,
    private val playerView: PlayerView
) {

    private val player = ExoPlayer.Builder(context).build()

    init {
        playerView.player = player
    }

    fun play(uri: String) {
        val mediaItem = MediaItem.fromUri(uri)

        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    fun setSpeed(speed: Float) {
        player.setPlaybackSpeed(speed)
    }

    fun getSpeed(): Float {
        return player.playbackParameters.speed
    }

    fun seekForward(seconds: Long) {
        player.seekTo(player.currentPosition + seconds * 1000)
    }

    fun seekBackward(seconds: Long) {
        player.seekTo(
            (player.currentPosition - seconds * 1000).coerceAtLeast(0)
        )
    }

    fun release() {
        player.release()
    }
}
