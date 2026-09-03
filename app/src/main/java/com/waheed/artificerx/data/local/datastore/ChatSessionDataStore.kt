package com.waheed.artificerx.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.chatSessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "chat_session_state")

@Singleton
class ChatSessionDataStore @Inject constructor(@ApplicationContext private val context: Context) {
    private val activeThreadKey = stringPreferencesKey("active_thread_id")
    suspend fun getActiveThreadId(): String? = context.chatSessionDataStore.data.map { it[activeThreadKey] }.first()
    suspend fun setActiveThreadId(id: String) { context.chatSessionDataStore.edit { it[activeThreadKey] = id } }
    suspend fun clearActiveThreadId() { context.chatSessionDataStore.edit { it.remove(activeThreadKey) } }
}
