package com.ahmedsamy.imagetype

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ahmedsamy.imagetype.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class EditorViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "EditorViewModel"

    // Services from AppContainer
    private val container = (application as ImageTypeApp).container
    private val preferencesManager = container.preferencesManager
    private val imageGenerator = container.imageGenerator
    private val imageStorageManager = container.imageStorageManager
    private val templateSerializer = container.templateSerializer

    // --- Tab 1 Editor & Actions UI States ---
    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _fitText = MutableStateFlow(true)
    val fitText: StateFlow<Boolean> = _fitText.asStateFlow()

    private val _fontSize = MutableStateFlow(60f)
    val fontSize: StateFlow<Float> = _fontSize.asStateFlow()

    private val _textPosition = MutableStateFlow(TextPosition.Center)
    val textPosition: StateFlow<TextPosition> = _textPosition.asStateFlow()

    private val _fontFamily = MutableStateFlow("Amiri")
    val fontFamily: StateFlow<String> = _fontFamily.asStateFlow()

    private val _fontStyle = MutableStateFlow(FontStyle.Regular)
    val fontStyle: StateFlow<FontStyle> = _fontStyle.asStateFlow()

    private val _textColor = MutableStateFlow(TextColor.White)
    val textColor: StateFlow<TextColor> = _textColor.asStateFlow()

    private val _textShadow = MutableStateFlow(true)
    val textShadow: StateFlow<Boolean> = _textShadow.asStateFlow()

    private val _dimensions = MutableStateFlow(ImageDimensions.Square)
    val dimensions: StateFlow<ImageDimensions> = _dimensions.asStateFlow()

    private val _bgType = MutableStateFlow(BgType.SolidColor)
    val bgType: StateFlow<BgType> = _bgType.asStateFlow()

    private val _bgColor = MutableStateFlow(TextColor.Black)
    val bgColor: StateFlow<TextColor> = _bgColor.asStateFlow()

    private val _bgImageUri = MutableStateFlow<Uri?>(null)
    val bgImageUri: StateFlow<Uri?> = _bgImageUri.asStateFlow()

    private val _imageQuality = MutableStateFlow(ImageQuality.High)
    val imageQuality: StateFlow<ImageQuality> = _imageQuality.asStateFlow()

    // --- Tab 2 Live Preview Output States ---
    private val _generatedBitmap = MutableStateFlow<Bitmap?>(null)
    val generatedBitmap: StateFlow<Bitmap?> = _generatedBitmap.asStateFlow()

    private val _isRendering = MutableStateFlow(false)
    val isRendering: StateFlow<Boolean> = _isRendering.asStateFlow()

    // --- Tab 3 Settings & Metadata States ---
    private val _appTheme = MutableStateFlow(AppTheme.System)
    val appTheme: StateFlow<AppTheme> = _appTheme.asStateFlow()

    private val _appLanguage = MutableStateFlow("en")
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    private val _hapticsEnabled = MutableStateFlow(true)
    val hapticsEnabled: StateFlow<Boolean> = _hapticsEnabled.asStateFlow()

    private val _savedTemplates = MutableStateFlow<List<Template>>(emptyList())
    val savedTemplates: StateFlow<List<Template>> = _savedTemplates.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesManager.themeFlow.collect { _appTheme.value = AppTheme.fromValue(it) }
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

        viewModelScope.launch {
            combine(
                _inputText, _fitText, _fontSize, _textPosition, _fontFamily,
                _fontStyle, _textColor, _textShadow, _dimensions, _bgType,
                _bgColor, _bgImageUri
            ) { args -> args }
                .debounce(100)
                .collect {
                    generateImage()
                }
        }
    }

    // --- Change Functions representing State mutation ---
    fun setInputText(text: String) { _inputText.value = text }
    fun setFitText(fit: Boolean) { _fitText.value = fit }
    fun setFontSize(size: Float) { _fontSize.value = size }
    fun setTextPosition(position: TextPosition) { _textPosition.value = position }
    fun setFontFamily(family: String) { _fontFamily.value = family }
    fun setFontStyle(style: FontStyle) { _fontStyle.value = style }
    fun setTextColor(color: TextColor) { _textColor.value = color }
    fun setTextShadow(shadow: Boolean) { _textShadow.value = shadow }
    fun setDimensions(dim: ImageDimensions) { _dimensions.value = dim }
    fun setBgType(type: BgType) { _bgType.value = type }
    fun setBgColor(color: TextColor) { _bgColor.value = color }
    fun setBgImageUri(uri: Uri?) { _bgImageUri.value = uri }
    fun setImageQuality(quality: ImageQuality) { _imageQuality.value = quality }

    // --- Preferences modifications ---
    fun updateTheme(theme: AppTheme) {
        _appTheme.value = theme
        viewModelScope.launch { preferencesManager.saveTheme(theme.value) }
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
            textPosition = _textPosition.value.value,
            fontFamily = _fontFamily.value,
            fontStyle = _fontStyle.value.value,
            textColor = _textColor.value.value,
            textShadow = _textShadow.value,
            dimensions = _dimensions.value.value,
            bgType = _bgType.value.value,
            bgColor = _bgColor.value.value,
            imageQuality = _imageQuality.value.value
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
        _textPosition.value = TextPosition.fromValue(template.textPosition)
        _fontFamily.value = template.fontFamily
        _fontStyle.value = FontStyle.fromValue(template.fontStyle)
        _textColor.value = TextColor.fromValue(template.textColor)
        _textShadow.value = template.textShadow
        _dimensions.value = ImageDimensions.fromValue(template.dimensions)
        _bgColor.value = TextColor.fromValue(template.bgColor)
        _imageQuality.value = ImageQuality.fromValue(template.imageQuality)
        if (BgType.fromValue(template.bgType) == BgType.ExistingImage) {
            _bgType.value = BgType.SolidColor
            _bgImageUri.value = null
        } else {
            _bgType.value = BgType.fromValue(template.bgType)
            _bgImageUri.value = null
        }
        generateImage()
    }

    fun deleteTemplate(template: Template) {
        val currentList = _savedTemplates.value.toMutableList()
        currentList.removeAll { it.id == template.id }
        _savedTemplates.value = currentList
        viewModelScope.launch {
            val json = templateSerializer.toJson(currentList)
            preferencesManager.saveTemplates(json)
        }
    }

    // --- Image Generation (delegates to ImageGenerator) ---
    fun generateImage() {
        viewModelScope.launch(Dispatchers.Default) {
            _isRendering.value = true
            try {
                val bitmap = imageGenerator.generate(
                    inputText = _inputText.value,
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
                    bgImageUri = _bgImageUri.value
                )
                _generatedBitmap.value = bitmap
            } catch (e: Exception) {
                Log.e(TAG, "Exception during image generation", e)
            } finally {
                _isRendering.value = false
            }
        }
    }

    // --- Save to Gallery (delegates to ImageStorageManager) ---
    fun saveImageToGallery(onFinished: (Boolean) -> Unit) {
        val bitmap = _generatedBitmap.value
        if (bitmap == null) {
            onFinished(false)
            return
        }
        imageStorageManager.saveToGallery(bitmap, _imageQuality.value.qualityInt, _bgType.value, viewModelScope, onFinished)
    }

    // --- Share Image (delegates to ImageStorageManager) ---
    fun shareGeneratedImage(onFinished: (Boolean) -> Unit) {
        val bitmap = _generatedBitmap.value
        if (bitmap == null) {
            onFinished(false)
            return
        }
        imageStorageManager.shareImage(bitmap, _imageQuality.value.qualityInt, _bgType.value, viewModelScope, onFinished)
    }
}
