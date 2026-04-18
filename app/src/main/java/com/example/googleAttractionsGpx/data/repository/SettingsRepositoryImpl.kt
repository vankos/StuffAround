package com.example.googleAttractionsGpx.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.googleAttractionsGpx.domain.repository.SettingsRepository
import androidx.core.content.edit

class SettingsRepositoryImpl
    (context: Context) : SettingsRepository {

    val sharedPrefs : SharedPreferences

    init {
        sharedPrefs = context.getSharedPreferences("MY_APP_PREFS", Context.MODE_PRIVATE)
    }

    override var googleApiKey: String
        get() = sharedPrefs.getString("API_KEY", "") ?: ""
        set(value) {
            sharedPrefs.edit() { putString("API_KEY", value) }
        }
    override var tripAdvisorApiKey: String
        get() = sharedPrefs.getString("TRIP_ADVISOR_API_KEY", "") ?: ""
        set(value) {
            sharedPrefs.edit() { putString("TRIP_ADVISOR_API_KEY", value) }
        }

    override var iNaturalistUsername: String
        get() = sharedPrefs.getString("INATURALIST_USERNAME", "") ?: ""
        set(value) {
            sharedPrefs.edit() { putString("INATURALIST_USERNAME", value) }
        }

    override var selectedSources: Set<String>
        get() = sharedPrefs.getStringSet("SELECTED_SOURCES", setOf("google", "osm")) ?: setOf("google", "osm")
        set(value) {
            sharedPrefs.edit() { putStringSet("SELECTED_SOURCES", value) }
        }

}