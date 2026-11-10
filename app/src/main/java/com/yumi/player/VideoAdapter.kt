
package com.yumi.player

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

class VideoAdapter(
    private var videos: List<VideoItem>,
    private val onVideoClick: (VideoItem) -> Unit
) : RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

    class VideoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.textVideoName)
        val duration: TextView = view.findViewById(R.id.textVideoDuration)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): VideoViewHolder {
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

        holder.itemView.setOnClickListener {
            onVideoClick(video)
        }
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
