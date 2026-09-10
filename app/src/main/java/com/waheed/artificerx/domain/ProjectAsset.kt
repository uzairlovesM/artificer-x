package com.waheed.artificerx.domain

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.waheed.artificerx.util.ImageBitmapRoom

@Keep // Prevent Room from stripping the class
@Entity(tableName = "project_assets", indices = [Index(value = ["assetId"], unique = true)])
@TypeConverters(ImageBitmapRoom::class)
data class ProjectAsset
    (@ColumnInfo(name = "assetId") val assetId: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "projectId") val projectId: String,
    @ColumnInfo(name = "assetType") val assetType: String,
    @ColumnInfo(name = "createdAt") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "assetData") val assetData: Bitmap
)