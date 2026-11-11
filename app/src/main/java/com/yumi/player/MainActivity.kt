package com.yumi.player

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
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

    private lateinit var buttonSearch: TextView
    private lateinit var buttonSort: TextView
    private lateinit var buttonSettings: TextView
    private lateinit var buttonListView: TextView
    private lateinit var buttonGridView: TextView

    private val scanExecutor = Executors.newSingleThreadExecutor()

    private var allVideos: List<VideoItem> = emptyList()
    private var sortPosition = 0
    private var isGridView = true
    private var isSearchVisible = false

    private val sortOptions = arrayOf(
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

    private val videoPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val granted = permissions.values.any { it }

            if (granted || hasVideoPermission()) {
                loadPreferredVideos()
            } else {
                Toast.makeText(
                    this,
                    "Video izni verilmedi. Klasör seçerek devam edebilirsin.",
                    Toast.LENGTH_LONG
                ).show()

                restoreSelectedFolder()
            }
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

        recyclerView = findViewById(R.id.recyclerViewVideos)
        editSearch = findViewById(R.id.editSearch)
        spinnerSort = findViewById(R.id.spinnerSort)

        buttonSearch = findViewById(R.id.buttonSearch)
        buttonSort = findViewById(R.id.buttonSort)
        buttonSettings = findViewById(R.id.buttonSettings)
        buttonListView = findViewById(R.id.buttonListView)
        buttonGridView = findViewById(R.id.buttonGridView)

        val buttonSelectFolder =
            findViewById<TextView>(R.id.buttonSelectFolder)

        videoRepository = VideoRepository(this)

        videoAdapter = VideoAdapter(emptyList()) { video ->
            val intent = Intent(this, PlayerActivity::class.java)
            intent.putExtra("video_uri", video.uri)
            startActivity(intent)
        }

        recyclerView.adapter = videoAdapter

        setGridView(true)

        buttonSelectFolder.setOnClickListener {
            folderPicker.launch(null)
        }

        buttonSearch.setOnClickListener {
            toggleSearch()
        }

        buttonSort.setOnClickListener {
            showSortDialog()
        }

        buttonSettings.setOnClickListener {
            Toast.makeText(
                this,
                "Ayarlar ekranı sonraki aşamada eklenecek.",
                Toast.LENGTH_SHORT
            ).show()
        }

        buttonListView.setOnClickListener {
            setGridView(false)
        }

        buttonGridView.setOnClickListener {
            setGridView(true)
        }

        editSearch.addTextChangedListener(
            object : android.text.TextWatcher {

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

                override fun afterTextChanged(
                    s: android.text.Editable?
                ) {}
            }
        )

        requestVideoPermission()
    }

    private fun getRequiredVideoPermissions(): Array<String> {
        return when {
            Build.VERSION.SDK_INT >= 34 -> arrayOf(
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            )

            Build.VERSION.SDK_INT >= 33 -> arrayOf(
                Manifest.permission.READ_MEDIA_VIDEO
            )

            else -> arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
    }

    private fun hasVideoPermission(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= 34 -> {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_VIDEO
                ) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                    ) == PackageManager.PERMISSION_GRANTED
            }

            Build.VERSION.SDK_INT >= 33 -> {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_VIDEO
                ) == PackageManager.PERMISSION_GRANTED
            }

            else -> {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
    }

    private fun requestVideoPermission() {
        if (hasVideoPermission()) {
            loadPreferredVideos()
        } else {
            videoPermissionLauncher.launch(
                getRequiredVideoPermissions()
            )
        }
    }

    private fun loadPreferredVideos() {
        if (!restoreSelectedFolder()) {
            loadAllVideos()
        }
    }

    private fun loadAllVideos() {
        scanExecutor.execute {
            val videos = videoRepository.getAllVideos()

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
        }
    }

    private fun toggleSearch() {
        isSearchVisible = !isSearchVisible

        if (isSearchVisible) {
            editSearch.visibility = View.VISIBLE
            editSearch.requestFocus()

            val keyboard = getSystemService(
                Context.INPUT_METHOD_SERVICE
            ) as InputMethodManager

            keyboard.showSoftInput(
                editSearch,
                InputMethodManager.SHOW_IMPLICIT
            )
        } else {
            editSearch.text.clear()
            editSearch.visibility = View.GONE

            val keyboard = getSystemService(
                Context.INPUT_METHOD_SERVICE
            ) as InputMethodManager

            keyboard.hideSoftInputFromWindow(
                editSearch.windowToken,
                0
            )
        }
    }

    private fun showSortDialog() {
        AlertDialog.Builder(this)
            .setTitle("Videoları sırala")
            .setSingleChoiceItems(
                sortOptions,
                sortPosition
            ) { dialog, which ->

                sortPosition = which
                spinnerSort.setSelection(which)

                applyFilters()
                dialog.dismiss()
            }
            .setNegativeButton("Kapat", null)
            .show()
    }

    private fun setGridView(grid: Boolean) {
        isGridView = grid

        if (grid) {
            recyclerView.layoutManager =
                GridLayoutManager(this, 3)

            buttonGridView.setBackgroundColor(
                android.graphics.Color.rgb(41, 41, 41)
            )

            buttonListView.setBackgroundColor(
                android.graphics.Color.rgb(25, 25, 25)
            )
        } else {
            recyclerView.layoutManager =
                LinearLayoutManager(this)

            buttonListView.setBackgroundColor(
                android.graphics.Color.rgb(41, 41, 41)
            )

            buttonGridView.setBackgroundColor(
                android.graphics.Color.rgb(25, 25, 25)
            )
        }
    }

    private fun restoreSelectedFolder(): Boolean {
        val savedUri = preferences.getString(
            "selected_folder",
            null
        ) ?: return false

        val folderUri = try {
            Uri.parse(savedUri)
        } catch (e: Exception) {
            preferences.edit()
                .remove("selected_folder")
                .apply()
            return false
        }

        val hasPermission = try {
            contentResolver.persistedUriPermissions.any {
                it.uri == folderUri && it.isReadPermission
            }
        } catch (e: Exception) {
            false
        }

        if (hasPermission) {
            loadVideos(folderUri)
            return true
        }

        preferences.edit()
            .remove("selected_folder")
            .apply()

        return false
    }

    private fun loadVideos(folderUri: Uri) {
        scanExecutor.execute {
            try {
                val includeSubfolders = preferences.getBoolean(
                    "include_subfolders",
                    false
                )

                val videos = videoRepository.getVideosFromFolder(
                    folderUri,
                    includeSubfolders
                )

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
            video.name
                .lowercase(Locale.getDefault())
                .contains(query)
        }

        val sortedVideos = when (sortPosition) {

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
