package com.ahmedsamy.imagetype

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ahmedsamy.imagetype.ui.theme.MyApplicationTheme
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: EditorViewModel = viewModel()
            
            // Handle shared plain text intent immediately upon launch
            LaunchedEffect(Unit) {
                val intent = this@MainActivity.intent
                if (intent != null && intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
                    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                    if (!sharedText.isNullOrBlank()) {
                        viewModel.setInputText(sharedText)
                    }
                }
            }

            val themeState by viewModel.appTheme.collectAsState()
            val forceDark = when (themeState) {
                "Dark Mode" -> true
                "Light Mode" -> false
                else -> isSystemInDarkTheme()
            }

            val appLanguage by viewModel.appLanguage.collectAsState()
            
            val locale = remember(appLanguage) { Locale(appLanguage) }
            val baseConfig = LocalConfiguration.current
            val configuration = remember(appLanguage) {
                Configuration(baseConfig).apply {
                    setLocale(locale)
                }
            }
            val baseContext = LocalContext.current
            val context = remember(appLanguage) {
                baseContext.createConfigurationContext(configuration)
            }

            CompositionLocalProvider(
                LocalConfiguration provides configuration,
                LocalContext provides context
            ) {
                MyApplicationTheme(darkTheme = forceDark) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        MainScreen(viewModel)
                    }
                }
            }
        }
    }
}

// Custom DrawBehind Modifier inside Compose to show Photoshop style transparent grid checking
@Stable
fun Modifier.checkerboardBackground(
    tileSize: Dp = 12.dp,
    colorLight: ComposeColor = ComposeColor(0xFFEAEAEA),
    colorDark: ComposeColor = ComposeColor(0xFFD4D4D4)
) = drawBehind {
    val sizePx = tileSize.toPx()
    val columns = (size.width / sizePx).toInt() + 1
    val rows = (size.height / sizePx).toInt() + 1
    for (i in 0 until columns) {
        for (j in 0 until rows) {
            val color = if ((i + j) % 2 == 0) colorLight else colorDark
            drawRect(
                color = color,
                topLeft = Offset(i * sizePx, j * sizePx),
                size = androidx.compose.ui.geometry.Size(sizePx, sizePx)
            )
        }
    }
}

