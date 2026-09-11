package com.yumi.player

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class VideoAdapter(
    private var videos: List<VideoItem>
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
        holder.duration.text = formatDuration(video.duration)
    }

    override fun getItemCount(): Int {
        return videos.size
    }

    fun updateVideos(newVideos: List<VideoItem>) {
        videos = newVideos
        notifyDataSetChanged()
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
}
