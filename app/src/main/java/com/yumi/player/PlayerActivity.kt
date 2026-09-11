package com.yumi.player

import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.ui.PlayerView

class PlayerActivity : AppCompatActivity() {

    private lateinit var playerView: PlayerView
    private lateinit var playerManager: PlayerManager
    private lateinit var gestureDetector: GestureDetector

    private var seekSeconds = 5L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        playerView = findViewById(R.id.playerView)

        playerManager = PlayerManager(
            this,
            playerView
        )

        val videoUri = intent.getStringExtra("video_uri")

        if (videoUri != null) {
            playerManager.play(videoUri)
        }

        setupGestures()
    }

    private fun setupGestures() {
        gestureDetector = GestureDetector(
            this,
            object : GestureDetector.SimpleOnGestureListener() {

                override fun onDown(event: MotionEvent): Boolean {
                    return true
                }

                override fun onDoubleTap(event: MotionEvent): Boolean {
                    val screenWidth = playerView.width

                    if (event.x < screenWidth / 2f) {
                        playerManager.seekBackward(seekSeconds)
                    } else {
                        playerManager.seekForward(seekSeconds)
                    }

                    return true
                }

                override fun onLongPress(event: MotionEvent) {
                    playerManager.setSpeed(2f)
                }
            }
        )

        playerView.setOnTouchListener { _, event ->

            when (event.action) {
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    playerManager.setSpeed(1f)
                }
            }

            gestureDetector.onTouchEvent(event)
            true
        }
    }

    override fun onDestroy() {
        playerManager.release()
        super.onDestroy()
    }
}
