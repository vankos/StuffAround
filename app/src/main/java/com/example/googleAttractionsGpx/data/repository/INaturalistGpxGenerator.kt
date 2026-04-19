package com.example.googleAttractionsGpx.data.repository

import com.example.googleAttractionsGpx.domain.models.Coordinates
import com.example.googleAttractionsGpx.domain.models.PointData
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate
import java.time.MonthDay
import java.util.Locale

class INaturalistGpxGenerator(private val username: String) : GpxGeneratorBase() {

    private data class Observation(
        val taxonId: Int,
        val commonName: String,
        val scientificName: String,
        val latitude: Double,
        val longitude: Double,
        val observedOn: String,
        val id: Long,
        val dayOfYear: Int
    )

    override fun getData(coordinates: Coordinates, radiusMeters: Int): List<PointData> {
        val userId = resolveUserId(username)
        val today = LocalDate.now()
        val months = getMonthsInWindow(today, 15)
        val radiusKm = radiusMeters / 1000

        val allObservations = fetchAllObservations(
            userId = userId,
            lat = coordinates.latitude,
            lng = coordinates.longitude,
            radiusKm = radiusKm,
            months = months
        )

        val todayDoy = today.dayOfYear
        val filtered = allObservations.filter { obs ->
            isDayInWindow(obs.dayOfYear, todayDoy, 15, isLeapYear = false)
        }

        if (filtered.isEmpty()) {
            throw Exception("No unobserved species found near this location in the ±15 day window.")
        }

        val grouped = filtered.groupBy { it.taxonId }
        val topTaxonId = grouped.maxByOrNull { it.value.size }!!.key
        val topObservations = grouped[topTaxonId]!!
        val speciesName = topObservations.first().let {
            it.commonName.ifEmpty { it.scientificName }
        }

        return topObservations.map { obs ->
            PointData(
                coordinates = Coordinates(obs.latitude, obs.longitude),
                name = speciesName,
                description = "Observed: ${obs.observedOn}\nhttps://www.inaturalist.org/observations/${obs.id}"
            )
        }
    }

    private fun resolveUserId(login: String): Int {
        val url = "https://api.inaturalist.org/v2/users/autocomplete?q=${URLEncoder.encode(login, "UTF-8")}&per_page=1"
        val json = fetchJson(url)
        val results = json.optJSONArray("results")
            ?: throw Exception("Cannot resolve iNaturalist user: $login")

        if (results.length() == 0) {
            throw Exception("iNaturalist user not found: $login")
        }
        return results.getJSONObject(0).getInt("id")
    }

    private fun getMonthsInWindow(today: LocalDate, days: Int): Set<Int> {
        val months = mutableSetOf<Int>()
        for (offset in -days..days) {
            months.add(today.plusDays(offset.toLong()).monthValue)
        }
        return months
    }

    private fun isDayInWindow(obsDoy: Int, todayDoy: Int, windowDays: Int, isLeapYear: Boolean): Boolean {
        val daysInYear = if (isLeapYear) 366 else 365
        val diff = Math.abs(obsDoy - todayDoy)
        val wrappedDiff = minOf(diff, daysInYear - diff)
        return wrappedDiff <= windowDays
    }

    private fun fetchAllObservations(
        userId: Int,
        lat: Double,
        lng: Double,
        radiusKm: Int,
        months: Set<Int>
    ): List<Observation> {
        val allObs = mutableListOf<Observation>()
        var page = 1
        val perPage = 200
        val monthParam = months.sorted().joinToString(",")
        val fields = "(id:!t,taxon:(id:!t,preferred_common_name:!t,name:!t),location:!t,observed_on:!t)"
        val locale = Locale.getDefault().language

        while (true) {
            val url = "https://api.inaturalist.org/v2/observations" +
                    "?unobserved_by_user_id=$userId" +
                    "&lat=$lat" +
                    "&lng=$lng" +
                    "&radius=$radiusKm" +
                    "&month=$monthParam" +
                    "&per_page=$perPage" +
                    "&page=$page" +
                    "&locale=$locale" +
                    "&fields=$fields"

            val json = fetchJson(url)
            val results = json.optJSONArray("results") ?: break
            val totalResults = json.optInt("total_results", 0)

            for (i in 0 until results.length()) {
                val obs = results.getJSONObject(i)
                val taxon = obs.optJSONObject("taxon") ?: continue
                val location = obs.optString("location", "")
                if (location.isEmpty()) continue

                val parts = location.split(",")
                if (parts.size != 2) continue
                val obsLat = parts[0].trim().toDoubleOrNull() ?: continue
                val obsLng = parts[1].trim().toDoubleOrNull() ?: continue

                val observedOn = obs.optString("observed_on", "")
                val doy = try {
                    LocalDate.parse(observedOn).dayOfYear
                } catch (e: Exception) {
                    continue
                }

                allObs.add(
                    Observation(
                        taxonId = taxon.getInt("id"),
                        commonName = taxon.optString("preferred_common_name", ""),
                        scientificName = taxon.optString("name", "Unknown"),
                        latitude = obsLat,
                        longitude = obsLng,
                        observedOn = observedOn,
                        id = obs.optLong("id", 0L),
                        dayOfYear = doy
                    )
                )
            }

            if (page * perPage >= totalResults) break
            page++

            Thread.sleep(100)
        }

        return allObs
    }

    private fun fetchJson(urlString: String): JSONObject {
        val connection = URL(urlString).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("User-Agent", "GoogleAttractionsGpx/1.0 (iNaturalist integration)")
        connection.setRequestProperty("Accept", "application/json")
        connection.connectTimeout = 15000
        connection.readTimeout = 15000

        val response = connection.inputStream.bufferedReader().use { it.readText() }
        return JSONObject(response)
    }
}
