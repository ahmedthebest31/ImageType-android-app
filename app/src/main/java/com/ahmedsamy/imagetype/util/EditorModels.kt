package com.ahmedsamy.imagetype.util

import com.ahmedsamy.imagetype.R

enum class TextColor(val value: String, val displayResId: Int) {
    White("White", R.string.color_white),
    Black("Black", R.string.color_black),
    Red("Red", R.string.color_red),
    Blue("Blue", R.string.color_blue),
    Green("Green", R.string.color_green),
    Yellow("Yellow", R.string.color_yellow),
    Orange("Orange", R.string.color_orange),
    Pink("Pink", R.string.color_pink),
    Purple("Purple", R.string.color_purple),
    Gray("Gray", R.string.color_gray),
    Cyan("Cyan", R.string.color_cyan),
    Magenta("Magenta", R.string.color_magenta),
    LightBlue("Light Blue", R.string.color_light_blue),
    LightGreen("Light Green", R.string.color_light_green),
    IslamicGreen("Islamic Green", R.string.color_islamic_green);

    val hexColor: Int get() = when (this) {
        White -> 0xFFFFFFFF.toInt()
        Black -> 0xFF000000.toInt()
        Red -> 0xFFE53935.toInt()
        Blue -> 0xFF1E88E5.toInt()
        Green -> 0xFF4CAF50.toInt()
        Yellow -> 0xFFFFEB3B.toInt()
        Orange -> 0xFFFB8C00.toInt()
        Pink -> 0xFFE91E63.toInt()
        Purple -> 0xFF8E24AA.toInt()
        Gray -> 0xFF757575.toInt()
        Cyan -> 0xFF00ACC1.toInt()
        Magenta -> 0xFFD81B60.toInt()
        LightBlue -> 0xFF90CAF9.toInt()
        LightGreen -> 0xFFA5D6A7.toInt()
        IslamicGreen -> 0xFF009933.toInt()
    }

    val isLight: Boolean get() = this in LIGHT_COLORS

    companion object {
        private val LIGHT_COLORS = setOf(White, Yellow, Pink, Cyan, LightBlue, LightGreen, IslamicGreen)
        fun fromValue(value: String): TextColor = entries.find { it.value == value } ?: White
    }
}

enum class BgType(val value: String, val displayResId: Int) {
    SolidColor("Solid Color", R.string.bg_solid),
    Transparent("Transparent", R.string.bg_transparent),
    ExistingImage("Existing Image", R.string.bg_existing);

    companion object {
        fun fromValue(value: String): BgType = entries.find { it.value == value } ?: SolidColor
    }
}

enum class TextPosition(val value: String, val displayResId: Int) {
    TopLeft("Top Left", R.string.pos_top_left),
    TopCenter("Top Center", R.string.pos_top_center),
    TopRight("Top Right", R.string.pos_top_right),
    MiddleLeft("Middle Left", R.string.pos_middle_left),
    Center("Center", R.string.pos_center),
    MiddleRight("Middle Right", R.string.pos_middle_right),
    BottomLeft("Bottom Left", R.string.pos_bottom_left),
    BottomCenter("Bottom Center", R.string.pos_bottom_center),
    BottomRight("Bottom Right", R.string.pos_bottom_right);

    companion object {
        fun fromValue(value: String): TextPosition = entries.find { it.value == value } ?: Center
    }
}

enum class ImageDimensions(val value: String, val displayResId: Int, val width: Int, val height: Int) {
    Standard("Standard (1920x1080)", R.string.dim_standard, 1920, 1080),
    StandardHD("Standard HD (1280x720)", R.string.dim_hd, 1280, 720),
    Square("Square (1080x1080)", R.string.dim_square, 1080, 1080),
    Portrait("Portrait/Reels (1080x1920)", R.string.dim_portrait, 1080, 1920),
    FourByThree("4:3 (1024x768)", R.string.dim_4_3, 1024, 768),
    ThreeByFour("3:4 (768x1024)", R.string.dim_3_4, 768, 1024),
    Ultrawide("Ultrawide (2560x1080)", R.string.dim_ultrawide, 2560, 1080),
    TwitterHeader("Twitter Header (1500x500)", R.string.dim_twitter, 1500, 500);

    companion object {
        fun fromValue(value: String): ImageDimensions = entries.find { it.value == value } ?: Square
    }
}

enum class FontStyle(val value: String, val displayResId: Int) {
    Regular("Regular", R.string.style_regular),
    Bold("Bold", R.string.style_bold),
    Italic("Italic", R.string.style_italic),
    BoldItalic("Bold-Italic", R.string.style_bold_italic);

    val androidTypefaceStyle: Int get() = when (this) {
        Regular -> android.graphics.Typeface.NORMAL
        Bold -> android.graphics.Typeface.BOLD
        Italic -> android.graphics.Typeface.ITALIC
        BoldItalic -> android.graphics.Typeface.BOLD_ITALIC
    }

    companion object {
        fun fromValue(value: String): FontStyle = entries.find { it.value == value } ?: Regular
    }
}

enum class ImageQuality(val value: String, val displayResId: Int, val qualityInt: Int) {
    High("100% (High)", R.string.qual_high, 100),
    Medium("80% (Medium)", R.string.qual_medium, 80),
    Low("60% (Low)", R.string.qual_low, 60);

    companion object {
        fun fromValue(value: String): ImageQuality = entries.find { it.value == value } ?: High
    }
}

enum class AppTheme(val value: String, val displayResId: Int) {
    Dark("Dark Mode", R.string.theme_dark),
    Light("Light Mode", R.string.theme_light),
    System("System Default", R.string.theme_system);

    companion object {
        fun fromValue(value: String): AppTheme = entries.find { it.value == value } ?: System
    }
}
