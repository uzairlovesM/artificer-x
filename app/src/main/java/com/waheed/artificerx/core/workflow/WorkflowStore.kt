package com.waheed.artificerx.core.workflow

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.workflowDataStore: DataStore<Preferences> by preferencesDataStore(name = "workflow_library")

@Singleton
class WorkflowStore @Inject constructor(@ApplicationContext private val context: Context) {
    private val key = stringPreferencesKey("definitions")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    val workflows: Flow<List<WorkflowDefinition>> = context.workflowDataStore.data.map { prefs ->
        runCatching { json.decodeFromString<List<WorkflowDefinition>>(prefs[key].orEmpty()) }.getOrDefault(emptyList())
    }

    suspend fun upsert(workflow: WorkflowDefinition) = context.workflowDataStore.edit { prefs ->
        val current = runCatching { json.decodeFromString<List<WorkflowDefinition>>(prefs[key].orEmpty()) }.getOrDefault(emptyList())
        prefs[key] = json.encodeToString<List<WorkflowDefinition>>((current.filterNot { it.id == workflow.id } + workflow).takeLast(100))
    }

    suspend fun delete(id: String) = context.workflowDataStore.edit { prefs ->
        val current = runCatching { json.decodeFromString<List<WorkflowDefinition>>(prefs[key].orEmpty()) }.getOrDefault(emptyList())
        prefs[key] = json.encodeToString<List<WorkflowDefinition>>(current.filterNot { it.id == id })
    }
}
