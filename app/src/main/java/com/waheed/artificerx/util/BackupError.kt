package com.waheed.artificerx.util


sealed class BackupError {
    object NetworkUnreachable : BackupError()
    object DiskFull : BackupError()
    object PermissionDenied : BackupError()
    data class UnknownError(val message: String) : BackupError()

    fun getErrorMessage(context: android.content.Context): String =
        when (this) {
            NetworkUnreachable -> "Network unavailable"
            DiskFull -> "Insufficient storage"
            PermissionDenied -> "Required permission is missing"
            is UnknownError -> message
        }
}