package com.example.googleAttractionsGpx.data.repository

import com.example.googleAttractionsGpx.domain.models.Coordinates
import com.example.googleAttractionsGpx.domain.models.PointData
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale

class NeedPhotoWikidataGpxGenerator(
    exclusions: List<String>,
) : GpxGeneratorBase() {

    private val normalizedExclusions = exclusions
        .map(NeedPhotoSettings::normalizeExclusion)
        .filter { it.isNotEmpty() }
        .toSet()

    private val systemLanguage = Locale.getDefault().language

    override fun getData(coordinates: Coordinates, radiusMeters: Int): List<PointData> {
        val response = queryNeedPhotoPlaces(coordinates, radiusMeters / 1000.0) ?: return emptyList()
        return parseNeedPhotoPlaces(response, normalizedExclusions)
    }

    private fun queryNeedPhotoPlaces(coordinates: Coordinates, radiusKm: Double): String? {
        val query = """
            SELECT ?q ?qLabel ?location ?image ?desc (GROUP_CONCAT(DISTINCT ?instanceOfLabel; separator=", ") AS ?instanceOfLabels) WHERE {
              {
                SELECT ?q ?location ?image ?desc ?autoLabel (MIN(?anyLabel) AS ?fallbackLabel) WHERE {
                  SERVICE wikibase:around {
                    ?q wdt:P625 ?location .
                    bd:serviceParam wikibase:center "Point(${coordinates.longitude} ${coordinates.latitude})"^^geo:wktLiteral .
                    bd:serviceParam wikibase:radius "$radiusKm"
                  }
                  OPTIONAL { ?q wdt:P18 ?image }
                  OPTIONAL { ?q wdt:P576 ?discontinuedDate }
                  OPTIONAL { ?q wdt:P5816 ?status }
                  FILTER(!BOUND(?discontinuedDate))
                  FILTER(!BOUND(?status) || ?status != wdt:Q56556915)
                  FILTER (!BOUND(?image))

                  SERVICE wikibase:label {
                    bd:serviceParam wikibase:language "$systemLanguage,en,[AUTO_LANGUAGE]" .
                    ?q schema:description ?desc .
                    ?q rdfs:label ?autoLabel .
                  }

                  OPTIONAL { ?q rdfs:label ?anyLabel . }
                }
                GROUP BY ?q ?location ?image ?desc ?autoLabel
              }

              OPTIONAL {
                ?q wdt:P31 ?instanceOf .
                ?instanceOf rdfs:label ?instanceOfLabel .
                FILTER (LANG(?instanceOfLabel) = "en")
              }

              BIND(IF(STRSTARTS(?autoLabel, "Q") && BOUND(?fallbackLabel), ?fallbackLabel, ?autoLabel) AS ?qLabel)
            }
            GROUP BY ?q ?qLabel ?location ?image ?desc
            LIMIT 3000
        """.trimIndent()

        val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
        val url = "https://query.wikidata.org/bigdata/namespace/wdq/sparql?query=$encodedQuery&format=json"

        return try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "GoogleAttractionsGpx/1.0")
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseNeedPhotoPlaces(response: String, exclusions: Set<String>): List<PointData> {
        val bindings = JSONObject(response)
            .optJSONObject("results")
            ?.optJSONArray("bindings")
            ?: return emptyList()

        val points = mutableListOf<PointData>()
        for (i in 0 until bindings.length()) {
            val binding = bindings.optJSONObject(i) ?: continue
            val place = parsePlace(binding) ?: continue
            if (isExcluded(place.instanceOfLabels, exclusions)) continue
            points.add(place.toPointData())
        }
        return points
    }

    private fun parsePlace(binding: JSONObject): NeedPhotoPlace? {
        val label = binding.optJSONObject("qLabel")?.optString("value").orEmpty().ifBlank { "Unknown" }
        val location = binding.optJSONObject("location")?.optString("value").orEmpty()
        val itemUrl = binding.optJSONObject("q")?.optString("value").orEmpty()
        val instanceOfLabels = binding.optJSONObject("instanceOfLabels")?.optString("value").orEmpty()
        val coordinates = parseWikidataPoint(location) ?: return null

        return NeedPhotoPlace(
            label = label,
            latitude = coordinates.first,
            longitude = coordinates.second,
            itemUrl = itemUrl,
            instanceOfLabels = instanceOfLabels,
        )
    }

    private fun parseWikidataPoint(locationValue: String): Pair<Double, Double>? {
        val match = POINT_REGEX.matchEntire(locationValue.trim()) ?: return null
        val longitude = match.groupValues[1].toDoubleOrNull() ?: return null
        val latitude = match.groupValues[2].toDoubleOrNull() ?: return null
        return latitude to longitude
    }

    private fun isExcluded(instanceOfLabels: String, exclusions: Set<String>): Boolean {
        if (instanceOfLabels.isBlank() || exclusions.isEmpty()) return false
        return instanceOfLabels
            .split(",")
            .map(NeedPhotoSettings::normalizeExclusion)
            .any(exclusions::contains)
    }

    private fun NeedPhotoPlace.toPointData(): PointData {
        return toPointData(
            label = label,
            latitude = latitude,
            longitude = longitude,
            itemUrl = itemUrl,
            instanceOfLabels = instanceOfLabels,
        )
    }

    private fun toPointData(
        label: String,
        latitude: Double,
        longitude: Double,
        itemUrl: String,
        instanceOfLabels: String,
    ): PointData {
        val description = buildString {
            append("Needs photo: yes")
            if (instanceOfLabels.isNotBlank()) {
                append("\nInstance of: ")
                append(instanceOfLabels)
            }
            if (itemUrl.isNotBlank()) {
                append("\nWikidata item: ")
                append(itemUrl)
            }
        }

        return PointData(
            coordinates = Coordinates(latitude, longitude),
            name = label,
            description = description,
        )
    }

    private data class NeedPhotoPlace(
        val label: String,
        val latitude: Double,
        val longitude: Double,
        val itemUrl: String,
        val instanceOfLabels: String,
    )

    companion object {
        private val POINT_REGEX = Regex("""Point\(([-\d.]+)\s+([-\d.]+)\)""")

        internal fun isExcludedForTest(instanceOfLabels: String, exclusions: List<String>): Boolean {
            val generator = NeedPhotoWikidataGpxGenerator(exclusions)
            return generator.isExcluded(instanceOfLabels, generator.normalizedExclusions)
        }

        internal fun pointFromRawValuesForTest(
            label: String,
            locationValue: String,
            itemUrl: String,
            instanceOfLabels: String,
        ): PointData? {
            val generator = NeedPhotoWikidataGpxGenerator(emptyList())
            val coordinates = generator.parseWikidataPoint(locationValue) ?: return null
            return generator.toPointData(
                label = label,
                latitude = coordinates.first,
                longitude = coordinates.second,
                itemUrl = itemUrl,
                instanceOfLabels = instanceOfLabels,
            )
        }
    }
}
