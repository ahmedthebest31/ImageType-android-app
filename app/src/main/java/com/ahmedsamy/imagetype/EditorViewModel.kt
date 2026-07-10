package com.ahmedsamy.imagetype

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ahmedsamy.imagetype.util.Template
import com.ahmedsamy.imagetype.util.TemplateSerializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class EditorViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "EditorViewModel"
    private val context = application.applicationContext
    private val preferencesManager = PreferencesManager(context)

    // Template Serialization
    private val templateSerializer = TemplateSerializer()

    // --- Tab 1 Editor & Actions UI States ---
    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _fitText = MutableStateFlow(true)
    val fitText: StateFlow<Boolean> = _fitText.asStateFlow()

    private val _fontSize = MutableStateFlow(60f) // range 10f to 200f
    val fontSize: StateFlow<Float> = _fontSize.asStateFlow()

    private val _textPosition = MutableStateFlow("Center")
    val textPosition: StateFlow<String> = _textPosition.asStateFlow()

    private val _fontFamily = MutableStateFlow("Amiri")
    val fontFamily: StateFlow<String> = _fontFamily.asStateFlow()

    private val _fontStyle = MutableStateFlow("Regular")
    val fontStyle: StateFlow<String> = _fontStyle.asStateFlow()

    private val _textColor = MutableStateFlow("White")
    val textColor: StateFlow<String> = _textColor.asStateFlow()

    private val _textShadow = MutableStateFlow(true)
    val textShadow: StateFlow<Boolean> = _textShadow.asStateFlow()

    private val _dimensions = MutableStateFlow("Square (1080x1080)")
    val dimensions: StateFlow<String> = _dimensions.asStateFlow()

    private val _bgType = MutableStateFlow("Solid Color")
    val bgType: StateFlow<String> = _bgType.asStateFlow()

    private val _bgColor = MutableStateFlow("Black")
    val bgColor: StateFlow<String> = _bgColor.asStateFlow()

    private val _bgImageUri = MutableStateFlow<Uri?>(null)
    val bgImageUri: StateFlow<Uri?> = _bgImageUri.asStateFlow()

    private val _imageQuality = MutableStateFlow("100% (High)")
    val imageQuality: StateFlow<String> = _imageQuality.asStateFlow()

    // --- Tab 2 Live Preview Output States ---
    private val _generatedBitmap = MutableStateFlow<Bitmap?>(null)
    val generatedBitmap: StateFlow<Bitmap?> = _generatedBitmap.asStateFlow()

    private val _isRendering = MutableStateFlow(false)
    val isRendering: StateFlow<Boolean> = _isRendering.asStateFlow()

    // --- Tab 3 Settings & Metadata States ---
    private val _appTheme = MutableStateFlow("System Default")
    val appTheme: StateFlow<String> = _appTheme.asStateFlow()

    private val _appLanguage = MutableStateFlow("en")
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    private val _hapticsEnabled = MutableStateFlow(true)
    val hapticsEnabled: StateFlow<Boolean> = _hapticsEnabled.asStateFlow()

    private val _savedTemplates = MutableStateFlow<List<Template>>(emptyList())
    val savedTemplates: StateFlow<List<Template>> = _savedTemplates.asStateFlow()

    init {
        // Load data from DataStore Preferences on Init
        viewModelScope.launch {
            preferencesManager.themeFlow.collect { _appTheme.value = it }
        }
        viewModelScope.launch {
            preferencesManager.languageFlow.collect { _appLanguage.value = it }
        }
        viewModelScope.launch {
            preferencesManager.hapticsFlow.collect { _hapticsEnabled.value = it }
        }
        viewModelScope.launch {
            preferencesManager.templatesFlow.collect { json ->
                try {
                    _savedTemplates.value = templateSerializer.fromJson(json) ?: emptyList()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed loading templates JSON block", e)
                }
            }
        }

        // Combine inputs to generate the preview dynamically and instantly
        viewModelScope.launch {
            combine(
                _inputText, _fitText, _fontSize, _textPosition, _fontFamily,
                _fontStyle, _textColor, _textShadow, _dimensions, _bgType,
                _bgColor, _bgImageUri
            ) { args -> args }
                .debounce(100) // prevent overwhelming the thread during sliders/typing
                .collect {
                    generateImage()
                }
        }
    }

    // --- Change Functions representing State mutation ---
    fun setInputText(text: String) { _inputText.value = text }
    fun setFitText(fit: Boolean) { _fitText.value = fit }
    fun setFontSize(size: Float) { _fontSize.value = size }
    fun setTextPosition(position: String) { _textPosition.value = position }
    fun setFontFamily(family: String) { _fontFamily.value = family }
    fun setFontStyle(style: String) { _fontStyle.value = style }
    fun setTextColor(color: String) { _textColor.value = color }
    fun setTextShadow(shadow: Boolean) { _textShadow.value = shadow }
    fun setDimensions(dim: String) { _dimensions.value = dim }
    fun setBgType(type: String) { _bgType.value = type }
    fun setBgColor(color: String) { _bgColor.value = color }
    fun setBgImageUri(uri: Uri?) { _bgImageUri.value = uri }
    fun setImageQuality(quality: String) { _imageQuality.value = quality }

    // --- Preferences modifications ---
    fun updateTheme(theme: String) {
        _appTheme.value = theme
        viewModelScope.launch { preferencesManager.saveTheme(theme) }
    }

    fun updateLanguage(lang: String) {
        _appLanguage.value = lang
        viewModelScope.launch { preferencesManager.saveLanguage(lang) }
    }

    fun updateHaptics(enabled: Boolean) {
        _hapticsEnabled.value = enabled
        viewModelScope.launch { preferencesManager.saveHaptics(enabled) }
    }

    // --- Templates custom operations ---
    fun saveAsTemplate(name: String) {
        if (name.isBlank()) return
        val currentList = _savedTemplates.value.toMutableList()
        val template = Template(
            id = System.currentTimeMillis().toString(),
            name = name,
            text = _inputText.value,
            fitText = _fitText.value,
            fontSize = _fontSize.value,
            textPosition = _textPosition.value,
            fontFamily = _fontFamily.value,
            fontStyle = _fontStyle.value,
            textColor = _textColor.value,
            textShadow = _textShadow.value,
            dimensions = _dimensions.value,
            bgType = _bgType.value,
            bgColor = _bgColor.value,
            imageQuality = _imageQuality.value
        )
        currentList.removeAll { it.name.trim().lowercase() == name.trim().lowercase() }
        currentList.add(template)
        _savedTemplates.value = currentList
        viewModelScope.launch {
            val json = templateSerializer.toJson(currentList)
            preferencesManager.saveTemplates(json)
        }
    }

    fun loadTemplate(template: Template) {
        _inputText.value = template.text
        _fitText.value = template.fitText
        _fontSize.value = template.fontSize
        _textPosition.value = template.textPosition
        _fontFamily.value = template.fontFamily
        _fontStyle.value = template.fontStyle
        _textColor.value = template.textColor
        _textShadow.value = template.textShadow
        _dimensions.value = template.dimensions
        _bgType.value = template.bgType
        _bgColor.value = template.bgColor
        _imageQuality.value = template.imageQuality
        // Uri load isn't standard in persistence but we fall back nicely
        generateImage()
    }

    // --- Native Canvas Image Generation Engine ---
    fun generateImage() {
        viewModelScope.launch(Dispatchers.Default) {
            _isRendering.value = true
            try {
                val size = parseDimensions(_dimensions.value)
                val width = size.first
                val height = size.second

                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)

                // 1. Draw Background
                drawBackground(canvas, width, height)

                // 2. Setup Text Paint
                val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
                
                // Determine text characteristics
                val isArabic = FontManager.containsArabic(_inputText.value)
                val paintStyle = when (_fontStyle.value) {
                    "Bold" -> Typeface.BOLD
                    "Italic" -> Typeface.ITALIC
                    "Bold-Italic" -> Typeface.BOLD_ITALIC
                    else -> Typeface.NORMAL
                }
                
                val typeface = FontManager.getTypeface(context, _fontFamily.value, paintStyle, isArabic)
                textPaint.typeface = typeface

                // Margins/Padding
                val paddingX = width * 0.08f
                val paddingY = height * 0.08f
                val maxTextWidth = (width - (paddingX * 2)).coerceAtLeast(100f)
                val maxTextHeight = (height - (paddingY * 2)).coerceAtLeast(100f)

                // Font Size selection
                var finalFontSize = _fontSize.value
                if (_fitText.value) {
                    finalFontSize = binarySearchBestFontSize(_inputText.value, maxTextWidth, maxTextHeight, textPaint)
                }

                textPaint.textSize = finalFontSize

                // Prepare layout
                val textAlignment = getLayoutAlignment(_textPosition.value, isArabic)
                val staticLayout = StaticLayout.Builder.obtain(
                    _inputText.value,
                    0,
                    _inputText.value.length,
                    textPaint,
                    maxTextWidth.toInt()
                )
                    .setAlignment(textAlignment)
                    .setLineSpacing(0f, 1.1f)
                    .setIncludePad(false)
                    .build()

                // Calculate Position XY offsets
                val layoutWidth = staticLayout.width
                val layoutHeight = staticLayout.height
                val translationXY = calculateTranslation(
                    _textPosition.value,
                    width.toFloat(),
                    height.toFloat(),
                    layoutWidth.toFloat(),
                    layoutHeight.toFloat(),
                    paddingX,
                    paddingY
                )

                val tx = translationXY.first
                val ty = translationXY.second

                // Rendering Stroke Outline (For extreme WCAG High Contrast legibility)
                if (_textShadow.value) {
                    textPaint.style = Paint.Style.STROKE
                    textPaint.strokeWidth = finalFontSize * 0.08f
                    textPaint.strokeJoin = Paint.Join.ROUND
                    textPaint.color = if (isColorLight(_textColor.value)) Color.BLACK else Color.WHITE
                    
                    canvas.save()
                    canvas.translate(tx, ty)
                    staticLayout.draw(canvas)
                    canvas.restore()
                }

                // Render filled text
                textPaint.style = Paint.Style.FILL
                textPaint.strokeWidth = 0f
                textPaint.color = getHexColor(_textColor.value)
                if (_textShadow.value) {
                    val adaptiveShadowColor = if (isColorLight(_textColor.value)) 0x80000000.toInt() else 0x80FFFFFF.toInt()
                    textPaint.setShadowLayer(8f, 3f, 3f, adaptiveShadowColor)
                } else {
                    textPaint.clearShadowLayer()
                }

                canvas.save()
                canvas.translate(tx, ty)
                staticLayout.draw(canvas)
                canvas.restore()

                _generatedBitmap.value = bitmap
            } catch (e: Exception) {
                Log.e(TAG, "Exception during native image generation rendering", e)
            } finally {
                _isRendering.value = false
            }
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

            val testLayout = StaticLayout.Builder.obtain(text, 0, text.length, paint, maxWidth.toInt())
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
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
            "Top Left" -> {
                x = padX
                y = padY
            }
            "Top Center" -> {
                x = (canvasW - layoutW) / 2f
                y = padY
            }
            "Top Right" -> {
                x = canvasW - layoutW - padX
                y = padY
            }
            "Middle Left" -> {
                x = padX
                y = (canvasH - layoutH) / 2f
            }
            "Center" -> {
                x = (canvasW - layoutW) / 2f
                y = (canvasH - layoutH) / 2f
            }
            "Middle Right" -> {
                x = canvasW - layoutW - padX
                y = (canvasH - layoutH) / 2f
            }
            "Bottom Left" -> {
                x = padX
                y = canvasH - layoutH - padY
            }
            "Bottom Center" -> {
                x = (canvasW - layoutW) / 2f
                y = canvasH - layoutH - padY
            }
            "Bottom Right" -> {
                x = canvasW - layoutW - padX
                y = canvasH - layoutH - padY
            }
            else -> {
                x = (canvasW - layoutW) / 2f
                y = (canvasH - layoutH) / 2f
            }
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

    private fun drawBackground(canvas: Canvas, w: Int, h: Int) {
        val paint = Paint().apply { style = Paint.Style.FILL }
        when (_bgType.value) {
            "Solid Color" -> {
                paint.color = getHexColor(_bgColor.value)
                canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
            }
            "Transparent" -> {
                // Clear canvas with full transparency
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            }
            "Existing Image" -> {
                val uri = _bgImageUri.value
                if (uri != null) {
                    try {
                        val sourceBitmap = loadUriBitmap(uri)
                        if (sourceBitmap != null) {
                            val scaledBg = scaleCenterCrop(sourceBitmap, w, h)
                            canvas.drawBitmap(scaledBg, 0f, 0f, paint)
                        } else {
                            // Fallback to solid color if loading fails
                            paint.color = Color.DKGRAY
                            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to load existing background image from Uri: $uri", e)
                        paint.color = Color.DKGRAY
                        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
                    }
                } else {
                    // Fallback grey if no image selected yet
                    paint.color = Color.DKGRAY
                    canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
                }
            }
        }
    }

    private fun loadUriBitmap(uri: Uri): Bitmap? {
        return try {
            val contentResolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }

            // Safe inSampleSize downscaling to prevent OOM
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
            contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
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

    private fun parseDimensions(dim: String): Pair<Int, Int> {
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

    private fun getHexColor(colorName: String): Int {
        return when (colorName) {
            "White" -> Color.WHITE
            "Black" -> Color.BLACK
            "Red" -> 0xFFE53935.toInt()
            "Blue" -> 0xFF1E88E5.toInt()
            "Green" -> 0xFF4CAF50.toInt()
            "Islamic Green" -> 0xFF009933.toInt()
            "Yellow" -> 0xFFFFEB3B.toInt()
            "Orange" -> 0xFFFB8C00.toInt()
            "Pink" -> 0xFFE91E63.toInt()
            "Purple" -> 0xFF8E24AA.toInt()
            "Gray" -> 0xFF757575.toInt()
            "Cyan" -> 0xFF00ACC1.toInt()
            "Magenta" -> 0xFFD81B60.toInt()
            "Light Blue" -> 0xFF90CAF9.toInt()
            "Light Green" -> 0xFFA5D6A7.toInt()
            else -> Color.WHITE
        }
    }

    private fun isColorLight(colorName: String): Boolean {
        return colorName in listOf("White", "Yellow", "Pink", "Cyan", "Light Blue", "Light Green", "Islamic Green")
    }

    private fun getExportQualityInt(qualityStr: String): Int {
        return when {
            qualityStr.contains("High") -> 100
            qualityStr.contains("Medium") -> 80
            qualityStr.contains("Low") -> 60
            else -> 100
        }
    }

    // --- Action Button 1: Save to Gallery ---
    fun saveImageToGallery(onFinished: (Boolean) -> Unit) {
        val bitmap = _generatedBitmap.value
        if (bitmap == null) {
            onFinished(false)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            var success = false
            try {
                val quality = getExportQualityInt(_imageQuality.value)
                val format = if (_bgType.value == "Transparent") Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                val extension = if (_bgType.value == "Transparent") "png" else "jpg"
                val mimeType = if (_bgType.value == "Transparent") "image/png" else "image/jpeg"
                val filename = "ImageType_${System.currentTimeMillis()}.$extension"

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val resolver = context.contentResolver
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ImageType")
                    }

                    val imageUri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    if (imageUri != null) {
                        resolver.openOutputStream(imageUri).use { outStream ->
                            if (outStream != null) {
                                success = bitmap.compress(format, quality, outStream)
                            }
                        }
                    }
                } else {
                    // Legacy Support
                    val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).resolve("ImageType")
                    if (!imagesDir.exists()) {
                        imagesDir.mkdirs()
                    }
                    val file = File(imagesDir, filename)
                    FileOutputStream(file).use { outStream ->
                        success = bitmap.compress(format, quality, outStream)
                    }
                    // Trigger scanner to make it visible
                    val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                    mediaScanIntent.data = Uri.fromFile(file)
                    context.sendBroadcast(mediaScanIntent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Save to Gallery Error", e)
            }

            withContext(Dispatchers.Main) {
                onFinished(success)
            }
        }
    }

    // --- Action Button 2: Share Image ---
    fun shareGeneratedImage(onFinished: (Boolean) -> Unit) {
        val bitmap = _generatedBitmap.value
        if (bitmap == null) {
            onFinished(false)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val quality = getExportQualityInt(_imageQuality.value)
                val format = if (_bgType.value == "Transparent") Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                val extension = if (_bgType.value == "Transparent") "png" else "jpg"

                val cachePath = File(context.cacheDir, "shared_images")
                cachePath.mkdirs()
                val file = File(cachePath, "ImageType_share.$extension")
                val stream = FileOutputStream(file)
                bitmap.compress(format, quality, stream)
                stream.close()

                val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                if (contentUri != null) {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        setDataAndType(contentUri, context.contentResolver.getType(contentUri))
                        putExtra(Intent.EXTRA_STREAM, contentUri)
                        type = "image/*"
                    }
                    val chooser = Intent.createChooser(shareIntent, "Share generated text image via:")
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(chooser)
                    withContext(Dispatchers.Main) { onFinished(true) }
                } else {
                    withContext(Dispatchers.Main) { onFinished(false) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Sharing image error details", e)
                withContext(Dispatchers.Main) { onFinished(false) }
            }
        }
    }
}
