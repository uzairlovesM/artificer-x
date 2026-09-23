package com.waheed.artificerx.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Canonical database migration registry. Current project database migrations are versioned independently from workspace schema. */
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

val MIGRATION_2_TO_3 = object : androidx.room.migration.Migration(2, 3) {
    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE chat_messages ADD COLUMN reasoningSummariesJson TEXT NOT NULL DEFAULT '[]'")
        database.execSQL("ALTER TABLE chat_messages ADD COLUMN reasoningEffort TEXT")
        database.execSQL("ALTER TABLE chat_messages ADD COLUMN reasoningDurationMs INTEGER")
    }
}

val MIGRATIONS = arrayOf(MIGRATION_1_TO_2, MIGRATION_2_TO_3)
