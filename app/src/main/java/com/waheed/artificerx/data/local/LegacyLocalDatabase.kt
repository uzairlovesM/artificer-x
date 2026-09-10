package com.waheed.artificerx.data.local

/**
 * Marker for the retired pre-v0.8 database graph. The canonical database is
 * [com.waheed.artificerx.data.local.db.ArtificerXDatabase] and its DAO/entity
 * graph is exposed from DatabaseModule. Keeping this marker avoids resurrecting
 * two Room databases against the same application database path.
 */
internal object LegacyLocalDatabase
