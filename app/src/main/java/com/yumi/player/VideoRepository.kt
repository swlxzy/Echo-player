package com.yumi.player

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile

class VideoRepository(private val context: Context) {

    fun getAllVideos(): List<VideoItem> {
        val videos = mutableListOf<VideoItem>()

        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DATE_MODIFIED
        )

        try {
            context.contentResolver.query(
                collection,
                projection,
                null,
                null,
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )?.use { cursor ->

                val idColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)

                val nameColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)

                val durationColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)

                val sizeColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)

                val widthColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)

                val heightColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)

                val modifiedColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)

                    val uri = ContentUris.withAppendedId(
                        collection,
                        id
                    )

                    videos.add(
                        VideoItem(
                            name = cursor.getString(nameColumn)
                                ?: "Bilinmeyen video",
                            uri = uri.toString(),
                            duration = cursor.getLong(durationColumn),
                            sizeBytes = cursor.getLong(sizeColumn),
                            width = cursor.getInt(widthColumn),
                            height = cursor.getInt(heightColumn),
                            lastModified = cursor.getLong(modifiedColumn) * 1000L
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return videos
    }

    fun getVideosFromFolder(
        folderUri: Uri,
        includeSubfolders: Boolean = false
    ): List<VideoItem> {

        val rootFolder = try {
            DocumentFile.fromTreeUri(context, folderUri)
        } catch (e: Exception) {
            null
        } ?: return emptyList()

        val videos = mutableListOf<VideoItem>()
        val pendingFolders = mutableListOf(rootFolder)
        val visitedFolders = mutableSetOf(rootFolder.uri.toString())

        while (pendingFolders.isNotEmpty()) {

            val currentFolder = pendingFolders.removeAt(
                pendingFolders.lastIndex
            )

            val children = try {
                currentFolder.listFiles()
            } catch (e: Exception) {
                e.printStackTrace()
                continue
            }

            for (file in children) {
                try {
                    when {
                        file.isDirectory && includeSubfolders -> {
                            val folderKey = file.uri.toString()

                            if (visitedFolders.add(folderKey)) {
                                pendingFolders.add(file)
                            }
                        }

                        file.isFile && isVideoFile(file.name, file.type) -> {
                            val metadata = getVideoMetadata(file.uri)

                            videos.add(
                                VideoItem(
                                    name = file.name ?: "Bilinmeyen video",
                                    uri = file.uri.toString(),
                                    duration = metadata.first,
                                    sizeBytes = file.length(),
                                    width = metadata.second,
                                    height = metadata.third,
                                    lastModified = file.lastModified()
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        return videos
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

    private fun isVideoFile(
        name: String?,
        mimeType: String?
    ): Boolean {
        if (mimeType?.startsWith("video/") == true) {
            return true
        }

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
            "m4v",
            "3gp",
            "3gpp",
            "ts",
            "m2ts",
            "mts",
            "mpg",
            "mpeg",
            "vob",
            "flv"
        )
    }
}
