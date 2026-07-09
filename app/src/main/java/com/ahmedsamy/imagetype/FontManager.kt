package com.ahmedsamy.imagetype

import android.content.Context
import android.graphics.Typeface
import android.util.Log

object FontManager {
    private const val TAG = "FontManager"

    private val fontAssetMap = mapOf(
        "Amiri" to "fonts/amiri-regular.ttf",
        "Cairo" to "fonts/cairo-regular.ttf",
        "Roboto" to null,
        "Open Sans" to "fonts/opensans-regular.ttf",
        "Lato" to "fonts/lato-regular.ttf",
        "Montserrat" to "fonts/montserrat-regular.ttf",
        "Oswald" to "fonts/oswald-regular.ttf",
        "Raleway" to "fonts/raleway-regular.ttf",
        "Noto Sans" to "fonts/notosans-regular.ttf"
    )

    val fontFamilies: List<String> get() = fontAssetMap.keys.toList()

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
        val selectedFamily = if (isArabic && isEnglishOnly(family)) {
            "Amiri"
        } else {
            family
        }

        val assetPath = fontAssetMap[selectedFamily]
        if (assetPath != null) {
            try {
                val tf = Typeface.createFromAsset(context.assets, assetPath)
                return if (style != Typeface.NORMAL) Typeface.create(tf, style) else tf
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load font from assets: $assetPath", e)
            }
        }

        return when (selectedFamily) {
            "Amiri" -> Typeface.create("serif", style)
            "Cairo" -> Typeface.create("sans-serif", style)
            "Oswald" -> Typeface.create("sans-serif-condensed", style)
            "Roboto", "Open Sans", "Lato", "Montserrat", "Raleway", "Noto Sans" -> Typeface.create("sans-serif", style)
            else -> Typeface.defaultFromStyle(style)
        }
    }
}
