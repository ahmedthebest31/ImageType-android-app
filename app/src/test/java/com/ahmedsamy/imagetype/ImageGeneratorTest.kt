package com.ahmedsamy.imagetype

import android.content.Context
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.ahmedsamy.imagetype.util.ImageDimensions
import com.ahmedsamy.imagetype.util.ImageGenerator
import com.ahmedsamy.imagetype.util.ImageQuality
import com.ahmedsamy.imagetype.util.TextColor
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
        val dims = ImageDimensions.fromValue("Square (1080x1080)")
        assertEquals(1080, dims.width)
        assertEquals(1080, dims.height)
    }

    @Test
    fun parseDimensionsStandardHD() {
        val dims = ImageDimensions.fromValue("Standard HD (1280x720)")
        assertEquals(1280, dims.width)
        assertEquals(720, dims.height)
    }

    @Test
    fun parseDimensionsPortrait() {
        val dims = ImageDimensions.fromValue("Portrait/Reels (1080x1920)")
        assertEquals(1080, dims.width)
        assertEquals(1920, dims.height)
    }

    @Test
    fun parseDimensionsUnknownReturnsDefault() {
        val dims = ImageDimensions.fromValue("Unknown Dimension")
        assertEquals(1080, dims.width)
        assertEquals(1080, dims.height)
    }

    @Test
    fun getHexColorWhite() {
        assertEquals(Color.WHITE, TextColor.White.hexColor)
    }

    @Test
    fun getHexColorBlack() {
        assertEquals(Color.BLACK, TextColor.Black.hexColor)
    }

    @Test
    fun getHexColorUnknownReturnsWhite() {
        val resolved = TextColor.fromValue("NonExistentColor")
        assertEquals(Color.WHITE, resolved.hexColor)
    }

    @Test
    fun isColorLightReturnsTrueForWhite() {
        assertTrue(TextColor.White.isLight)
    }

    @Test
    fun isColorLightReturnsFalseForBlack() {
        assertFalse(TextColor.Black.isLight)
    }

    @Test
    fun getExportQualityHigh() {
        assertEquals(100, ImageQuality.fromValue("100% (High)").qualityInt)
    }

    @Test
    fun getExportQualityMedium() {
        assertEquals(80, ImageQuality.fromValue("80% (Medium)").qualityInt)
    }

    @Test
    fun getExportQualityLow() {
        assertEquals(60, ImageQuality.fromValue("60% (Low)").qualityInt)
    }

    @Test
    fun getExportQualityUnknownReturns100() {
        assertEquals(100, ImageQuality.fromValue("Unknown").qualityInt)
    }
}
