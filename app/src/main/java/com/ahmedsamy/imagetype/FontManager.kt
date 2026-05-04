package com.ahmedsamy.imagetype

import android.content.Context
import android.graphics.Typeface
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

object FontManager {
    private const val TAG = "FontManager"

    val fontFamilies = listOf(
        "Amiri",
        "Cairo",
        "Roboto",
        "Open Sans",
        "Lato",
        "Montserrat",
        "Oswald",
        "Raleway",
        "Noto Sans"
    )

    private val fontUrls = mapOf(
        "Amiri" to "https://raw.githubusercontent.com/google/fonts/main/ofl/amiri/Amiri-Regular.ttf",
        "Cairo" to "https://raw.githubusercontent.com/google/fonts/main/ofl/cairo/Cairo-Regular.ttf",
        "Montserrat" to "https://raw.githubusercontent.com/google/fonts/main/ofl/montserrat/Montserrat-Regular.ttf",
        "Oswald" to "https://raw.githubusercontent.com/google/fonts/main/ofl/oswald/Oswald-Regular.ttf",
        "Open Sans" to "https://raw.githubusercontent.com/google/fonts/main/ofl/opensans/OpenSans-Regular.ttf",
        "Lato" to "https://github.com/google/fonts/raw/main/ofl/lato/Lato-Regular.ttf",
        "Raleway" to "https://raw.githubusercontent.com/google/fonts/main/ofl/raleway/Raleway-Regular.ttf",
        "Noto Sans" to "https://raw.githubusercontent.com/google/fonts/main/ofl/notosans/NotoSans-Regular.ttf"
    )

    fun isEnglishOnly(family: String): Boolean {
        return family in listOf("Roboto", "Open Sans", "Lato", "Montserrat", "Oswald", "Raleway")
    }

    fun containsArabic(text: String): Boolean {
        for (char in text) {
            val block = Character.UnicodeBlock.of(char)
            if (block == Character.UnicodeBlock.ARABIC ||
                block == Character.UnicodeBlock.ARABIC_PRESENTATION_FORMS_A ||
                block == Character.UnicodeBlock.ARABIC_PRESENTATION_FORMS_B ||
                block == Character.UnicodeBlock.ARABIC_SUPPLEMENT
            ) {
                return true
            }
        }
        return false
    }

    fun getTypeface(context: Context, family: String, style: Int, isArabic: Boolean): Typeface {
        // Fallback engine: force Amiri if text has Arabic but chosen font is English-only
        val selectedFamily = if (isArabic && isEnglishOnly(family)) {
            "Amiri"
        } else {
            family
        }

        val fontDir = context.filesDir.resolve("fonts")
        val fontFile = fontDir.resolve("${selectedFamily.lowercase().replace(" ", "")}-regular.ttf")

        if (fontFile.exists() && fontFile.length() > 0) {
            try {
                val fileTypeface = Typeface.createFromFile(fontFile)
                if (style != Typeface.NORMAL) {
                    return Typeface.create(fileTypeface, style)
                }
                return fileTypeface
            } catch (e: Exception) {
                Log.e(TAG, "Error loading typeface from file: ${fontFile.absolutePath}", e)
            }
        }

        // Offline system fallbacks
        return when (selectedFamily) {
            "Amiri" -> Typeface.create("serif", style)
            "Cairo" -> Typeface.create("sans-serif", style)
            "Oswald" -> Typeface.create("sans-serif-condensed", style)
            "Roboto", "Open Sans", "Lato", "Montserrat", "Raleway", "Noto Sans" -> Typeface.create("sans-serif", style)
            else -> Typeface.defaultFromStyle(style)
        }
    }

    suspend fun downloadFontIfMissing(context: Context, family: String, onComplete: () -> Unit) {
        val urlString = fontUrls[family] ?: return
        val fontDir = context.filesDir.resolve("fonts")
        if (!fontDir.exists()) {
            fontDir.mkdirs()
        }

        val fontFile = fontDir.resolve("${family.lowercase().replace(" ", "")}-regular.ttf")
        if (fontFile.exists() && fontFile.length() > 0) {
            withContext(Dispatchers.Main) { onComplete() }
            return
        }

        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Downloading font $family from $urlString")
                val url = URL(urlString)
                url.openStream().use { input ->
                    fontFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                Log.d(TAG, "Font $family downloaded successfully to ${fontFile.absolutePath}")
                withContext(Dispatchers.Main) {
                    onComplete()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to download font $family", e)
            }
        }
    }
}
