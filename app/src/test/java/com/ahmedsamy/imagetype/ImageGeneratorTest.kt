package com.ahmedsamy.imagetype

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.ahmedsamy.imagetype.util.ImageGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ImageGeneratorTest {
    private lateinit var generator: ImageGenerator

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        generator = ImageGenerator(context)
    }

    @Test
    fun parseDimensionsSquare() {
        val (w, h) = generator.parseDimensions("Square (1080x1080)")
        assertEquals(1080, w)
        assertEquals(1080, h)
    }

    @Test
    fun parseDimensionsStandardHD() {
        val (w, h) = generator.parseDimensions("Standard HD (1280x720)")
        assertEquals(1280, w)
        assertEquals(720, h)
    }

    @Test
    fun parseDimensionsPortrait() {
        val (w, h) = generator.parseDimensions("Portrait/Reels (1080x1920)")
        assertEquals(1080, w)
        assertEquals(1920, h)
    }

    @Test
    fun parseDimensionsUnknownReturnsDefault() {
        val (w, h) = generator.parseDimensions("Unknown Dimension")
        assertEquals(1080, w)
        assertEquals(1080, h)
    }

    @Test
    fun getHexColorWhite() {
        assertEquals(android.graphics.Color.WHITE, generator.getHexColor("White"))
    }

    @Test
    fun getHexColorBlack() {
        assertEquals(android.graphics.Color.BLACK, generator.getHexColor("Black"))
    }

    @Test
    fun getHexColorUnknownReturnsWhite() {
        assertEquals(android.graphics.Color.WHITE, generator.getHexColor("NonExistentColor"))
    }

    @Test
    fun isColorLightReturnsTrueForWhite() {
        assertTrue(generator.isColorLight("White"))
    }

    @Test
    fun isColorLightReturnsFalseForBlack() {
        assertFalse(generator.isColorLight("Black"))
    }

    @Test
    fun getExportQualityHigh() {
        assertEquals(100, ImageGenerator.getExportQualityInt("100% (High)"))
    }

    @Test
    fun getExportQualityMedium() {
        assertEquals(80, ImageGenerator.getExportQualityInt("80% (Medium)"))
    }

    @Test
    fun getExportQualityLow() {
        assertEquals(60, ImageGenerator.getExportQualityInt("60% (Low)"))
    }

    @Test
    fun getExportQualityUnknownReturns100() {
        assertEquals(100, ImageGenerator.getExportQualityInt("Unknown"))
    }
}
