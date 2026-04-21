package com.example.googleAttractionsGpx

import com.example.googleAttractionsGpx.data.repository.NeedPhotoSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class NeedPhotoSettingsTest {

    @Test
    fun deserializeExclusions_usesNoPhotoDefaultsWhenEmpty() {
        assertEquals(
            listOf(
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
            ),
            NeedPhotoSettings.deserializeExclusions(null)
        )
    }

    @Test
    fun exclusions_roundTripWithoutLosingOrder() {
        val exclusions = listOf("hotel", "guest house", "siege")

        val serialized = NeedPhotoSettings.serializeExclusions(exclusions)
        val restored = NeedPhotoSettings.deserializeExclusions(serialized)

        assertEquals(exclusions, restored)
    }
}
