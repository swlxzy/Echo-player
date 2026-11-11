package com.yumi.player

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.ui.PlayerView

class PlayerActivity : AppCompatActivity() {

    private lateinit var playerView: PlayerView
    private lateinit var playerManager: PlayerManager
    private lateinit var gestureDetector: GestureDetector
    private lateinit var orientationLockButton: TextView
    private lateinit var speedIndicator: View

    private var seekSeconds = 5L
    private var videoUri: String? = null
    private var previousSpeed = 1f
    private var isTemporarySpeedActive = false
    private var isOrientationLocked = false

    private val preferences by lazy {
        getSharedPreferences("yumi_settings", MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContentView(R.layout.activity_player)

        hideSystemBars()

        playerView = findViewById(R.id.playerView)
        orientationLockButton =
            findViewById(R.id.orientationLockButton)
        speedIndicator =
            findViewById(R.id.speedIndicator)

        isOrientationLocked =
            savedInstanceState?.getBoolean("orientation_locked") ?: false

        updateOrientationLock()

        orientationLockButton.setOnClickListener {
            isOrientationLocked = !isOrientationLocked
            updateOrientationLock()
        }

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

    private fun updateOrientationLock() {
        if (isOrientationLocked) {
            requestedOrientation =
                ActivityInfo.SCREEN_ORIENTATION_LOCKED

            orientationLockButton.text = "🔒"
            orientationLockButton.contentDescription =
                getString(R.string.screen_orientation_locked)
        } else {
            requestedOrientation =
                ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR

            orientationLockButton.text = "↻"
            orientationLockButton.contentDescription =
                getString(R.string.automatic_rotation_enabled)
        }
    }

    private fun hideSystemBars() {
        val controller = WindowInsetsControllerCompat(
            window,
            window.decorView
        )

        controller.hide(
            WindowInsetsCompat.Type.statusBars() or
                WindowInsetsCompat.Type.navigationBars()
        )

        controller.systemBarsBehavior =
            WindowInsetsControllerCompat
                .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        if (hasFocus) {
            hideSystemBars()
        }
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
                    if (!isTemporarySpeedActive) {
                        previousSpeed = playerManager.getSpeed()
                        isTemporarySpeedActive = true

                        playerManager.setSpeed(2f)
                        speedIndicator.visibility = View.VISIBLE
                    }
                }
            }
        )
    }

    private fun restoreTemporarySpeed() {
        if (!isTemporarySpeedActive) return

        playerManager.setSpeed(previousSpeed)

        isTemporarySpeedActive = false
        speedIndicator.visibility = View.GONE
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (::gestureDetector.isInitialized) {
            gestureDetector.onTouchEvent(event)

            when (event.action) {
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    restoreTemporarySpeed()
                }
            }
        }

        return super.dispatchTouchEvent(event)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(
            "orientation_locked",
            isOrientationLocked
        )

        super.onSaveInstanceState(outState)
    }

    override fun onPause() {
        if (::playerManager.isInitialized) {
            restoreTemporarySpeed()
        }

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
        if (::playerManager.isInitialized) {
            playerManager.release()
        }

        super.onDestroy()
    }
}
