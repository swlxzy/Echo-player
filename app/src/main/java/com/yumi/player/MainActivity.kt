package com.yumi.player

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var videoAdapter: VideoAdapter
    private lateinit var videoRepository: VideoRepository

    private val folderPicker =
        registerForActivityResult(
            ActivityResultContracts.OpenDocumentTree()
        ) { uri: Uri? ->

            if (uri != null) {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                loadVideos(uri)
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

        videoAdapter = VideoAdapter(emptyList())

        recyclerView.layoutManager =
            LinearLayoutManager(this)

        recyclerView.adapter = videoAdapter

        buttonSelectFolder.setOnClickListener {
            folderPicker.launch(null)
        }
    }

    private fun loadVideos(folderUri: Uri) {
        val videos =
            videoRepository.getVideosFromFolder(folderUri)

        videoAdapter.updateVideos(videos)
    }
}
