
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
    private var videoUri: String? = null

    private val preferences by lazy {
        getSharedPreferences("yumi_settings", MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        playerView = findViewById(R.id.playerView)

        playerManager = PlayerManager(
            this,
            playerView
        )

        videoUri = intent.getStringExtra("video_uri")

        videoUri?.let { uri ->
            playerManager.play(uri)

            val savedPosition = preferences.getLong(
                "position_$uri",
                0L
            )

            if (savedPosition > 0L) {
                playerManager.seekTo(savedPosition)
            }
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
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (::gestureDetector.isInitialized) {
            when (event.action) {
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    playerManager.setSpeed(1f)
                }
            }

            gestureDetector.onTouchEvent(event)
        }

        return super.dispatchTouchEvent(event)
    }

    override fun onPause() {
        super.onPause()

        videoUri?.let { uri ->
            preferences.edit()
                .putLong(
                    "position_$uri",
                    playerManager.getCurrentPosition()
                )
                .apply()
        }
    }

    override fun onDestroy() {
        playerManager.release()
        super.onDestroy()
    }
}
