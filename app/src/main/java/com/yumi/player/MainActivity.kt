
package com.yumi.player

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var videoAdapter: VideoAdapter
    private lateinit var videoRepository: VideoRepository
    private lateinit var editSearch: EditText
    private lateinit var spinnerSort: Spinner

    private val scanExecutor = Executors.newSingleThreadExecutor()

    private var allVideos: List<VideoItem> = emptyList()

    private val sortOptions = listOf(
        "İsim: A-Z",
        "İsim: Z-A",
        "Boyut: Büyükten küçüğe",
        "Boyut: Küçükten büyüğe",
        "Tarih: Yeniden eskiye",
        "Tarih: Eskiden yeniye"
    )

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

        editSearch =
            findViewById(R.id.editSearch)

        spinnerSort =
            findViewById(R.id.spinnerSort)

        videoRepository = VideoRepository(this)

        videoAdapter = VideoAdapter(emptyList()) { video ->
            val intent = Intent(this, PlayerActivity::class.java)
            intent.putExtra("video_uri", video.uri)
            startActivity(intent)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = videoAdapter

        val spinnerAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            sortOptions
        )

        spinnerAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spinnerSort.adapter = spinnerAdapter

        editSearch.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {}

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                applyFilters()
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        spinnerSort.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: android.view.View?,
                    position: Int,
                    id: Long
                ) {
                    applyFilters()
                }

                override fun onNothingSelected(
                    parent: android.widget.AdapterView<*>?
                ) {}
            }

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
                        allVideos = videos
                        applyFilters()

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

    private fun applyFilters() {
        val query = editSearch.text
            .toString()
            .trim()
            .lowercase(Locale.getDefault())

        val filteredVideos = allVideos.filter { video ->
            video.name.lowercase(Locale.getDefault()).contains(query)
        }

        val sortedVideos = when (spinnerSort.selectedItemPosition) {

            0 -> filteredVideos.sortedBy {
                it.name.lowercase(Locale.getDefault())
            }

            1 -> filteredVideos.sortedByDescending {
                it.name.lowercase(Locale.getDefault())
            }

            2 -> filteredVideos.sortedByDescending {
                it.sizeBytes
            }

            3 -> filteredVideos.sortedBy {
                it.sizeBytes
            }

            4 -> filteredVideos.sortedByDescending {
                it.lastModified
            }

            5 -> filteredVideos.sortedBy {
                it.lastModified
            }

            else -> filteredVideos
        }

        videoAdapter.updateVideos(sortedVideos)
    }

    override fun onDestroy() {
        scanExecutor.shutdownNow()
        super.onDestroy()
    }
}
