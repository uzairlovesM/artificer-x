package com.waheed.artificerx.core.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Section 76/198 Security: API keys never touch plaintext SharedPreferences,
 * a plaintext file, or logs. Backed by AndroidX Security Crypto's
 * EncryptedSharedPreferences (AES256-GCM values, AES256-SIV keys) with a
 * hardware-backed MasterKey where the device supports StrongBox/TEE,
 * falling back transparently to a software-backed keystore key otherwise.
 *
 * This class is the ONLY place in the app that ever holds a raw API key
 * string in memory outside of the moment it's attached to an outgoing
 * HTTP request (via LLMAdapter — see core.network). Every other layer
 * (UI, domain models, logs) only ever sees a keyAlias + masked preview.
 */
@Singleton
class EncryptedKeyStore
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val masterKey =
            MasterKey
                .Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

        private val prefs: SharedPreferences =
            EncryptedSharedPreferences.create(
                context,
                FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )

        fun storeKey(
            keyAlias: String,
            rawKeyValue: String,
        ) {
            prefs.edit().putString(keyAlias, rawKeyValue).apply()
        }

        fun retrieveKey(keyAlias: String): String? = prefs.getString(keyAlias, null)

        fun deleteKey(keyAlias: String) {
            prefs.edit().remove(keyAlias).apply()
        }

        fun hasKey(keyAlias: String): Boolean = prefs.contains(keyAlias)

        /** Section 198: masked preview for display — never the full key. */
        fun maskedPreview(rawKeyValue: String): String {
            if (rawKeyValue.length <= 8) return "•".repeat(rawKeyValue.length.coerceAtLeast(4))
            val visibleStart = rawKeyValue.take(4)
            val visibleEnd = rawKeyValue.takeLast(4)
            return "$visibleStart${"•".repeat(6)}$visibleEnd"
        }

        fun clearAllKeys() {
            prefs.edit().clear().apply()
        }

        companion object {
            private const val FILE_NAME = "artificerx_encrypted_keys"
        }
    }
