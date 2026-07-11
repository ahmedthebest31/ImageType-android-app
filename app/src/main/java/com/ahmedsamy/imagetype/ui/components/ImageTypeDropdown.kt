package com.ahmedsamy.imagetype.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.ahmedsamy.imagetype.R
import com.ahmedsamy.imagetype.util.LocalAppLanguage
import java.util.Locale

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
    val currentLang = LocalAppLanguage.current

    val localizedContext = remember(context, currentLang) {
        val locale = Locale.forLanguageTag(currentLang)
        val config = Configuration(context.resources.configuration).apply {
            setLocale(locale)
        }
        context.createConfigurationContext(config)
    }

    val localizedGetLabel: (T) -> String = { item ->
        val stringRepresentation = getLabel(item)
        val resId = getDropdownItemStringResId(stringRepresentation)
        if (resId != 0) {
            localizedContext.getString(resId)
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
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
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
                    modifier = Modifier
                )
            }
        }
    }
}

fun getDropdownItemStringResId(option: String): Int {
    return when (option) {
        "White" -> R.string.color_white
        "Black" -> R.string.color_black
        "Red" -> R.string.color_red
        "Blue" -> R.string.color_blue
        "Green" -> R.string.color_green
        "Islamic Green" -> R.string.color_islamic_green
        "Yellow" -> R.string.color_yellow
        "Orange" -> R.string.color_orange
        "Pink" -> R.string.color_pink
        "Purple" -> R.string.color_purple
        "Gray" -> R.string.color_gray
        "Cyan" -> R.string.color_cyan
        "Magenta" -> R.string.color_magenta
        "Light Blue" -> R.string.color_light_blue
        "Light Green" -> R.string.color_light_green
        "Top Left" -> R.string.pos_top_left
        "Top Center" -> R.string.pos_top_center
        "Top Right" -> R.string.pos_top_right
        "Middle Left" -> R.string.pos_middle_left
        "Center" -> R.string.pos_center
        "Middle Right" -> R.string.pos_middle_right
        "Bottom Left" -> R.string.pos_bottom_left
        "Bottom Center" -> R.string.pos_bottom_center
        "Bottom Right" -> R.string.pos_bottom_right
        "Solid Color" -> R.string.bg_solid
        "Transparent" -> R.string.bg_transparent
        "Existing Image" -> R.string.bg_existing
        "Standard (1920x1080)" -> R.string.dim_standard
        "Standard HD (1280x720)" -> R.string.dim_hd
        "Square (1080x1080)" -> R.string.dim_square
        "Portrait/Reels (1080x1920)" -> R.string.dim_portrait
        "4:3 (1024x768)" -> R.string.dim_4_3
        "3:4 (768x1024)" -> R.string.dim_3_4
        "Ultrawide (2560x1080)" -> R.string.dim_ultrawide
        "Twitter Header (1500x500)" -> R.string.dim_twitter
        "100% (High)" -> R.string.qual_high
        "80% (Medium)" -> R.string.qual_medium
        "60% (Low)" -> R.string.qual_low
        "Regular" -> R.string.style_regular
        "Bold" -> R.string.style_bold
        "Italic" -> R.string.style_italic
        "Bold-Italic" -> R.string.style_bold_italic
        "Dark Mode" -> R.string.theme_dark
        "Light Mode" -> R.string.theme_light
        "System Default" -> R.string.theme_system
        "System" -> R.string.language_system_default
        else -> 0
    }
}
