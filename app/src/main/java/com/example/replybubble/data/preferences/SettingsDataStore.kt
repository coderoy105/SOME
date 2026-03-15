package com.example.replybubble.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.replybubble.domain.model.AppSettings
import com.example.replybubble.domain.model.RelationshipType
import com.example.replybubble.domain.model.ToneStyle
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "replybubble_settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val onboardingCompleted = booleanPreferencesKey("onboarding_completed")
        val defaultRelationshipType = stringPreferencesKey("default_relationship_type")
        val defaultToneStyle = stringPreferencesKey("default_tone_style")
        val autoSaveHistory = booleanPreferencesKey("auto_save_history")
        val limitPunctuationOveruse = booleanPreferencesKey("limit_punctuation_overuse")
        val limitLaughOveruse = booleanPreferencesKey("limit_laugh_overuse")
        val overlayScale = floatPreferencesKey("overlay_scale")
    }

    val settingsFlow: Flow<AppSettings> = context.settingsDataStore.data.map { preferences ->
        AppSettings(
            onboardingCompleted = preferences[Keys.onboardingCompleted] ?: false,
            defaultRelationshipType = enumOrDefault(
                preferences[Keys.defaultRelationshipType],
                RelationshipType.FRIEND,
            ),
            defaultToneStyle = enumOrDefault(
                preferences[Keys.defaultToneStyle],
                ToneStyle.CASUAL,
            ),
            autoSaveHistory = preferences[Keys.autoSaveHistory] ?: true,
            limitPunctuationOveruse = preferences[Keys.limitPunctuationOveruse] ?: false,
            limitLaughOveruse = preferences[Keys.limitLaughOveruse] ?: false,
            overlayScale = (preferences[Keys.overlayScale] ?: 1f).coerceIn(0.8f, 1.2f),
        )
    }

    suspend fun update(transform: suspend (MutableSettings) -> Unit) {
        context.settingsDataStore.edit { preferences ->
            val mutableSettings = MutableSettings.from(preferences)
            transform(mutableSettings)
            mutableSettings.writeTo(preferences)
        }
    }

    suspend fun reset() {
        context.settingsDataStore.edit { it.clear() }
    }

    private fun <T : Enum<T>> enumOrDefault(value: String?, default: T): T {
        return default.declaringJavaClass.enumConstants
            ?.firstOrNull { it.name == value }
            ?: default
    }

    class MutableSettings(
        var onboardingCompleted: Boolean,
        var defaultRelationshipType: RelationshipType,
        var defaultToneStyle: ToneStyle,
        var autoSaveHistory: Boolean,
        var limitPunctuationOveruse: Boolean,
        var limitLaughOveruse: Boolean,
        var overlayScale: Float,
    ) {
        fun writeTo(preferences: MutablePreferences) {
            preferences[Keys.onboardingCompleted] = onboardingCompleted
            preferences[Keys.defaultRelationshipType] = defaultRelationshipType.name
            preferences[Keys.defaultToneStyle] = defaultToneStyle.name
            preferences[Keys.autoSaveHistory] = autoSaveHistory
            preferences[Keys.limitPunctuationOveruse] = limitPunctuationOveruse
            preferences[Keys.limitLaughOveruse] = limitLaughOveruse
            preferences[Keys.overlayScale] = overlayScale.coerceIn(0.8f, 1.2f)
        }

        companion object {
            fun from(preferences: Preferences): MutableSettings {
                return MutableSettings(
                    onboardingCompleted = preferences[Keys.onboardingCompleted] ?: false,
                    defaultRelationshipType = runCatching {
                        enumValueOf<RelationshipType>(
                            preferences[Keys.defaultRelationshipType] ?: RelationshipType.FRIEND.name,
                        )
                    }.getOrDefault(RelationshipType.FRIEND),
                    defaultToneStyle = runCatching {
                        enumValueOf<ToneStyle>(
                            preferences[Keys.defaultToneStyle] ?: ToneStyle.CASUAL.name,
                        )
                    }.getOrDefault(ToneStyle.CASUAL),
                    autoSaveHistory = preferences[Keys.autoSaveHistory] ?: true,
                    limitPunctuationOveruse = preferences[Keys.limitPunctuationOveruse] ?: false,
                    limitLaughOveruse = preferences[Keys.limitLaughOveruse] ?: false,
                    overlayScale = (preferences[Keys.overlayScale] ?: 1f).coerceIn(0.8f, 1.2f),
                )
            }
        }
    }
}
