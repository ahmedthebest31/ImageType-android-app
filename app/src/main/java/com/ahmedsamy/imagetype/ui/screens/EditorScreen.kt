package com.ahmedsamy.imagetype.ui.screens

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import com.ahmedsamy.imagetype.util.stringRes
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.ahmedsamy.imagetype.EditorViewModel
import com.ahmedsamy.imagetype.FontManager
import com.ahmedsamy.imagetype.R
import com.ahmedsamy.imagetype.ui.components.ImageTypeDropdown
import com.ahmedsamy.imagetype.util.BgType
import com.ahmedsamy.imagetype.util.FontStyle
import com.ahmedsamy.imagetype.util.ImageDimensions
import com.ahmedsamy.imagetype.util.ImageQuality
import com.ahmedsamy.imagetype.util.TextColor
import com.ahmedsamy.imagetype.util.TextPosition
import kotlinx.coroutines.launch

@Composable
fun TabEditorWorkspace(viewModel: EditorViewModel, snackbarHostState: SnackbarHostState) {
    val scrollState = rememberScrollState()
    val hapticFeedback = LocalHapticFeedback.current
    val hapticsEnabled by viewModel.hapticsEnabled.collectAsState()
    val scope = rememberCoroutineScope()

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

    val pasteSuccessText = stringRes(R.string.paste_success)
    val pasteErrorText = stringRes(R.string.paste_error)
    val saveErrorText = stringRes(R.string.save_error)
    val shareErrorText = stringRes(R.string.share_error)
    val fontSizeSliderText = stringRes(R.string.font_size_slider)

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
                    value = inputText,
                    onValueChange = { viewModel.setInputText(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_text_field"),
                    minLines = 3,
                    maxLines = 6,
                    label = { Text(stringRes(R.string.input_placeholder)) },
                    placeholder = { Text(stringRes(R.string.input_placeholder)) }
                )

                Button(
                    onClick = {
                        if (hapticsEnabled) hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        if (clipboard.hasPrimaryClip() && clipboard.primaryClipDescription?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) == true) {
                            val item = clipboard.primaryClip?.getItemAt(0)
                            val pasteText = item?.text?.toString() ?: ""
                            if (pasteText.isNotBlank()) {
                                viewModel.setInputText(inputText + pasteText)
                                scope.launch { snackbarHostState.showSnackbar(pasteSuccessText) }
                            }
                        } else {
                            scope.launch { snackbarHostState.showSnackbar(pasteErrorText) }
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
                        Text(stringRes(R.string.paste_button))
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            viewModel.setFitText(!fitText)
                            if (hapticsEnabled) hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        .padding(8.dp)
                        .semantics(mergeDescendants = true) {},
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = fitText,
                        onCheckedChange = null,
                        modifier = Modifier.testTag("checkbox_fit_text")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringRes(R.string.fit_text_checkbox),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                AnimatedVisibility(
                    visible = !fitText,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column {
                            Text(
                                text = stringRes(R.string.font_size_label, fontSize.toInt()),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Slider(
                                value = fontSize,
                                onValueChange = { size -> viewModel.setFontSize(size) },
                                valueRange = 10f..200f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("slider_font_size")
                                    .semantics { contentDescription = "$fontSizeSliderText: ${fontSize.toInt()}" }
                            )
                        }

                        ImageTypeDropdown(
                            label = stringRes(R.string.text_position_label),
                            options = TextPosition.entries.toList(),
                            selectedOption = textPosition,
                            getLabel = { it.value },
                            onOptionSelected = { viewModel.setTextPosition(it) },
                            hapticFeedback = hapticFeedback,
                            hapticsEnabled = hapticsEnabled
                        )
                    }
                }

                val fontOptionLabels = FontManager.fontFamilies
                ImageTypeDropdown(
                    label = stringRes(R.string.font_family_label),
                    options = fontOptionLabels,
                    selectedOption = fontFamily,
                    onOptionSelected = { viewModel.setFontFamily(it) },
                    hapticFeedback = hapticFeedback,
                    hapticsEnabled = hapticsEnabled
                )

                ImageTypeDropdown(
                    label = stringRes(R.string.font_style_label),
                    options = FontStyle.entries.toList(),
                    selectedOption = fontStyle,
                    getLabel = { it.value },
                    onOptionSelected = { viewModel.setFontStyle(it) },
                    hapticFeedback = hapticFeedback,
                    hapticsEnabled = hapticsEnabled
                )

                ImageTypeDropdown(
                    label = stringRes(R.string.text_color_label),
                    options = TextColor.entries.toList(),
                    selectedOption = textColor,
                    getLabel = { it.value },
                    onOptionSelected = { viewModel.setTextColor(it) },
                    hapticFeedback = hapticFeedback,
                    hapticsEnabled = hapticsEnabled
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            viewModel.setTextShadow(!textShadow)
                            if (hapticsEnabled) hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        .padding(8.dp)
                        .semantics(mergeDescendants = true) {},
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = textShadow,
                        onCheckedChange = null,
                        modifier = Modifier.testTag("checkbox_text_shadow")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringRes(R.string.enable_shadow_label),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ImageTypeDropdown(
                    label = stringRes(R.string.dimensions_label),
                    options = ImageDimensions.entries.toList(),
                    selectedOption = dimensions,
                    getLabel = { it.value },
                    onOptionSelected = { viewModel.setDimensions(it) },
                    hapticFeedback = hapticFeedback,
                    hapticsEnabled = hapticsEnabled
                )

                ImageTypeDropdown(
                    label = stringRes(R.string.bg_type_label),
                    options = BgType.entries.toList(),
                    selectedOption = bgType,
                    getLabel = { it.value },
                    onOptionSelected = { viewModel.setBgType(it) },
                    hapticFeedback = hapticFeedback,
                    hapticsEnabled = hapticsEnabled
                )

                AnimatedVisibility(
                    visible = bgType == BgType.SolidColor,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    ImageTypeDropdown(
                        label = stringRes(R.string.bg_color_label),
                        options = TextColor.entries.toList(),
                        selectedOption = bgColor,
                        getLabel = { it.value },
                        onOptionSelected = { viewModel.setBgColor(it) },
                        hapticFeedback = hapticFeedback,
                        hapticsEnabled = hapticsEnabled
                    )
                }

                AnimatedVisibility(
                    visible = bgType == BgType.ExistingImage,
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
                            Text(stringRes(R.string.existing_image_button))
                        }

                        if (bgImageUri != null) {
                            Text(
                                text = stringRes(R.string.bg_selected, bgImageUri?.lastPathSegment ?: ""),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1
                            )
                        }
                    }
                }

                ImageTypeDropdown(
                    label = stringRes(R.string.image_quality_label),
                    options = ImageQuality.entries.toList(),
                    selectedOption = imageQuality,
                    getLabel = { it.value },
                    onOptionSelected = { viewModel.setImageQuality(it) },
                    hapticFeedback = hapticFeedback,
                    hapticsEnabled = hapticsEnabled
                )
            }
        }

        val errorText = stringRes(R.string.error_empty_text)
        val successSaveText = stringRes(R.string.success_save)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    if (hapticsEnabled) hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (inputText.isBlank()) {
                        scope.launch { snackbarHostState.showSnackbar(errorText) }
                    } else {
                        viewModel.saveImageToGallery { success ->
                            if (success) {
                                scope.launch { snackbarHostState.showSnackbar(successSaveText) }
                            } else {
                                scope.launch { snackbarHostState.showSnackbar(saveErrorText) }
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
                Text(stringRes(R.string.save_gallery_button), style = MaterialTheme.typography.titleMedium)
            }

            Button(
                onClick = {
                    if (hapticsEnabled) hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (inputText.isBlank()) {
                        scope.launch { snackbarHostState.showSnackbar(errorText) }
                    } else {
                        viewModel.shareGeneratedImage { success ->
                            if (!success) {
                                scope.launch { snackbarHostState.showSnackbar(shareErrorText) }
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
                    Text(stringRes(R.string.share_image_button), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
