
package com.yumi.player

import android.graphics.Bitmap
import android.util.LruCache
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class VideoAdapter(
    private var videos: List<VideoItem>,
    private val onVideoClick: (VideoItem) -> Unit
) : RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

    private val thumbnailCache = object : LruCache<String, Bitmap>(
        8 * 1024
    ) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    private val thumbnailExecutor = ThreadPoolExecutor(
        2,
        2,
        30L,
        TimeUnit.SECONDS,
        LinkedBlockingQueue()
    )

    private lateinit var repository: VideoRepository

    class VideoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.textVideoName)
        val duration: TextView = view.findViewById(R.id.textVideoDuration)
        val thumbnail: ImageView =
            view.findViewById(R.id.imageVideoThumbnail)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): VideoViewHolder {
        if (!::repository.isInitialized) {
            repository = VideoRepository(
                parent.context.applicationContext
            )
        }

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_video, parent, false)

        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: VideoViewHolder,
        position: Int
    ) {
        val video = videos[position]

        holder.name.text = video.name

        val durationText = formatDuration(video.duration)
        val sizeText = formatFileSize(video.sizeBytes)

        val resolutionText =
            if (video.width > 0 && video.height > 0) {
                "${video.width} × ${video.height}"
            } else {
                "Çözünürlük bilinmiyor"
            }

        holder.duration.text =
            "$durationText • $sizeText • $resolutionText"

        loadThumbnail(holder, video)

        holder.itemView.setOnClickListener {
            onVideoClick(video)
        }
    }

    private fun loadThumbnail(
        holder: VideoViewHolder,
        video: VideoItem
    ) {
        holder.thumbnail.tag = video.uri

        val cachedBitmap = thumbnailCache.get(video.uri)

        if (cachedBitmap != null) {
            holder.thumbnail.setImageBitmap(cachedBitmap)
            return
        }

        holder.thumbnail.setImageDrawable(null)

        thumbnailExecutor.execute {
            val bitmap = repository.getVideoThumbnail(
                android.net.Uri.parse(video.uri)
            )

            if (bitmap != null) {
                thumbnailCache.put(video.uri, bitmap)

                holder.thumbnail.post {
                    if (holder.thumbnail.tag == video.uri) {
                        holder.thumbnail.setImageBitmap(bitmap)
                    }
                }
            }
        }
    }

    override fun onViewRecycled(holder: VideoViewHolder) {
        holder.thumbnail.tag = null
        holder.thumbnail.setImageDrawable(null)
        super.onViewRecycled(holder)
    }

    override fun getItemCount(): Int = videos.size

    fun updateVideos(newVideos: List<VideoItem>) {
        val oldVideos = videos
        val updatedVideos = newVideos.toList()

        val diffCallback = object : DiffUtil.Callback() {

            override fun getOldListSize(): Int = oldVideos.size

            override fun getNewListSize(): Int = updatedVideos.size

            override fun areItemsTheSame(
                oldItemPosition: Int,
                newItemPosition: Int
            ): Boolean {
                return oldVideos[oldItemPosition].uri ==
                    updatedVideos[newItemPosition].uri
            }

            override fun areContentsTheSame(
                oldItemPosition: Int,
                newItemPosition: Int
            ): Boolean {
                return oldVideos[oldItemPosition] ==
                    updatedVideos[newItemPosition]
            }
        }

        val diffResult = DiffUtil.calculateDiff(diffCallback)

        videos = updatedVideos
        diffResult.dispatchUpdatesTo(this)
    }

    private fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    private fun formatFileSize(sizeBytes: Long): String {
        if (sizeBytes <= 0) return "Bilinmeyen boyut"

        val megabytes = sizeBytes / (1024.0 * 1024.0)

        return if (megabytes >= 1024) {
            String.format("%.2f GB", megabytes / 1024)
        } else {
            String.format("%.1f MB", megabytes)
        }
    }
}
