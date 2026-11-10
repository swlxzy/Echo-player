
package com.yumi.player

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var videoAdapter: VideoAdapter
    private lateinit var videoRepository: VideoRepository

    private val scanExecutor = Executors.newSingleThreadExecutor()

    private val preferences by lazy {
        getSharedPreferences("yumi_settings", MODE_PRIVATE)
    }

    private val folderPicker =
        registerForActivityResult(
            ActivityResultContracts.OpenDocumentTree()
        ) { uri: Uri? ->

            if (uri != null) {
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )

                    preferences.edit()
                        .putString("selected_folder", uri.toString())
                        .apply()

                    loadVideos(uri)

                } catch (e: SecurityException) {
                    Toast.makeText(
                        this,
                        "Klasör erişim izni alınamadı.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val buttonSelectFolder =
            findViewById<Button>(R.id.buttonSelectFolder)

        recyclerView =
            findViewById(R.id.recyclerViewVideos)

        videoRepository = VideoRepository(this)

        videoAdapter = VideoAdapter(emptyList()) { video ->
            val intent = Intent(this, PlayerActivity::class.java)
            intent.putExtra("video_uri", video.uri)
            startActivity(intent)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = videoAdapter

        buttonSelectFolder.setOnClickListener {
            folderPicker.launch(null)
        }

        restoreSelectedFolder()
    }

    private fun restoreSelectedFolder() {
        val savedUri = preferences.getString("selected_folder", null)
            ?: return

        val folderUri = Uri.parse(savedUri)

        val hasPermission = contentResolver.persistedUriPermissions.any {
            it.uri == folderUri && it.isReadPermission
        }

        if (hasPermission) {
            loadVideos(folderUri)
        } else {
            preferences.edit()
                .remove("selected_folder")
                .apply()
        }
    }

    private fun loadVideos(folderUri: Uri) {
        scanExecutor.execute {
            try {
                val videos =
                    videoRepository.getVideosFromFolder(folderUri)

                runOnUiThread {
                    if (!isFinishing && !isDestroyed) {
                        videoAdapter.updateVideos(videos)

                        Toast.makeText(
                            this,
                            "${videos.size} video bulundu.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

            } catch (e: Exception) {
                runOnUiThread {
                    if (!isFinishing && !isDestroyed) {
                        Toast.makeText(
                            this,
                            "Videolar taranırken hata oluştu.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        scanExecutor.shutdownNow()
        super.onDestroy()
    }
}
