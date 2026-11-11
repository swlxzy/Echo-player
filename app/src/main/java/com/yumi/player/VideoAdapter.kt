package com.yumi.player

import android.graphics.Bitmap
import android.util.LruCache
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.RejectedExecutionException
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
        ArrayBlockingQueue(64),
        ThreadPoolExecutor.DiscardOldestPolicy()
    )

    private lateinit var repository: VideoRepository

    class VideoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.textVideoName)
        val duration: TextView = view.findViewById(R.id.textVideoDuration)
        val thumbnail: ImageView =
            view.findViewById(R.id.imageVideoThumbnail)
        val content: LinearLayout =
            view.findViewById(R.id.videoItemContent)
        val details: LinearLayout =
            view.findViewById(R.id.videoItemDetails)
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
        val context = holder.itemView.context

        val isGrid =
            (holder.itemView.parent as? RecyclerView)
                ?.layoutManager is GridLayoutManager

        configureItemLayout(holder, isGrid)

        holder.name.text = video.name

        val durationText = formatDuration(video.duration)
        val sizeText = formatFileSize(context, video.sizeBytes)

        val resolutionText =
            if (video.width > 0 && video.height > 0) {
                context.getString(
                    R.string.video_resolution_format,
                    video.width,
                    video.height
                )
            } else {
                context.getString(R.string.unknown_resolution)
            }

        holder.duration.text =
            "$durationText • $sizeText • $resolutionText"

        loadThumbnail(holder, video)

        holder.itemView.setOnClickListener {
            onVideoClick(video)
        }
    }

    private fun configureItemLayout(
        holder: VideoViewHolder,
        isGrid: Boolean
    ) {
        val context = holder.itemView.context

        fun dp(value: Int): Int {
            return (value * context.resources.displayMetrics.density)
                .toInt()
        }

        holder.content.orientation =
            if (isGrid) LinearLayout.VERTICAL
            else LinearLayout.HORIZONTAL

        holder.content.setPadding(
            dp(if (isGrid) 5 else 9),
            dp(if (isGrid) 5 else 9),
            dp(if (isGrid) 5 else 9),
            dp(if (isGrid) 5 else 9)
        )

        val thumbnailParams = holder.thumbnail.layoutParams
        thumbnailParams.width =
            if (isGrid) ViewGroup.LayoutParams.MATCH_PARENT
            else dp(120)
        thumbnailParams.height = dp(if (isGrid) 105 else 76)
        holder.thumbnail.layoutParams = thumbnailParams

        val detailsParams =
            holder.details.layoutParams as LinearLayout.LayoutParams

        detailsParams.width =
            if (isGrid) ViewGroup.LayoutParams.MATCH_PARENT else 0

        detailsParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        detailsParams.weight = if (isGrid) 0f else 1f
        detailsParams.marginStart = dp(if (isGrid) 0 else 12)
        detailsParams.topMargin = dp(if (isGrid) 6 else 0)

        holder.details.layoutParams = detailsParams

        val nameParams =
            holder.name.layoutParams as LinearLayout.LayoutParams

        nameParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        nameParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        nameParams.marginStart = 0
        nameParams.topMargin = 0
        holder.name.layoutParams = nameParams

        holder.name.textSize = if (isGrid) 11f else 15f
        holder.name.maxLines = 2

        val durationParams =
            holder.duration.layoutParams as LinearLayout.LayoutParams

        durationParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        durationParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        durationParams.marginStart = 0
        durationParams.topMargin = dp(if (isGrid) 3 else 7)
        holder.duration.layoutParams = durationParams

        holder.duration.textSize = if (isGrid) 9f else 11f

        val cardParams =
            holder.itemView.layoutParams as? ViewGroup.MarginLayoutParams

        cardParams?.let {
            it.marginStart = dp(if (isGrid) 3 else 0)
            it.marginEnd = dp(if (isGrid) 3 else 0)
            it.bottomMargin = dp(7)
            holder.itemView.layoutParams = it
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

        if (thumbnailExecutor.isShutdown) return

        try {
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
        } catch (e: RejectedExecutionException) {
            // Executor kapanmışsa yeni küçük resim görevi başlatılmaz.
        }
    }

    override fun onViewRecycled(holder: VideoViewHolder) {
        holder.thumbnail.tag = null
        holder.thumbnail.setImageDrawable(null)
        super.onViewRecycled(holder)
    }

    fun shutdown() {
        thumbnailExecutor.shutdownNow()
        thumbnailCache.evictAll()
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
            String.format(
                Locale.ROOT,
                "%02d:%02d:%02d",
                hours,
                minutes,
                seconds
            )
        } else {
            String.format(
                Locale.ROOT,
                "%02d:%02d",
                minutes,
                seconds
            )
        }
    }

    private fun formatFileSize(
        context: android.content.Context,
        sizeBytes: Long
    ): String {
        if (sizeBytes <= 0) {
            return context.getString(R.string.unknown_size)
        }

        val megabytes = sizeBytes / (1024.0 * 1024.0)

        return if (megabytes >= 1024) {
            context.getString(
                R.string.video_size_gigabytes,
                megabytes / 1024
            )
        } else {
            context.getString(
                R.string.video_size_megabytes,
                megabytes
            )
        }
    }
}
