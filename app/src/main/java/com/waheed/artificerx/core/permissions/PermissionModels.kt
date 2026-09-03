package com.waheed.artificerx.core.permissions

data class PermissionSnapshot(
    val camera: Boolean,
    val voice: Boolean,
    val images: Boolean,
    val video: Boolean,
    val audio: Boolean,
    val notifications: Boolean,
) {
    val grantedCount: Int get() = listOf(camera, voice, images, video, audio, notifications).count { it }
    val totalCount: Int get() = 6
}
