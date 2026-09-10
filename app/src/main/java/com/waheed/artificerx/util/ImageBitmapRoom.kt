package com.waheed.artificerx.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.room.TypeConverter

@DatabaseView("""SELECT * FROM projects""")
data class ProjectView(
    @ColumnInfo(name = "projectId") val projectId: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "createdAt") val createdAt: Long,
    @ColumnInfo(name = "lastModified") val lastModified: Long,
    @ColumnInfo(name = "artifacts") val artifactBytes: ByteArray
)

class ImageBitmapRoom {
    @TypeConverter
    fun from_bitmap(b: Bitmap?): ByteArray? = b?.let { bitmapToBytes(it) }

    @TypeConverter
    fun to_bitmap(bytes: ByteArray?): Bitmap? = bytes?.let { bytesToBitmap(it) }

    @TypeConverter
    fun bitmapToBytes(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }

    @TypeConverter
    fun bytesToBitmap(bytes: ByteArray): Bitmap {
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    @TypeConverter
    fun from_list(list: List<Bitmap>?): ArrayList<Bitmap>? = list?.let { it as ArrayList }

    @TypeConverter
    fun to_list(arrayList: ArrayList<Bitmap>?): List<Bitmap>? = arrayList?.toList()
}