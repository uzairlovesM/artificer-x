package com.waheed.artificerx.core.chat

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import javax.inject.Inject
import javax.inject.Singleton

private val Context.chatProfilesDataStore by preferencesDataStore(name = "ai_chat_profiles")

@Singleton
class ChatProfileStore @Inject constructor(@ApplicationContext private val context: Context) {
    private val profilesKey = stringPreferencesKey("profiles_json")
    private val activeKey = stringPreferencesKey("active_profile")
    private val threadProfilesKey = stringPreferencesKey("thread_profiles_json")
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }
    val profiles: Flow<List<ChatProfile>> = context.chatProfilesDataStore.data.map { p ->
        p[profilesKey]?.let { runCatching { json.decodeFromString<List<ChatProfile>>(it) }.getOrNull() }.orEmpty().ifEmpty { defaults() }
    }
    val activeProfileId: Flow<String?> = context.chatProfilesDataStore.data.map { it[activeKey] ?: "creative" }
    suspend fun getProfileForThread(threadId:String):String? = context.chatProfilesDataStore.data.map { prefs ->
        runCatching { json.decodeFromString<Map<String,String>>(prefs[threadProfilesKey].orEmpty()) }.getOrDefault(emptyMap())[threadId] ?: prefs[activeKey] ?: "creative"
    }.first()
    suspend fun setProfileForThread(threadId:String, profileId:String) = context.chatProfilesDataStore.edit { prefs ->
        val current=runCatching { json.decodeFromString<Map<String,String>>(prefs[threadProfilesKey].orEmpty()) }.getOrDefault(emptyMap()).toMutableMap()
        current[threadId]=profileId
        prefs[threadProfilesKey]=json.encodeToString<Map<String, String>>(current)
        prefs[activeKey]=profileId
    }
    suspend fun saveProfiles(value: List<ChatProfile>) = context.chatProfilesDataStore.edit { it[profilesKey] = json.encodeToString<List<ChatProfile>>(value) }
    suspend fun setActive(id: String) = context.chatProfilesDataStore.edit { it[activeKey] = id }
    private fun defaults() = listOf(
        ChatProfile("creative","Creative Studio",temperature=0.25,reasoningEnabled=true,webResearch=true,creativeAutonomy=true,contextMode=ContextMode.DEEP),
        ChatProfile("engineering","Engineering Agent",temperature=0.15,reasoningEnabled=true,webResearch=true,creativeAutonomy=true,contextMode=ContextMode.LARGE),
        ChatProfile("research","Research Analyst",temperature=0.2,reasoningEnabled=true,webResearch=true,creativeAutonomy=false,contextMode=ContextMode.DEEP),
        ChatProfile("local","Local Model",temperature=0.35,reasoningEnabled=true,webResearch=false,creativeAutonomy=true,contextMode=ContextMode.LARGE),
    )
}
