package com.ahmedsamy.imagetype.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.RectF
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
        textPosition: TextPosition,
        fontFamily: String,
        fontStyle: FontStyle,
        textColor: TextColor,
        textShadow: Boolean,
        dimensions: ImageDimensions,
        bgType: BgType,
        bgColor: TextColor,
        bgImageUri: Uri?
    ): Bitmap? {
        return try {
            val width = dimensions.width
            val height = dimensions.height

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            drawBackground(canvas, width, height, bgType, bgColor, bgImageUri)

            val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
            val isArabic = FontManager.containsArabic(inputText)
            val typeface = FontManager.getTypeface(context, fontFamily, fontStyle.androidTypefaceStyle, isArabic)
            textPaint.typeface = typeface

            val paddingX = width * 0.08f
            val paddingY = height * 0.08f
            val maxTextWidth = (width - (paddingX * 2)).coerceAtLeast(100f)
            val maxTextHeight = (height - (paddingY * 2)).coerceAtLeast(100f)

            val textAlignment = getLayoutAlignment(textPosition, isArabic)

            var finalFontSize = fontSize
            if (fitText) {
                finalFontSize = binarySearchBestFontSize(inputText, maxTextWidth, maxTextHeight, textPaint, textAlignment)
            }
            textPaint.textSize = finalFontSize

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

            if (textShadow) {
                textPaint.style = Paint.Style.STROKE
                textPaint.strokeWidth = finalFontSize * 0.08f
                textPaint.strokeJoin = Paint.Join.ROUND
                textPaint.color = if (textColor.isLight) Color.BLACK else Color.WHITE

                canvas.save()
                canvas.translate(tx, ty)
                staticLayout.draw(canvas)
                canvas.restore()
            }

            textPaint.style = Paint.Style.FILL
            textPaint.strokeWidth = 0f
            textPaint.color = textColor.hexColor
            if (textShadow) {
                val shadowColor = if (textColor.isLight) 0x80000000.toInt() else 0x80FFFFFF.toInt()
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
        alignment: Layout.Alignment,
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
                .setAlignment(alignment)
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
        pos: TextPosition,
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
            TextPosition.TopLeft -> { x = padX; y = padY }
            TextPosition.TopCenter -> { x = (canvasW - layoutW) / 2f; y = padY }
            TextPosition.TopRight -> { x = canvasW - layoutW - padX; y = padY }
            TextPosition.MiddleLeft -> { x = padX; y = (canvasH - layoutH) / 2f }
            TextPosition.Center -> { x = (canvasW - layoutW) / 2f; y = (canvasH - layoutH) / 2f }
            TextPosition.MiddleRight -> { x = canvasW - layoutW - padX; y = (canvasH - layoutH) / 2f }
            TextPosition.BottomLeft -> { x = padX; y = canvasH - layoutH - padY }
            TextPosition.BottomCenter -> { x = (canvasW - layoutW) / 2f; y = canvasH - layoutH - padY }
            TextPosition.BottomRight -> { x = canvasW - layoutW - padX; y = canvasH - layoutH - padY }
        }
        return Pair(x.coerceAtLeast(0f), y.coerceAtLeast(0f))
    }

    private fun getLayoutAlignment(pos: TextPosition, isArabic: Boolean): Layout.Alignment {
        return when {
            pos.value.contains("Left") -> {
                if (isArabic) Layout.Alignment.ALIGN_OPPOSITE else Layout.Alignment.ALIGN_NORMAL
            }
            pos.value.contains("Right") -> {
                if (isArabic) Layout.Alignment.ALIGN_NORMAL else Layout.Alignment.ALIGN_OPPOSITE
            }
            else -> Layout.Alignment.ALIGN_CENTER
        }
    }

    private fun drawBackground(canvas: Canvas, w: Int, h: Int, bgType: BgType, bgColor: TextColor, bgImageUri: Uri?) {
        val paint = Paint().apply { style = Paint.Style.FILL }
        when (bgType) {
            BgType.SolidColor -> {
                paint.color = bgColor.hexColor
                canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
            }
            BgType.Transparent -> {
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            }
            BgType.ExistingImage -> {
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
}
