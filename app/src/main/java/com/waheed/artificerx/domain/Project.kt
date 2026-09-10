package com.waheed.artificerx.domain

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.waheed.artificerx.util.ImageBitmapRoom

@Entity(tableName = "projects", indices = [Index(value = ["projectId"], unique = true)])
@TypeConverters(ImageBitmapRoom::class)
data class Project
    (@ColumnInfo(name = "projectId") val projectId: String = UUID.randomUUID().toString(),
     @ColumnInfo(name = "name") val name: String,
     @ColumnInfo(name = "createdAt") val createdAt: Long = System.currentTimeMillis(),
     @ColumnInfo(name = "lastModified") val lastModified: Long = System.currentTimeMillis(),
     @ColumnInfo(name = "artifacts") val artifacts: ArrayList<Bitmap> = ArrayList()
)