package com.waheed.artificerx.domain

import com.waheed.artificerx.data.local.db.ProjectEntity

fun ProjectEntity.toDomain(): Project = Project(
    projectId = id,
    name = name,
    createdAt = createdAtEpochMillis,
    lastModified = lastModifiedEpochMillis,
    artifactPaths = listOfNotNull(thumbnailPath),
)
