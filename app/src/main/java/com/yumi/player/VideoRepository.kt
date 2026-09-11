package com.yumi.player

import android.content.Context
import android.net.Uri
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
                VideoItem(
                    name = file.name ?: "Bilinmeyen video",
                    uri = file.uri.toString(),
                    duration = 0L
                )
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
