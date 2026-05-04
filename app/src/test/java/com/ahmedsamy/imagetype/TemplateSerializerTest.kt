package com.ahmedsamy.imagetype

import com.ahmedsamy.imagetype.util.Template
import com.ahmedsamy.imagetype.util.TemplateSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TemplateSerializerTest {
    private val serializer = TemplateSerializer()

    @Test
    fun roundTripPreservesSingleTemplate() {
        val original = listOf(
            Template(
                id = "1", name = "Test", text = "Hello", fitText = true,
                fontSize = 60f, textPosition = "Center", fontFamily = "Amiri",
                fontStyle = "Regular", textColor = "White", textShadow = true,
                dimensions = "Square (1080x1080)", bgType = "Solid Color",
                bgColor = "Black", imageQuality = "100% (High)"
            )
        )
        val json = serializer.toJson(original)
        val restored = serializer.fromJson(json)
        assertEquals(original, restored)
    }

    @Test
    fun roundTripPreservesMultipleTemplates() {
        val original = listOf(
            Template("1", "First", "Text1", true, 60f, "Center", "Amiri", "Regular",
                "White", true, "Square (1080x1080)", "Solid Color", "Black", "100% (High)"),
            Template("2", "Second", "Text2", false, 40f, "Top Left", "Roboto", "Bold",
                "Red", false, "HD (1280x720)", "Transparent", "N/A", "80% (Medium)")
        )
        val json = serializer.toJson(original)
        val restored = serializer.fromJson(json)
        assertEquals(original, restored)
    }

    @Test
    fun fromJsonNullLiteralReturnsEmptyList() {
        val result = serializer.fromJson("null")
        assertTrue(result.isEmpty())
    }
}
