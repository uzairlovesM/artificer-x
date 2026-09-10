package com.waheed.artificerx.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Canonical database migration registry. The current schema is version 2. */
val MIGRATION_1_TO_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        val columns = database.query("PRAGMA table_info(projects)").use { cursor ->
            buildSet {
                val nameIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext() && nameIndex >= 0) add(cursor.getString(nameIndex))
            }
        }
        if (!columns.contains("lastOpenedEpochMillis")) {
            database.execSQL("ALTER TABLE projects ADD COLUMN lastOpenedEpochMillis INTEGER")
        }
    }
}

val MIGRATIONS = arrayOf(MIGRATION_1_TO_2)
