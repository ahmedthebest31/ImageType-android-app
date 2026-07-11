package com.ahmedsamy.imagetype

import android.content.Context
import android.graphics.Typeface
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object FontManager {
    private const val TAG = "FontManager"
    private const val CACHE_DIR = "fonts"

    private val bundledFonts = mapOf(
        "Cairo" to "fonts/cairo-regular.ttf",
        "Amiri" to "fonts/amiri-regular.ttf"
    )

    private val systemFonts = listOf(
        "Roboto", "Noto Sans", "Droid Sans"
    )

    private val downloadableFonts = mapOf(
        "Open Sans" to "Open Sans",
        "Montserrat" to "Montserrat",
        "Lato" to "Lato",
        "Oswald" to "Oswald",
        "Raleway" to "Raleway",
        "Noto Sans Arabic" to "Noto Sans Arabic"
    )

    val fontFamilies: List<String>
        get() = (bundledFonts.keys + systemFonts + downloadableFonts.keys).toList()

    private enum class FontSource { BUNDLED, SYSTEM, DOWNLOADABLE }

    private fun getFontSource(family: String): FontSource = when {
        family in bundledFonts -> FontSource.BUNDLED
        family in downloadableFonts -> FontSource.DOWNLOADABLE
        else -> FontSource.SYSTEM
    }

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

    private fun getCachedFile(context: Context, family: String, style: Int): File {
        val cacheDir = File(context.filesDir, CACHE_DIR)
        cacheDir.mkdirs()
        val suffix = when (style) {
            Typeface.BOLD -> "-bold"
            Typeface.ITALIC -> "-italic"
            Typeface.BOLD_ITALIC -> "-bolditalic"
            else -> "-regular"
        }
        val safeName = family.lowercase().replace(" ", "")
        return File(cacheDir, "$safeName$suffix.ttf")
    }

    private fun findCachedVariant(context: Context, family: String): File? {
        val cacheDir = File(context.filesDir, CACHE_DIR)
        if (!cacheDir.exists()) return null
        val safeName = family.lowercase().replace(" ", "")
        val variants = listOf("-regular", "-bold", "-italic", "-bolditalic")
        for (suffix in variants) {
            val file = File(cacheDir, "$safeName$suffix.ttf")
            if (file.exists() && file.length() > 0) return file
        }
        return null
    }

    private fun extractBundledFont(context: Context, assetPath: String, family: String, style: Int): File? {
        val file = getCachedFile(context, family, style)
        if (file.exists() && file.length() > 0) return file
        return try {
            context.assets.open(assetPath).use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
            file
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract bundled font: $assetPath", e)
            null
        }
    }

    private fun resolveGoogleFontsTtfUrl(family: String, weight: Int, italic: Boolean): String? {
        return try {
            val weightParam = if (italic) "$weight,1" else "$weight"
            val cssUrl = "https://fonts.googleapis.com/css2?family=${
                family.replace(" ", "+")
            }:wght@${if (italic) "${weight}i" else "$weight"}&display=swap"

            val conn = URL(cssUrl).openConnection() as HttpURLConnection
            conn.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 6.1; Trident/7.1; rv:11.0) like Gecko"
            )
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.connect()

            if (conn.responseCode == 200) {
                val css = conn.inputStream.bufferedReader().readText()
                val regex = """url\((https://[^)]+\.ttf)\)""".toRegex()
                regex.find(css)?.groupValues?.get(1)
            } else {
                Log.e(TAG, "Google Fonts CSS failed: HTTP ${conn.responseCode} for $family")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resolve Google Fonts URL for $family", e)
            null
        }
    }

    private suspend fun downloadFont(context: Context, family: String, style: Int): File? {
        return withContext(Dispatchers.IO) {
            try {
                val weight = when (style) {
                    Typeface.BOLD, Typeface.BOLD_ITALIC -> 700
                    else -> 400
                }
                val italic = style == Typeface.ITALIC || style == Typeface.BOLD_ITALIC

                val ttfUrl = resolveGoogleFontsTtfUrl(family, weight, italic)
                if (ttfUrl == null) {
                    Log.e(TAG, "Could not resolve TTF URL for $family")
                    return@withContext null
                }

                val file = getCachedFile(context, family, style)
                if (file.exists() && file.length() > 0) {
                    return@withContext file
                }

                Log.d(TAG, "Downloading font: $family (style=$style) from $ttfUrl")
                val conn = URL(ttfUrl).openConnection() as HttpURLConnection
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                conn.connect()

                if (conn.responseCode == 200) {
                    conn.inputStream.use { input ->
                        file.outputStream().use { output -> input.copyTo(output) }
                    }
                    Log.d(TAG, "Font saved: ${file.absolutePath} (${file.length()} bytes)")
                    file
                } else {
                    Log.e(TAG, "Font download failed: HTTP ${conn.responseCode}")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to download font: $family", e)
                null
            }
        }
    }

    suspend fun resolveTypeface(
        context: Context,
        family: String,
        style: Int,
        isArabic: Boolean
    ): Typeface {
        val selectedFamily = if (isArabic && isEnglishOnly(family)) "Amiri" else family

        when (getFontSource(selectedFamily)) {
            FontSource.BUNDLED -> {
                val assetPath = bundledFonts[selectedFamily]
                    ?: return Typeface.create("sans-serif", style)
                return try {
                    val file = extractBundledFont(context, assetPath, selectedFamily, style)
                        ?: return Typeface.create("sans-serif", style)
                    val base = Typeface.createFromFile(file)
                    if (style != Typeface.NORMAL) Typeface.create(base, style) else base
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load bundled font: $selectedFamily", e)
                    Typeface.create("sans-serif", style)
                }
            }

            FontSource.SYSTEM -> {
                val cached = findCachedVariant(context, selectedFamily)
                if (cached != null) {
                    return try {
                        val base = Typeface.createFromFile(cached)
                        if (style != Typeface.NORMAL) Typeface.create(base, style) else base
                    } catch (e: Exception) {
                        Typeface.create("sans-serif", style)
                    }
                }
                return Typeface.create("sans-serif", style)
            }

            FontSource.DOWNLOADABLE -> {
                val cached = getCachedFile(context, selectedFamily, style)
                if (cached.exists() && cached.length() > 0) {
                    return try {
                        Typeface.createFromFile(cached)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to load cached font: $selectedFamily", e)
                        Typeface.create("sans-serif", style)
                    }
                }

                val file = downloadFont(context, selectedFamily, style)
                if (file != null) {
                    return try {
                        Typeface.createFromFile(file)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to load downloaded font: $selectedFamily", e)
                        Typeface.create("sans-serif", style)
                    }
                }

                return Typeface.create("sans-serif", style)
            }
        }
    }

    @Deprecated("Use resolveTypeface() for async font loading. This is a synchronous fallback only.")
    fun getTypeface(context: Context, family: String, style: Int, isArabic: Boolean): Typeface {
        val selectedFamily = if (isArabic && isEnglishOnly(family)) "Amiri" else family
        val assetPath = bundledFonts[selectedFamily]
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
            "Roboto", "Open Sans", "Lato", "Montserrat", "Oswald", "Raleway", "Noto Sans", "Droid Sans" ->
                Typeface.create("sans-serif", style)
            else -> Typeface.defaultFromStyle(style)
        }
    }
}
