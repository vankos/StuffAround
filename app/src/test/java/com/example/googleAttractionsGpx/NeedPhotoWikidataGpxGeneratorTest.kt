package com.example.googleAttractionsGpx

import com.example.googleAttractionsGpx.data.repository.NeedPhotoWikidataGpxGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NeedPhotoWikidataGpxGeneratorTest {

    @Test
    fun excludedCategories_matchIgnoringCaseAndSpaces() {
        assertTrue(
            NeedPhotoWikidataGpxGenerator.isExcludedForTest(
                "Monument, Guest House, Museum",
                listOf(" guest house ", "hotel")
            )
        )
    }

    @Test
    fun excludedCategories_keepNonMatchingObjects() {
        assertFalse(
            NeedPhotoWikidataGpxGenerator.isExcludedForTest(
                "Monument, Museum",
                listOf("hotel", "guest house")
            )
        )
    }

    @Test
    fun pointFromRawValues_mapsValidValuesToPointData() {
        val point = NeedPhotoWikidataGpxGenerator.pointFromRawValuesForTest(
            label = "Ancient Gate",
            locationValue = "Point(30.5 50.4)",
            itemUrl = "https://www.wikidata.org/entity/Q1",
            instanceOfLabels = "monument, gate"
        )

        requireNotNull(point)
        assertEquals("Ancient Gate", point.name)
        assertEquals(50.4, point.coordinates.latitude, 0.0)
        assertEquals(30.5, point.coordinates.longitude, 0.0)
        assertTrue(point.description.contains("Needs photo: yes"))
        assertTrue(point.description.contains("monument, gate"))
        assertTrue(point.description.contains("https://www.wikidata.org/entity/Q1"))
    }

    @Test
    fun pointFromRawValues_returnsNullForInvalidCoordinates() {
        val point = NeedPhotoWikidataGpxGenerator.pointFromRawValuesForTest(
            label = "Broken Entry",
            locationValue = "not-a-point",
            itemUrl = "https://www.wikidata.org/entity/Q3",
            instanceOfLabels = "museum"
        )

        assertEquals(null, point)
    }
}
