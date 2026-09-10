package com.waheed.artificerx.util

import androidx.annotation.StringRes

sealed class BackupError {
    object NetworkUnreachable : BackupError()
    object DiskFull : BackupError()
    object PermissionDenied : BackupError()
    data class UnknownError(val message: String) : BackupError()

    fun getErrorMessage(context: android.content.Context): String =
        when (this) {
            NetworkUnreachable -> context.getString(R.string.error_network_unavailable)
            DiskFull -> context.getString(R.string.error_insufficient_storage)
            PermissionDenied -> context.getString(R.string.error_permissions_missing)
            is UnknownError -> context.getString(R.string.error_unknown, message)
        }
}