// Dynamic dynamic string translator
fun getTranslation(key: String, lang: String): String {
    val translations = mapOf(
        "en" to mapOf(
            "app_title" to "ImageType",
            "tab_editor" to "Editor",
            "tab_preview" to "Preview",
            "tab_settings" to "Settings",
            "text_input_label" to "Enter custom text",
            "paste_button" to "Paste text",
            "fit_text_checkbox" to "Fit Text to Image",
            "font_size_label" to "Font Size: ",
            "text_position_label" to "Text Position",
            "font_family_label" to "Font Family",
            "font_style_label" to "Font Style",
            "text_color_label" to "Text Color",
            "enable_shadow_label" to "Enable High Contrast (Stroke & Shadow)",
            "dimensions_label" to "Image Dimensions",
            "bg_type_label" to "Background Type",
            "bg_color_label" to "Background Color",
            "existing_image_button" to "Pick Background Image",
            "image_quality_label" to "Image Quality Preset",
            "save_gallery_button" to "Save to Gallery",
            "share_image_button" to "Share Image",
            "about_downloads_button" to "ImageType Website / Downloads",
            "share_app_button" to "Share ImageType App",
            "rate_app_button" to "Rate App on Store",
            "haptic_toggle" to "Enable Haptic Feedback",
            "theme_label" to "App Theme",
            "language_label" to "App Language",
            "save_template_button" to "Save as Template",
            "template_name_placeholder" to "Template Name",
            "load_template_label" to "Load Template",
            "preview_description" to "Live preview of the generated image",
            "no_preview_available" to "No live preview available. Please enter text first.",
            "success_save" to "Image saved to gallery successfully!",
            "success_template" to "Template saved successfully!",
            "error_empty_text" to "Text input cannot be empty!"
        ),
        "ar" to mapOf(
            "app_title" to "ImageType",
            "tab_editor" to "المحرر",
            "tab_preview" to "المعاينة",
            "tab_settings" to "الإعدادات",
            "text_input_label" to "أدخل النص هنا",
            "paste_button" to "لصق النص",
            "fit_text_checkbox" to "ملاءمة النص للصورة تلقائيًا",
            "font_size_label" to "حجم الخط: ",
            "text_position_label" to "موقع النص",
            "font_family_label" to "عائلة الخط",
            "font_style_label" to "نمط الخط",
            "text_color_label" to "لون النص",
            "enable_shadow_label" to "تفعيل تباين عالي (إطار وظل)",
            "dimensions_label" to "أبعاد الصورة",
            "bg_type_label" to "نوع الخلفية",
            "bg_color_label" to "لون الخلفية",
            "existing_image_button" to "اختر صورة الخلفية",
            "image_quality_label" to "جودة الصورة",
            "save_gallery_button" to "حفظ في الاستوديو",
            "share_image_button" to "مشاركة الصورة",
            "about_downloads_button" to "موقع آيميج تايب / التنزيلات",
            "share_app_button" to "مشاركة التطبيق",
            "rate_app_button" to "تقييم التطبيق على المتجر",
            "haptic_toggle" to "تفعيل الاهتزاز اللمسي",
            "theme_label" to "طابع التطبيق",
            "language_label" to "لغة التطبيق",
            "save_template_button" to "حفظ كقالب جديد",
            "template_name_placeholder" to "اسم القالب",
            "load_template_label" to "تحميل قالب مخزن",
            "preview_description" to "معاينة حية للصورة التي تم إنشاؤها",
            "no_preview_available" to "لا توجد معاينة متاحة. يرجى إدخال نص أولاً.",
            "success_save" to "تم حفظ الصورة في الاستوديو بنجاح!",
            "success_template" to "تم حفظ القالب بنجاح!",
            "error_empty_text" to "لا يمكن أن يكون النص فارغًا!"
        ),
        "fr" to mapOf(
            "app_title" to "ImageType",
            "tab_editor" to "Éditeur",
            "tab_preview" to "Aperçu",
            "tab_settings" to "Paramètres",
            "text_input_label" to "Saisir le texte personnalisé",
            "paste_button" to "Coller le texte",
            "fit_text_checkbox" to "Ajuster le texte à l'image",
            "font_size_label" to "Taille de police: ",
            "text_position_label" to "Position du texte",
            "font_family_label" to "Famille de polices",
            "font_style_label" to "Style de police",
            "text_color_label" to "Couleur du texte",
            "enable_shadow_label" to "Activer le contraste élevé (contour & ombre)",
            "dimensions_label" to "Dimensions de l'image",
            "bg_type_label" to "Type d'arrière-plan",
            "bg_color_label" to "Couleur de fond",
            "existing_image_button" to "Choisir une image de fond",
            "image_quality_label" to "Qualité de l'image",
            "save_gallery_button" to "Enregistrer dans la galerie",
            "share_image_button" to "Partager l'image",
            "about_downloads_button" to "Site Web ImageType / Téléchargements",
            "share_app_button" to "Partager l'application",
            "rate_app_button" to "Évaluer l'application",
            "haptic_toggle" to "Activer les retours haptiques",
            "theme_label" to "Thème de l'application",
            "language_label" to "Langue de l'application",
            "save_template_button" to "Enregistrer le modèle",
            "template_name_placeholder" to "Nom du modèle",
            "load_template_label" to "Charger un modèle",
            "preview_description" to "Aperçu en direct de l'image générée",
            "no_preview_available" to "Aucun aperçu disponible. Veuillez d'abord saisir du texte.",
            "success_save" to "Image enregistrée avec succès dans la galerie!",
            "success_template" to "Modèle enregistré avec succès!",
            "error_empty_text" to "Le texte ne peut pas être vide!"
        ),
        "it" to mapOf(
            "app_title" to "ImageType",
            "tab_editor" to "Editor",
            "tab_preview" to "Anteprima",
            "tab_settings" to "Impostazioni",
            "text_input_label" to "Inserisci testo personalizzato",
            "paste_button" to "Incolla testo",
            "fit_text_checkbox" to "Adatta testo all'immagine",
            "font_size_label" to "Dimensione carattere: ",
            "text_position_label" to "Posizione del testo",
            "font_family_label" to "Famiglia di caratteri",
            "font_style_label" to "Stile carattere",
            "text_color_label" to "Colore del testo",
            "enable_shadow_label" to "Abilita contrasto elevato (tratto e ombra)",
            "dimensions_label" to "Dimensioni dell'immagine",
            "bg_type_label" to "Tipo di sfondo",
            "bg_color_label" to "Colore di sfondo",
            "existing_image_button" to "Scegli immagine di sfondo",
            "image_quality_label" to "Qualità dell'immagine",
            "save_gallery_button" to "Salva in Galleria",
            "share_image_button" to "Condividi immagine",
            "about_downloads_button" to "Sito Web ImageType / Download",
            "share_app_button" to "Condividi applicazione",
            "rate_app_button" to "Valuta l'applicazione",
            "haptic_toggle" to "Attiva feedback tattile",
            "theme_label" to "Tema dell'applicazione",
            "language_label" to "Lingua dell'applicazione",
            "save_template_button" to "Salva come modello",
            "template_name_placeholder" to "Nome del modello",
            "load_template_label" to "Carica modello",
            "preview_description" to "Anteprima in tempo reale",
            "no_preview_available" to "Nessuna anteprima disponibile. Inserisci prima il testo.",
            "success_save" to "Immagine salvata in galleria con successo!",
            "success_template" to "Modello salvato con successo!",
            "error_empty_text" to "Il testo non può essere vuoto!"
        ),
        "tr" to mapOf(
            "app_title" to "ImageType",
            "tab_editor" to "Editör",
            "tab_preview" to "Önizleme",
            "tab_settings" to "Ayarlar",
            "text_input_label" to "Özel metin girin",
            "paste_button" to "Metni Yapıştır",
            "fit_text_checkbox" to "Metni Resme Sığdır",
            "font_size_label" to "Yazı Tipi Boyutu: ",
            "text_position_label" to "Metin Konumu",
            "font_family_label" to "Yazı Tipi Ailesi",
            "font_style_label" to "Yazı Tipi Stili",
            "text_color_label" to "Metin Rengi",
            "enable_shadow_label" to "Yüksek Kontrastı Etkinleştir (Kontur ve Gölge)",
            "dimensions_label" to "Görüntü Boyutları",
            "bg_type_label" to "Arka Plan Türü",
            "bg_color_label" to "Arka Plan Rengi",
            "existing_image_button" to "Arka Plan Resmi Seç",
            "image_quality_label" to "Görüntü Kalitesi",
            "save_gallery_button" to "Galeriye Kaydet",
            "share_image_button" to "Resmi Paylaş",
            "about_downloads_button" to "ImageType Web Sitesi / İndirmeler",
            "share_app_button" to "Uygulamayı Paylaş",
            "rate_app_button" to "Uygulamayı Değerlendir",
            "haptic_toggle" to "Titreşim Geri Bildirimi Etkinleştir",
            "theme_label" to "Uygulama Teması",
            "language_label" to "Uygulama Dili",
            "save_template_button" to "Şablon Olarak Kaydet",
            "template_name_placeholder" to "Şablon Adı",
            "load_template_label" to "Şablon Yükle",
            "preview_description" to "Oluşturulan görüntünün canlı önizlemesi",
            "no_preview_available" to "Önizleme mevcut değil. Lütfen önce metin girin.",
            "success_save" to "Görsel başarıyla galeriye kaydedildi!",
            "success_template" to "Şablon başarıyla kaydedildi!",
            "error_empty_text" to "Metin girişi boş olamaz!"
        ),
        "de" to mapOf(
            "app_title" to "ImageType",
            "tab_editor" to "Editor",
            "tab_preview" to "Vorschau",
            "tab_settings" to "Einstellungen",
            "text_input_label" to "Benutzerdefinierten Text eingeben",
            "paste_button" to "Text einfügen",
            "fit_text_checkbox" to "Text an Bild anpassen",
            "font_size_label" to "Schriftgröße: ",
            "text_position_label" to "Textposition",
            "font_family_label" to "Schriftfamilie",
            "font_style_label" to "Schriftstil",
            "text_color_label" to "Textfarbe",
            "enable_shadow_label" to "Hohen Kontrast aktivieren (Kontur & Schatten)",
            "dimensions_label" to "Bildabmessungen",
            "bg_type_label" to "Hintergrundtyp",
            "bg_color_label" to "Hintergrundfarbe",
            "existing_image_button" to "Hintergrundbild auswählen",
            "image_quality_label" to "Bildqualität-Voreinstellung",
            "save_gallery_button" to "In Galerie speichern",
            "share_image_button" to "Bild teilen",
            "about_downloads_button" to "ImageType Website / Downloads",
            "share_app_button" to "App teilen",
            "rate_app_button" to "App bewerten",
            "haptic_toggle" to "Haptisches Feedback aktivieren",
            "theme_label" to "App-Thema",
            "language_label" to "App-Sprache",
            "save_template_button" to "Als Vorlage speichern",
            "template_name_placeholder" to "Vorlagenname",
            "load_template_label" to "Vorlage laden",
            "preview_description" to "Live-Vorschau des generierten Bildes",
            "no_preview_available" to "Keine Vorschau verfügbar. Bitte zuerst Text eingeben.",
            "success_save" to "Bild erfolgreich in der Galerie gespeichert!",
            "success_template" to "Vorlage erfolgreich gespeichert!",
            "error_empty_text" to "Texteingabe darf nicht leer sein!"
        ),
        "es" to mapOf(
            "app_title" to "ImageType",
            "tab_editor" to "Editor",
            "tab_preview" to "Vista previa",
            "tab_settings" to "Configuración",
            "text_input_label" to "Introducir texto personalizado",
            "paste_button" to "Pegar texto",
            "fit_text_checkbox" to "Ajustar texto al tamaño de imagen",
            "font_size_label" to "Tamaño de letra: ",
            "text_position_label" to "Posición del texto",
            "font_family_label" to "Familia tipográfica",
            "font_style_label" to "Estilo de fuente",
            "text_color_label" to "Color de texto",
            "enable_shadow_label" to "Contraste alto (contorno y sombra)",
            "dimensions_label" to "Dimensiones de la imagen",
            "bg_type_label" to "Tipo de fondo",
            "bg_color_label" to "Color de fondo",
            "existing_image_button" to "Elegir imagen de fondo",
            "image_quality_label" to "Calidad de imagen",
            "save_gallery_button" to "Guardar en Galería",
            "share_image_button" to "Compartir imagen",
            "about_downloads_button" to "Sitio web de ImageType / Descargas",
            "share_app_button" to "Compartir la aplicación",
            "rate_app_button" to "Calificar en Play Store",
            "haptic_toggle" to "Activar vibración táctil",
            "theme_label" to "Tema de la aplicación",
            "language_label" to "Idioma de la aplicación",
            "save_template_button" to "Guardar como plantilla",
            "template_name_placeholder" to "Nombre de plantilla",
            "load_template_label" to "Cargar plantilla",
            "preview_description" to "Vista previa instantánea de la imagen",
            "no_preview_available" to "Vista previa no disponible. Introduce texto.",
            "success_save" to "¡Imagen guardada en la galería con éxito!",
            "success_template" to "¡Plantilla guardada con éxito!",
            "error_empty_text" to "¡El texto no puede estar vacío!"
        )
    )
    val langMap = translations[lang] ?: translations["en"]!!
    return langMap[key] ?: (translations["en"]!![key] ?: key)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: EditorViewModel) {
    val currentLang by viewModel.appLanguage.collectAsState()
    val isRtl = currentLang == "ar"

    // Multi-Language Layout Container
    CompositionLocalProvider(
        androidx.compose.ui.platform.LocalLayoutDirection provides if (isRtl) {
            androidx.compose.ui.unit.LayoutDirection.Rtl
        } else {
            androidx.compose.ui.unit.LayoutDirection.Ltr
        }
    ) {
        var selectedTabIndex by rememberSaveable { mutableStateOf(0) }
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "ImageType",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    scrollBehavior = scrollBehavior,
                    modifier = Modifier.shadow(4.dp)
                )
            },
            bottomBar = {
                NavigationBar(
                    windowInsets = WindowInsets.navigationBars,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    val haptics = LocalHapticFeedback.current
                    val isHapticsEnabled by viewModel.hapticsEnabled.collectAsState()

                    NavigationBarItem(
                        selected = selectedTabIndex == 0,
                        onClick = {
                            selectedTabIndex = 0
                            if (isHapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        icon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        label = { Text(getTranslation("tab_editor", currentLang)) },
                        modifier = Modifier.testTag("nav_tab_editor")
                    )
                    NavigationBarItem(
                        selected = selectedTabIndex == 1,
                        onClick = {
                            selectedTabIndex = 1
                            if (isHapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        icon = { Icon(Icons.Default.Image, contentDescription = null) },
                        label = { Text(getTranslation("tab_preview", currentLang)) },
                        modifier = Modifier.testTag("nav_tab_preview")
                    )
                    NavigationBarItem(
                        selected = selectedTabIndex == 2,
                        onClick = {
                            selectedTabIndex = 2
                            if (isHapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text(getTranslation("tab_settings", currentLang)) },
                        modifier = Modifier.testTag("nav_tab_settings")
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (selectedTabIndex) {
                    0 -> TabEditorWorkspace(viewModel, currentLang)
                    1 -> TabLivePreviewWorkspace(viewModel, currentLang)
                    2 -> TabSettingsWorkspace(viewModel, currentLang)
                }
            }
        }
    }
}

// ==================== TAB 1: Main Workspace ====================
@Composable
fun TabEditorWorkspace(viewModel: EditorViewModel, currentLang: String) {
    val scrollState = rememberScrollState()
    val hapticFeedback = LocalHapticFeedback.current
    val hapticsEnabled by viewModel.hapticsEnabled.collectAsState()

    // Retrieve state streams
    val inputText by viewModel.inputText.collectAsState()
    val fitText by viewModel.fitText.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()
    val textPosition by viewModel.textPosition.collectAsState()
    val fontFamily by viewModel.fontFamily.collectAsState()
    val fontStyle by viewModel.fontStyle.collectAsState()
    val textColor by viewModel.textColor.collectAsState()
    val textShadow by viewModel.textShadow.collectAsState()
    val dimensions by viewModel.dimensions.collectAsState()
    val bgType by viewModel.bgType.collectAsState()
    val bgColor by viewModel.bgColor.collectAsState()
    val bgImageUri by viewModel.bgImageUri.collectAsState()
    val imageQuality by viewModel.imageQuality.collectAsState()

    val context = LocalContext.current

    // Background existing image photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.setBgImageUri(uri)
                if (hapticsEnabled) hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // Card 1: Text Input Grouping
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = getTranslation("text_input_label", currentLang),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { viewModel.setInputText(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_text_field"),
                    minLines = 3,
                    maxLines = 6,
                    placeholder = { Text(getTranslation("text_input_label", currentLang)) }
                )

                // Paste Clipboard Button next/below
                Button(
                    onClick = {
                        if (hapticsEnabled) hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        if (clipboard.hasPrimaryClip() && clipboard.primaryClipDescription?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) == true) {
                            val item = clipboard.primaryClip?.getItemAt(0)
                            val pasteText = item?.text?.toString() ?: ""
                            if (pasteText.isNotBlank()) {
                                viewModel.setInputText(inputText + pasteText)
                                Toast.makeText(context, "Pasted text content", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "No plain text found on clipboard", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.align(Alignment.End),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Row(
                        modifier = Modifier.semantics(mergeDescendants = true) {},
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ContentPaste,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(getTranslation("paste_button", currentLang))
                    }
                }
            }
        }

        // Card 2: Typography & Sizing Grouping
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = getTranslation("font_family_label", currentLang),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Fit Text to Image (Checkbox)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            val nextVal = !fitText
                            viewModel.setFitText(nextVal)
                            if (hapticsEnabled) hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = fitText,
                        onCheckedChange = {
                            viewModel.setFitText(it)
                            if (hapticsEnabled) hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        modifier = Modifier.testTag("checkbox_fit_text")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = getTranslation("fit_text_checkbox", currentLang),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                // Conditional UI controls: Hide FontSize and TextPosition if Fit Text is checked!!
                AnimatedVisibility(
                    visible = !fitText,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Font Size Slider
                        Column {
                            Text(
                                text = getTranslation("font_size_label", currentLang) + " ${fontSize.toInt()}sp",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Slider(
                                value = fontSize,
                                onValueChange = { size ->
                                    viewModel.setFontSize(size)
                                },
                                valueRange = 10f..200f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("slider_font_size")
                            )
                        }

                        // Text Position Dropdown code
                        val positionOptions = listOf(
                            "Top Left", "Top Center", "Top Right",
                            "Middle Left", "Center", "Middle Right",
                            "Bottom Left", "Bottom Center", "Bottom Right"
                        )
                        ImageTypeDropdown(
                            label = getTranslation("text_position_label", currentLang),
                            options = positionOptions,
                            selectedOption = textPosition,
                            onOptionSelected = { viewModel.setTextPosition(it) },
                            hapticFeedback = hapticFeedback,
                            hapticsEnabled = hapticsEnabled
                        )
                    }
                }

                // Font Family Dropdown
                val fontOptionLabels = FontManager.fontFamilies
                ImageTypeDropdown(
                    label = getTranslation("font_family_label", currentLang),
                    options = fontOptionLabels,
                    selectedOption = fontFamily,
                    onOptionSelected = { viewModel.setFontFamily(it) },
                    hapticFeedback = hapticFeedback,
                    hapticsEnabled = hapticsEnabled
                )

                // Font Style Dropdown
                val fontStyles = listOf("Regular", "Bold", "Italic", "Bold-Italic")
                ImageTypeDropdown(
                    label = getTranslation("font_style_label", currentLang),
                    options = fontStyles,
                    selectedOption = fontStyle,
                    onOptionSelected = { viewModel.setFontStyle(it) },
                    hapticFeedback = hapticFeedback,
                    hapticsEnabled = hapticsEnabled
                )

                // Text Color Dropdown
                val colorsList = listOf(
                    "White", "Black", "Red", "Blue", "Green", "Yellow", "Orange",
                    "Pink", "Purple", "Gray", "Cyan", "Magenta", "Light Blue", "Light Green"
                )
                ImageTypeDropdown(
                    label = getTranslation("text_color_label", currentLang),
                    options = colorsList,
                    selectedOption = textColor,
                    onOptionSelected = { viewModel.setTextColor(it) },
                    hapticFeedback = hapticFeedback,
                    hapticsEnabled = hapticsEnabled
                )

                // Enable Text Shadow Checkbox (WCAG stroke shadow toggle)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            val prev = textShadow
                            viewModel.setTextShadow(!prev)
                            if (hapticsEnabled) hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = textShadow,
                        onCheckedChange = {
                            viewModel.setTextShadow(it)
                            if (hapticsEnabled) hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        modifier = Modifier.testTag("checkbox_text_shadow")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = getTranslation("enable_shadow_label", currentLang),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        // Card 3: Backdrops & Geometry Layout Rules
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = getTranslation("bg_type_label", currentLang),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Image Dimension Presets Dropdown Menu
                val dimensionsList = listOf(
                    "Standard (1920x1080)", "Standard HD (1280x720)", "Square (1080x1080)",
                    "Portrait/Reels (1080x1920)", "4:3 (1024x768)", "3:4 (768x1024)",
                    "Ultrawide (2560x1080)", "Twitter Header (1500x500)"
                )
                ImageTypeDropdown(
                    label = getTranslation("dimensions_label", currentLang),
                    options = dimensionsList,
                    selectedOption = dimensions,
                    onOptionSelected = { viewModel.setDimensions(it) },
                    hapticFeedback = hapticFeedback,
                    hapticsEnabled = hapticsEnabled
                )

                // Background Type Menu Selector
                val bgTypes = listOf("Solid Color", "Transparent", "Existing Image")
                ImageTypeDropdown(
                    label = getTranslation("bg_type_label", currentLang),
                    options = bgTypes,
                    selectedOption = bgType,
                    onOptionSelected = { viewModel.setBgType(it) },
                    hapticFeedback = hapticFeedback,
                    hapticsEnabled = hapticsEnabled
                )

                // Background Color (Visible ONLY if Bg Type is Solid Color)
                AnimatedVisibility(
                    visible = bgType == "Solid Color",
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    val colorsList = listOf(
                        "White", "Black", "Red", "Blue", "Green", "Yellow", "Orange",
                        "Pink", "Purple", "Gray", "Cyan", "Magenta", "Light Blue", "Light Green"
                    )
                    ImageTypeDropdown(
                        label = getTranslation("bg_color_label", currentLang),
                        options = colorsList,
                        selectedOption = bgColor,
                        onOptionSelected = { viewModel.setBgColor(it) },
                        hapticFeedback = hapticFeedback,
                        hapticsEnabled = hapticsEnabled
                    )
                }

                // Existing Image Picker (Visible ONLY if Bg Type is Existing Image)
                AnimatedVisibility(
                    visible = bgType == "Existing Image",
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (hapticsEnabled) hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(getTranslation("existing_image_button", currentLang))
                        }

                        if (bgImageUri != null) {
                            Text(
                                text = "Selected Background: ${bgImageUri?.lastPathSegment}",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Image Quality Export Presets Menu Selection
                val qualities = listOf("100% (High)", "80% (Medium)", "60% (Low)")
                ImageTypeDropdown(
                    label = getTranslation("image_quality_label", currentLang),
                    options = qualities,
                    selectedOption = imageQuality,
                    onOptionSelected = { viewModel.setImageQuality(it) },
                    hapticFeedback = hapticFeedback,
                    hapticsEnabled = hapticsEnabled
                )
            }
        }

        // Action Buttons: Save to Gallery & Share
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Save to Gallery
            Button(
                onClick = {
                    if (hapticsEnabled) hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (inputText.isBlank()) {
                        Toast.makeText(context, getTranslation("error_empty_text", currentLang), Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.saveImageToGallery { success ->
                            if (success) {
                                Toast.makeText(context, getTranslation("success_save", currentLang), Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Failure: Unable to save to photo gallery", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("action_save_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(getTranslation("save_gallery_button", currentLang), style = MaterialTheme.typography.titleMedium)
            }

            // Share Image
            Button(
                onClick = {
                    if (hapticsEnabled) hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (inputText.isBlank()) {
                        Toast.makeText(context, getTranslation("error_empty_text", currentLang), Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.shareGeneratedImage { success ->
                            if (!success) {
                                Toast.makeText(context, "Failure: Could not export and share image content", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("action_share_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Row(
                    modifier = Modifier.semantics(mergeDescendants = true) {},
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(getTranslation("share_image_button", currentLang), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

// ==================== TAB 2: Live Preview Workspace ====================
@Composable
fun TabLivePreviewWorkspace(viewModel: EditorViewModel, currentLang: String) {
    val bitmapState by viewModel.generatedBitmap.collectAsState()
    val isRendering by viewModel.isRendering.collectAsState()
    val bgType by viewModel.bgType.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        val bitmap = bitmapState
        if (bitmap != null) {
            var scale by remember { mutableStateOf(1f) }
            var offset by remember { mutableStateOf(Offset.Zero) }
            val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
                scale = (scale * zoomChange).coerceIn(1f, 6f)
                offset += offsetChange
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RectangleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RectangleShape)
                    .then(
                        if (bgType == "Transparent") Modifier.checkerboardBackground() else Modifier.background(
                            ComposeColor.DarkGray
                        )
                    )
                    .transformable(state = transformState)
                    .pointerInput(Unit) {
                        detectTapGestures(onDoubleTap = {
                            scale = if (scale > 1f) 1f else 2.5f
                            offset = Offset.Zero
                        })
                    }
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = getTranslation("preview_description", currentLang),
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        ),
                    contentScale = ContentScale.Fit
                )
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    Icons.Default.Panorama,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = getTranslation("no_preview_available", currentLang),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Processing overlay rendering
        if (isRendering) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ComposeColor.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(strokeWidth = 3.dp)
                        Text(
                            "Rendering graphics...",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ==================== TAB 3: Settings & Metadata ====================
@Composable
fun TabSettingsWorkspace(viewModel: EditorViewModel, currentLang: String) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val scrollState = rememberScrollState()

    // Retrieve state streams
    val appTheme by viewModel.appTheme.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val hapticsEnabled by viewModel.hapticsEnabled.collectAsState()
    val savedTemplates by viewModel.savedTemplates.collectAsState()

    var templateInputName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // Card 1: Display & App Locale Variables
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Theme Mode Selector
                val themes = listOf("Dark Mode", "Light Mode", "System Default")
                ImageTypeDropdown(
                    label = getTranslation("theme_label", currentLang),
                    options = themes,
                    selectedOption = appTheme,
                    onOptionSelected = { viewModel.updateTheme(it) },
                    hapticFeedback = hapticFeedback,
                    hapticsEnabled = hapticsEnabled
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Language Mode Selector
                val languages = listOf("en", "ar", "fr", "it", "tr", "de", "es")
                val languageDisplayNames = mapOf(
                    "en" to "English", "ar" to "العربية", "fr" to "Français",
                    "it" to "Italiano", "tr" to "Türkçe", "de" to "Deutsch", "es" to "Español"
                )

                ImageTypeDropdown(
                    label = getTranslation("language_label", currentLang),
                    options = languages,
                    selectedOption = appLanguage,
                    getLabel = { languageDisplayNames[it] ?: "English" },
                    onOptionSelected = { viewModel.updateLanguage(it) },
                    hapticFeedback = hapticFeedback,
                    hapticsEnabled = hapticsEnabled
                )

                // Haptic Feedback switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            val prev = hapticsEnabled
                            viewModel.updateHaptics(!prev)
                            if (!prev) hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        .padding(8.dp)
                        .semantics(mergeDescendants = true) {},
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = hapticsEnabled,
                        onCheckedChange = null, // let parent Row's clickable toggle handle and narrate it
                        modifier = Modifier.testTag("checkbox_haptic_feedback")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = getTranslation("haptic_toggle", currentLang),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        // Card 2: Saved Templates Manager
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = templateInputName,
                    onValueChange = { templateInputName = it },
                    label = { Text(getTranslation("template_name_placeholder", currentLang)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("template_name_input"),
                    singleLine = true
                )

                Button(
                    onClick = {
                        if (hapticsEnabled) hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (templateInputName.isNotBlank()) {
                            viewModel.saveAsTemplate(templateInputName)
                            Toast.makeText(context, getTranslation("success_template", currentLang), Toast.LENGTH_SHORT).show()
                            templateInputName = ""
                        } else {
                            Toast.makeText(context, "Please enter a valid template name", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(getTranslation("save_template_button", currentLang))
                }

                if (savedTemplates.isNotEmpty()) {
                    // Templates dropdown select
                    ImageTypeDropdown(
                        label = getTranslation("load_template_label", currentLang),
                        options = savedTemplates,
                        selectedOption = savedTemplates.first(),
                        getLabel = { it.name },
                        onOptionSelected = {
                            viewModel.loadTemplate(it)
                            Toast.makeText(context, "Loaded template: ${it.name}", Toast.LENGTH_SHORT).show()
                        },
                        hapticFeedback = hapticFeedback,
                        hapticsEnabled = hapticsEnabled
                    )
                }
            }
        }

        // Card 3: External metadata directories, store, and ratings
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Feature 1: About Website Link
                Button(
                    onClick = {
                        if (hapticsEnabled) hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://ahmedthebest31.github.io/ImageType/#downloads"))
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                ) {
                    Icon(Icons.Default.Language, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(getTranslation("about_downloads_button", currentLang))
                }

                // Feature 2: Share App Store Link
                Button(
                    onClick = {
                        if (hapticsEnabled) hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        val shareText = "https://play.google.com/store/apps/details?id=com.ahmedsamy.imagetype"
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        val chooser = Intent.createChooser(intent, "Share ImageType App with:")
                        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(chooser)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                ) {
                    Row(
                        modifier = Modifier.semantics(mergeDescendants = true) {},
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(getTranslation("share_app_button", currentLang))
                    }
                }

                // Feature 3: Rate App
                Button(
                    onClick = {
                        if (hapticsEnabled) hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        val packageName = "com.ahmedsamy.imagetype"
                        val marketUri = Uri.parse("market://details?id=$packageName")
                        val webUri = Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                        val intent = Intent(Intent.ACTION_VIEW, marketUri).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(webIntent)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                ) {
                    Row(
                        modifier = Modifier.semantics(mergeDescendants = true) {},
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.StarRate, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(getTranslation("rate_app_button", currentLang))
                    }
                }
            }
        }
    }
}

// Beautiful customized drop-down items to eliminate boilerplates
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> ImageTypeDropdown(
    label: String,
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    getLabel: (T) -> String = { it.toString() },
    modifier: Modifier = Modifier,
    hapticFeedback: HapticFeedback? = null,
    hapticsEnabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val localizedGetLabel: (T) -> String = { item ->
        val stringRepresentation = getLabel(item)
        val resId = getDropdownItemStringResId(stringRepresentation)
        if (resId != 0) {
            context.getString(resId)
        } else {
            stringRepresentation
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            expanded = !expanded
            if (hapticsEnabled) hapticFeedback?.performHapticFeedback(HapticFeedbackType.LongPress)
        },
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        OutlinedTextField(
            readOnly = true,
            value = localizedGetLabel(selectedOption),
            onValueChange = {},
            label = { Text(label, style = MaterialTheme.typography.bodyMedium) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .clearAndSetSemantics {
                    contentDescription = "$label: Selected ${localizedGetLabel(selectedOption)}. Double-tap to change."
                }
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(localizedGetLabel(option), style = MaterialTheme.typography.bodyLarge) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                        if (hapticsEnabled) hapticFeedback?.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    modifier = Modifier.minimumInteractiveComponentSize()
                )
            }
        }
    }
}

fun getDropdownItemStringResId(option: String): Int {
    return when (option) {
        // Colors
        "White" -> R.string.color_white
        "Black" -> R.string.color_black
        "Red" -> R.string.color_red
        "Blue" -> R.string.color_blue
        "Green" -> R.string.color_green
        "Yellow" -> R.string.color_yellow
        "Orange" -> R.string.color_orange
        "Pink" -> R.string.color_pink
        "Purple" -> R.string.color_purple
        "Gray" -> R.string.color_gray
        "Cyan" -> R.string.color_cyan
        "Magenta" -> R.string.color_magenta
        "Light Blue" -> R.string.color_light_blue
        "Light Green" -> R.string.color_light_green
        
        // Text Position
        "Top Left" -> R.string.pos_top_left
        "Top Center" -> R.string.pos_top_center
        "Top Right" -> R.string.pos_top_right
        "Middle Left" -> R.string.pos_middle_left
        "Center" -> R.string.pos_center
        "Middle Right" -> R.string.pos_middle_right
        "Bottom Left" -> R.string.pos_bottom_left
        "Bottom Center" -> R.string.pos_bottom_center
        "Bottom Right" -> R.string.pos_bottom_right
        
        // Background Type
        "Solid Color" -> R.string.bg_solid
        "Transparent" -> R.string.bg_transparent
        "Existing Image" -> R.string.bg_existing
        
        // Dimensions
        "Standard (1920x1080)" -> R.string.dim_standard
        "Standard HD (1280x720)" -> R.string.dim_hd
        "Square (1080x1080)" -> R.string.dim_square
        "Portrait/Reels (1080x1920)" -> R.string.dim_portrait
        "4:3 (1024x768)" -> R.string.dim_4_3
        "3:4 (768x1024)" -> R.string.dim_3_4
        "Ultrawide (2560x1080)" -> R.string.dim_ultrawide
        "Twitter Header (1500x500)" -> R.string.dim_twitter
        
        // Quality
        "100% (High)" -> R.string.qual_high
        "80% (Medium)" -> R.string.qual_medium
        "60% (Low)" -> R.string.qual_low
        
        // Font style
        "Regular" -> R.string.style_regular
        "Bold" -> R.string.style_bold
        "Italic" -> R.string.style_italic
        "Bold-Italic" -> R.string.style_bold_italic
        
        // App Theme
        "Dark Mode" -> R.string.theme_dark
        "Light Mode" -> R.string.theme_light
        "System Default" -> R.string.theme_system
        
        else -> 0
    }
}
