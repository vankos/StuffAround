package com.example.googleAttractionsGpx.domain.repository

interface SettingsRepository {
    var googleApiKey: String
    var tripAdvisorApiKey: String
    var iNaturalistUsername: String
}