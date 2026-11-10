
package com.yumi.player

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import androidx.documentfile.provider.DocumentFile

class VideoRepository(private val context: Context) {

    fun getVideosFromFolder(folderUri: Uri): List<VideoItem> {
        val folder = DocumentFile.fromTreeUri(context, folderUri)
            ?: return emptyList()

        return folder.listFiles()
            .filter { file ->
                file.isFile && isVideoFile(file.name)
            }
            .map { file ->
                val metadata = getVideoMetadata(file.uri)

                VideoItem(
                    name = file.name ?: "Bilinmeyen video",
                    uri = file.uri.toString(),
                    duration = metadata.first,
                    sizeBytes = file.length(),
                    width = metadata.second,
                    height = metadata.third,
                    lastModified = file.lastModified()
                )
            }
    }

    fun getVideoThumbnail(uri: Uri): Bitmap? {
        val retriever = MediaMetadataRetriever()

        return try {
            retriever.setDataSource(context, uri)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                retriever.getScaledFrameAtTime(
                    -1,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                    320,
                    180
                )
            } else {
                val original = retriever.getFrameAtTime(
                    -1,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                ) ?: return null

                val scale = minOf(
                    320f / original.width,
                    180f / original.height,
                    1f
                )

                val width = (original.width * scale).toInt().coerceAtLeast(1)
                val height = (original.height * scale).toInt().coerceAtLeast(1)

                val scaled = Bitmap.createScaledBitmap(
                    original,
                    width,
                    height,
                    true
                )

                if (scaled !== original) {
                    original.recycle()
                }

                scaled
            }
        } catch (e: Exception) {
            null
        } finally {
            retriever.release()
        }
    }

    private fun getVideoMetadata(uri: Uri): Triple<Long, Int, Int> {
        val retriever = MediaMetadataRetriever()

        return try {
            retriever.setDataSource(context, uri)

            val duration = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLongOrNull() ?: 0L

            val width = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
            )?.toIntOrNull() ?: 0

            val height = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
            )?.toIntOrNull() ?: 0

            Triple(duration, width, height)
        } catch (e: Exception) {
            Triple(0L, 0, 0)
        } finally {
            retriever.release()
        }
    }

    private fun isVideoFile(name: String?): Boolean {
        if (name == null) return false

        val extension = name
            .substringAfterLast('.', "")
            .lowercase()

        return extension in setOf(
            "mp4",
            "mkv",
            "webm",
            "avi",
            "mov",
            "m4v"
        )
    }
}
