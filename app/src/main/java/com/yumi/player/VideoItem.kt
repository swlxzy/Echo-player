
package com.yumi.player

data class VideoItem(
    val name: String,
    val uri: String,
    val duration: Long = 0L,
    val sizeBytes: Long = 0L,
    val width: Int = 0,
    val height: Int = 0
)
