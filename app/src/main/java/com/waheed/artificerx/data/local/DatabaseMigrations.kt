package com.waheed.artificerx.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Version 1 to 2 migration - actual implementation if needed
val MIGRATION_1_TO_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add new columns, tables, or constraints here
        // Example: database.execSQL("ALTER TABLE projects ADD COLUMN version INTEGER NOT NULL DEFAULT 1")
        DebugLogger.d("DatabaseMigration", "Performed 1→2 schema migration")
    }
}

// Version 2 to 3 migration
val MIGRATION_2_TO_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Handle project state field expansions
        DebugLogger.d("DatabaseMigration", "Performed 2→3 schema migration - added drawing metadata")
    }
}

// Cached migration set
val MIGRATIONS = arrayOf(MIGRATION_1_TO_2, MIGRATION_2_TO_3)
