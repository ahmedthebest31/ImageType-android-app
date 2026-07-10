package com.ahmedsamy.imagetype.util

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

val LocalAppLanguage = staticCompositionLocalOf { "en" }

@Composable
fun stringRes(id: Int, vararg formatArgs: Any): String {
    val context = LocalContext.current
    val lang = LocalAppLanguage.current
    val locale = Locale.forLanguageTag(lang)
    val localizedContext = remember(context, lang) {
        val config = Configuration(context.resources.configuration).apply {
            setLocale(locale)
        }
        context.createConfigurationContext(config)
    }
    return if (formatArgs.isEmpty()) {
        localizedContext.resources.getString(id)
    } else {
        val template = localizedContext.resources.getString(id)
        String.format(locale, template, *formatArgs)
    }
}
