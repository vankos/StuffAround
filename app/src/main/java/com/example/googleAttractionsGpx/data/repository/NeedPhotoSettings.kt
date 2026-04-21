package com.example.googleAttractionsGpx.data.repository

object NeedPhotoSettings {
    val defaultExclusions = listOf(
        "hotel",
        "hostel",
        "guest house",
        "apartment",
        "neighborhood",
        "quarter",
        "mahalle",
        "battle",
        "ancient city",
        "siege",
    )

    fun serializeExclusions(exclusions: List<String>): String {
        return exclusions
            .map(::normalizeExclusion)
            .filter { it.isNotEmpty() }
            .joinToString("\n")
    }

    fun deserializeExclusions(raw: String?): List<String> {
        val parsed = raw
            .orEmpty()
            .lineSequence()
            .map(::normalizeExclusion)
            .filter { it.isNotEmpty() }
            .distinct()
            .toList()
        return parsed.ifEmpty { defaultExclusions }
    }

    fun normalizeExclusion(value: String): String = value.trim().lowercase()
}
