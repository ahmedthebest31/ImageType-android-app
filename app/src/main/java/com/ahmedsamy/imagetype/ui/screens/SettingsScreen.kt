package com.ahmedsamy.imagetype.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.StarRate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.ahmedsamy.imagetype.R
import com.ahmedsamy.imagetype.ui.components.ImageTypeDropdown
import com.ahmedsamy.imagetype.util.AppTheme
import kotlinx.coroutines.launch

@Composable
fun TabSettingsWorkspace(viewModel: EditorViewModel, snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    val appTheme by viewModel.appTheme.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val hapticsEnabled by viewModel.hapticsEnabled.collectAsState()
    val savedTemplates by viewModel.savedTemplates.collectAsState()

    var templateInputName by remember { mutableStateOf("") }
    var selectedTemplate by remember { mutableStateOf<com.ahmedsamy.imagetype.util.Template?>(null) }

    val templateNameErrorText = stringRes(R.string.template_name_error)
    val shareAppTitleText = stringRes(R.string.share_app_title)
    val templateLoadedText = stringRes(R.string.template_loaded)
    val deleteTemplateText = stringRes(R.string.delete_template_button)
    val templateDeletedText = stringRes(R.string.template_deleted)

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
                ImageTypeDropdown(
                    label = stringRes(R.string.theme_label),
                    options = AppTheme.entries.toList(),
                    selectedOption = appTheme,
                    getLabel = { it.value },
                    onOptionSelected = { viewModel.updateTheme(it) },
                    hapticFeedback = hapticFeedback,
                    hapticsEnabled = hapticsEnabled
                )

                Spacer(modifier = Modifier.height(4.dp))

                val languages = listOf("en", "ar", "fr", "it", "tr", "de", "es")
                val languageDisplayNames = mapOf(
                    "en" to "English", "ar" to "\u0627\u0644\u0639\u0631\u0628\u064a\u0629", "fr" to "Fran\u00e7ais",
                    "it" to "Italiano", "tr" to "T\u00fcrk\u00e7e", "de" to "Deutsch", "es" to "Espa\u00f1ol"
                )

                ImageTypeDropdown(
                    label = stringRes(R.string.language_label),
                    options = languages,
                    selectedOption = appLanguage,
                    getLabel = { languageDisplayNames[it] ?: "English" },
                    onOptionSelected = { viewModel.updateLanguage(it) },
                    hapticFeedback = hapticFeedback,
                    hapticsEnabled = hapticsEnabled
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            viewModel.updateHaptics(!hapticsEnabled)
                            if (!hapticsEnabled) hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        .padding(8.dp)
                        .semantics(mergeDescendants = true) {},
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = hapticsEnabled,
                        onCheckedChange = null,
                        modifier = Modifier.testTag("switch_haptic_feedback")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringRes(R.string.haptic_toggle),
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
                OutlinedTextField(
                    value = templateInputName,
                    onValueChange = { templateInputName = it },
                    label = { Text(stringRes(R.string.template_name_placeholder)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("template_name_input"),
                    singleLine = true
                )

                val successTemplateText = stringRes(R.string.success_template)
                Button(
                    onClick = {
                        if (hapticsEnabled) hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (templateInputName.isNotBlank()) {
                            viewModel.saveAsTemplate(templateInputName)
                            scope.launch { snackbarHostState.showSnackbar(successTemplateText) }
                            templateInputName = ""
                        } else {
                            scope.launch { snackbarHostState.showSnackbar(templateNameErrorText) }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringRes(R.string.save_template_button))
                }

                if (savedTemplates.isNotEmpty()) {
                    if (selectedTemplate == null || savedTemplates.none { it.id == selectedTemplate!!.id }) {
                        selectedTemplate = savedTemplates.first()
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ImageTypeDropdown(
                            label = stringRes(R.string.load_template_label),
                            options = savedTemplates,
                            selectedOption = selectedTemplate ?: savedTemplates.first(),
                            getLabel = { it.name },
                            onOptionSelected = { template ->
                                selectedTemplate = template
                                viewModel.loadTemplate(template)
                                scope.launch { snackbarHostState.showSnackbar(String.format(templateLoadedText, template.name)) }
                            },
                            hapticFeedback = hapticFeedback,
                            hapticsEnabled = hapticsEnabled,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                if (hapticsEnabled) hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                selectedTemplate?.let { template ->
                                    viewModel.deleteTemplate(template)
                                    selectedTemplate = null
                                    scope.launch { snackbarHostState.showSnackbar(templateDeletedText) }
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = deleteTemplateText,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                } else {
                    selectedTemplate = null
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
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
                    Text(stringRes(R.string.about_downloads_button))
                }

                Button(
                    onClick = {
                        if (hapticsEnabled) hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        val shareText = "https://play.google.com/store/apps/details?id=com.ahmedsamy.imagetype"
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        val chooser = Intent.createChooser(intent, shareAppTitleText)
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
                        Text(stringRes(R.string.share_app_button))
                    }
                }

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
                        Text(stringRes(R.string.rate_app_button))
                    }
                }
            }
        }
    }
}
