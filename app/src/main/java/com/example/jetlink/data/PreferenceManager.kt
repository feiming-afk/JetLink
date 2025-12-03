package com.example.jetlink.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferenceManager(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("jetlink_prefs", Context.MODE_PRIVATE)

    private val _chatBackgroundUri = MutableStateFlow<String?>(sharedPreferences.getString(KEY_CHAT_BG, null))
    val chatBackgroundUri: StateFlow<String?> = _chatBackgroundUri.asStateFlow()

    fun saveChatBackground(uri: String) {
        sharedPreferences.edit {
            putString(KEY_CHAT_BG, uri)
        }
        _chatBackgroundUri.value = uri
    }

    companion object {
        private const val KEY_CHAT_BG = "chat_background_uri"
    }
}
