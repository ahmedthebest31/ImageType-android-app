package com.ahmedsamy.imagetype.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.util.Log
import com.ahmedsamy.imagetype.FontManager

class ImageGenerator(private val context: Context) {
    private val TAG = "ImageGenerator"
    @Volatile private var cachedBgUri: Uri? = null
    @Volatile private var cachedBgBitmap: Bitmap? = null

    fun generate(
        inputText: String,
        fitText: Boolean,
        fontSize: Float,
        textPosition: String,
        fontFamily: String,
        fontStyle: String,
        textColor: String,
        textShadow: Boolean,
        dimensions: String,
        bgType: String,
        bgColor: String,
        bgImageUri: Uri?
    ): Bitmap? {
        return try {
            val size = parseDimensions(dimensions)
            val width = size.first
            val height = size.second

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            drawBackground(canvas, width, height, bgType, bgColor, bgImageUri)

            val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
            val isArabic = FontManager.containsArabic(inputText)
            val paintStyle = when (fontStyle) {
                "Bold" -> Typeface.BOLD
                "Italic" -> Typeface.ITALIC
                "Bold-Italic" -> Typeface.BOLD_ITALIC
                else -> Typeface.NORMAL
            }
            val typeface = FontManager.getTypeface(context, fontFamily, paintStyle, isArabic)
            textPaint.typeface = typeface

            val paddingX = width * 0.08f
            val paddingY = height * 0.08f
            val maxTextWidth = (width - (paddingX * 2)).coerceAtLeast(100f)
            val maxTextHeight = (height - (paddingY * 2)).coerceAtLeast(100f)

            var finalFontSize = fontSize
            if (fitText) {
                finalFontSize = binarySearchBestFontSize(inputText, maxTextWidth, maxTextHeight, textPaint)
            }
            textPaint.textSize = finalFontSize

            val textAlignment = getLayoutAlignment(textPosition, isArabic)
            val textDir = if (FontManager.containsArabic(inputText)) {
                TextDirectionHeuristics.FIRSTSTRONG_LTR
            } else {
                TextDirectionHeuristics.LOCALE
            }
            val staticLayout = StaticLayout.Builder.obtain(
                inputText, 0, inputText.length, textPaint, maxTextWidth.toInt()
            )
                .setAlignment(textAlignment)
                .setTextDirection(textDir)
                .setLineSpacing(0f, 1.1f)
                .setIncludePad(false)
                .build()

            val layoutWidth = staticLayout.width
            val layoutHeight = staticLayout.height
            val translationXY = calculateTranslation(
                textPosition, width.toFloat(), height.toFloat(),
                layoutWidth.toFloat(), layoutHeight.toFloat(), paddingX, paddingY
            )
            val tx = translationXY.first
            val ty = translationXY.second

            // Stroke outline (for WCAG high contrast legibility)
            if (textShadow) {
                textPaint.style = Paint.Style.STROKE
                textPaint.strokeWidth = finalFontSize * 0.08f
                textPaint.strokeJoin = Paint.Join.ROUND
                textPaint.color = if (isColorLight(textColor)) Color.BLACK else Color.WHITE

                canvas.save()
                canvas.translate(tx, ty)
                staticLayout.draw(canvas)
                canvas.restore()
            }

            // Render filled text
            textPaint.style = Paint.Style.FILL
            textPaint.strokeWidth = 0f
            textPaint.color = getHexColor(textColor)
            if (textShadow) {
                val shadowColor = if (isColorLight(textColor)) 0x80000000.toInt() else 0x80FFFFFF.toInt()
                textPaint.setShadowLayer(8f, 3f, 3f, shadowColor)
            } else {
                textPaint.clearShadowLayer()
            }

            canvas.save()
            canvas.translate(tx, ty)
            staticLayout.draw(canvas)
            canvas.restore()

            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Exception during native image generation rendering", e)
            null
        }
    }

