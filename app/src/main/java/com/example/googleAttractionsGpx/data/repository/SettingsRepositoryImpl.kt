package com.example.googleAttractionsGpx.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.googleAttractionsGpx.domain.repository.SettingsRepository
import androidx.core.content.edit

class SettingsRepositoryImpl
    (context: Context) : SettingsRepository {

    companion object {
        private const val PREFS_NAME = "MY_APP_PREFS"
        private const val GOOGLE_API_KEY = "API_KEY"
        private const val TRIP_ADVISOR_API_KEY = "TRIP_ADVISOR_API_KEY"
        private const val INATURALIST_USERNAME = "INATURALIST_USERNAME"
        private const val NEED_PHOTO_EXCLUSIONS = "NEED_PHOTO_EXCLUSIONS"
        private const val SELECTED_SOURCES = "SELECTED_SOURCES"
        private const val SOURCE_COLOR_PREFIX = "SOURCE_COLOR_"

        val DEFAULT_SOURCE_COLORS = mapOf(
            "google"   to "#4CAF50",
            "osm"      to "#2196F3",
            "trip"     to "#E53935",
            "wikidata" to "#9C27B0",
            "inat"     to "#FF9800",
            "wiki"     to "#607D8B",
            "nophoto"  to "#00BCD4",
        )
    }

    val sharedPrefs : SharedPreferences

    init {
        sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override var googleApiKey: String
        get() = sharedPrefs.getString(GOOGLE_API_KEY, "") ?: ""
        set(value) {
            sharedPrefs.edit() { putString(GOOGLE_API_KEY, value) }
        }
    override var tripAdvisorApiKey: String
        get() = sharedPrefs.getString(TRIP_ADVISOR_API_KEY, "") ?: ""
        set(value) {
            sharedPrefs.edit() { putString(TRIP_ADVISOR_API_KEY, value) }
        }

    override var iNaturalistUsername: String
        get() = sharedPrefs.getString(INATURALIST_USERNAME, "") ?: ""
        set(value) {
            sharedPrefs.edit() { putString(INATURALIST_USERNAME, value) }
        }

    override var needPhotoExclusions: List<String>
        get() = NeedPhotoSettings.deserializeExclusions(sharedPrefs.getString(NEED_PHOTO_EXCLUSIONS, null))
        set(value) {
            sharedPrefs.edit() { putString(NEED_PHOTO_EXCLUSIONS, NeedPhotoSettings.serializeExclusions(value)) }
        }

    override var selectedSources: Set<String>
        get() = sharedPrefs.getStringSet(SELECTED_SOURCES, setOf("google", "osm")) ?: setOf("google", "osm")
        set(value) {
            sharedPrefs.edit() { putStringSet(SELECTED_SOURCES, value) }
        }

    override var sourceColors: Map<String, String>
        get() = DEFAULT_SOURCE_COLORS.mapValues { (id, default) ->
            sharedPrefs.getString("$SOURCE_COLOR_PREFIX$id", default) ?: default
        }
        set(value) {
            sharedPrefs.edit() {
                value.forEach { (id, color) -> putString("$SOURCE_COLOR_PREFIX$id", color) }
            }
        }

}