    private fun binarySearchBestFontSize(
        text: String,
        maxWidth: Float,
        maxHeight: Float,
        paint: TextPaint,
        minSize: Float = 10f,
        maxSize: Float = 400f
    ): Float {
        var low = minSize
        var high = maxSize
        var bestSize = low
        for (i in 0..12) {
            val mid = (low + high) / 2
            paint.textSize = mid
            val testTextDir = if (FontManager.containsArabic(text)) {
                TextDirectionHeuristics.FIRSTSTRONG_LTR
            } else {
                TextDirectionHeuristics.LOCALE
            }
            val testLayout = StaticLayout.Builder.obtain(text, 0, text.length, paint, maxWidth.toInt())
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setTextDirection(testTextDir)
                .setLineSpacing(0f, 1.1f)
                .setIncludePad(false)
                .build()
            if (testLayout.height <= maxHeight) {
                bestSize = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return bestSize
    }

    private fun calculateTranslation(
        pos: String,
        canvasW: Float,
        canvasH: Float,
        layoutW: Float,
        layoutH: Float,
        padX: Float,
        padY: Float
    ): Pair<Float, Float> {
        val x: Float
        val y: Float
        when (pos) {
            "Top Left" -> { x = padX; y = padY }
            "Top Center" -> { x = (canvasW - layoutW) / 2f; y = padY }
            "Top Right" -> { x = canvasW - layoutW - padX; y = padY }
            "Middle Left" -> { x = padX; y = (canvasH - layoutH) / 2f }
            "Center" -> { x = (canvasW - layoutW) / 2f; y = (canvasH - layoutH) / 2f }
            "Middle Right" -> { x = canvasW - layoutW - padX; y = (canvasH - layoutH) / 2f }
            "Bottom Left" -> { x = padX; y = canvasH - layoutH - padY }
            "Bottom Center" -> { x = (canvasW - layoutW) / 2f; y = canvasH - layoutH - padY }
            "Bottom Right" -> { x = canvasW - layoutW - padX; y = canvasH - layoutH - padY }
            else -> { x = (canvasW - layoutW) / 2f; y = (canvasH - layoutH) / 2f }
        }
        return Pair(x.coerceAtLeast(0f), y.coerceAtLeast(0f))
    }

    private fun getLayoutAlignment(pos: String, isArabic: Boolean): Layout.Alignment {
        return when {
            pos.contains("Left") -> {
                if (isArabic) Layout.Alignment.ALIGN_OPPOSITE else Layout.Alignment.ALIGN_NORMAL
            }
            pos.contains("Right") -> {
                if (isArabic) Layout.Alignment.ALIGN_NORMAL else Layout.Alignment.ALIGN_OPPOSITE
            }
            else -> Layout.Alignment.ALIGN_CENTER
        }
    }

    private fun drawBackground(canvas: Canvas, w: Int, h: Int, bgType: String, bgColor: String, bgImageUri: Uri?) {
        val paint = Paint().apply { style = Paint.Style.FILL }
        when (bgType) {
            "Solid Color" -> {
                paint.color = getHexColor(bgColor)
                canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
            }
            "Transparent" -> {
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            }
            "Existing Image" -> {
                val uri = bgImageUri
                if (uri == null) {
                    cachedBgUri = null
                    cachedBgBitmap = null
                }
                if (uri != null) {
                    try {
                        val sourceBitmap = loadUriBitmap(uri)
                        if (sourceBitmap != null) {
                            val scaledBg = scaleCenterCrop(sourceBitmap, w, h)
                            canvas.drawBitmap(scaledBg, 0f, 0f, paint)
                        } else {
                            paint.color = Color.DKGRAY
                            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to load existing background image from Uri: $uri", e)
                        paint.color = Color.DKGRAY
                        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
                    }
                } else {
                    paint.color = Color.DKGRAY
                    canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
                }
            }
        }
    }

    private fun loadUriBitmap(uri: Uri): Bitmap? {
        if (uri == cachedBgUri && cachedBgBitmap != null && !cachedBgBitmap!!.isRecycled) {
            return cachedBgBitmap
        }
        return try {
            val contentResolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }

            val maxBound = 2048
            var sampleSize = 1
            if (bounds.outWidth > maxBound || bounds.outHeight > maxBound) {
                val halfW = bounds.outWidth / 2
                val halfH = bounds.outHeight / 2
                while ((halfW / sampleSize) >= maxBound && (halfH / sampleSize) >= maxBound) {
                    sampleSize *= 2
                }
            }

            val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val bitmap = contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
            cachedBgUri = uri
            cachedBgBitmap = bitmap
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "OOM or Stream error loading background", e)
            null
        }
    }

    private fun scaleCenterCrop(source: Bitmap, destW: Int, destH: Int): Bitmap {
        val sW = source.width
        val sH = source.height
        val scaleX = destW.toFloat() / sW
        val scaleY = destH.toFloat() / sH
        val scale = maxOf(scaleX, scaleY)
        val scaledW = scale * sW
        val scaledH = scale * sH
        val left = (destW - scaledW) / 2f
        val top = (destH - scaledH) / 2f
        val targetRect = RectF(left, top, left + scaledW, top + scaledH)
        val destBitmap = Bitmap.createBitmap(destW, destH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(destBitmap)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(source, null, targetRect, paint)
        return destBitmap
    }

    fun parseDimensions(dim: String): Pair<Int, Int> {
        return when {
            dim.contains("1920x1080") -> Pair(1920, 1080)
            dim.contains("1280x720") -> Pair(1280, 720)
            dim.contains("1080x1080") -> Pair(1080, 1080)
            dim.contains("1080x1920") -> Pair(1080, 1920)
            dim.contains("1024x768") -> Pair(1024, 768)
            dim.contains("768x1024") -> Pair(768, 1024)
            dim.contains("2560x1080") -> Pair(2560, 1080)
            dim.contains("1500x500") -> Pair(1500, 500)
            else -> Pair(1080, 1080)
        }
    }

    fun getHexColor(colorName: String): Int {
        return when (colorName) {
            "White" -> Color.WHITE
            "Black" -> Color.BLACK
            "Red" -> 0xFFE53935.toInt()
            "Blue" -> 0xFF1E88E5.toInt()
            "Green" -> 0xFF4CAF50.toInt()
            "Yellow" -> 0xFFFFEB3B.toInt()
            "Orange" -> 0xFFFB8C00.toInt()
            "Pink" -> 0xFFE91E63.toInt()
            "Purple" -> 0xFF8E24AA.toInt()
            "Gray" -> 0xFF757575.toInt()
            "Cyan" -> 0xFF00ACC1.toInt()
            "Magenta" -> 0xFFD81B60.toInt()
            "Light Blue" -> 0xFF90CAF9.toInt()
            "Light Green" -> 0xFFA5D6A7.toInt()
            "Islamic Green" -> 0xFF009933.toInt()
            else -> Color.WHITE
        }
    }

    fun isColorLight(colorName: String): Boolean {
        return colorName in listOf("White", "Yellow", "Pink", "Cyan", "Light Blue", "Light Green", "Islamic Green")
    }

    companion object {
        fun getExportQualityInt(qualityStr: String): Int {
            return when {
                qualityStr.contains("High") -> 100
                qualityStr.contains("Medium") -> 80
                qualityStr.contains("Low") -> 60
                else -> 100
            }
        }
    }
}
