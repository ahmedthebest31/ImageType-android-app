package com.ahmedsamy.imagetype.di

import android.content.Context
import com.ahmedsamy.imagetype.PreferencesManager
import com.ahmedsamy.imagetype.util.ImageGenerator
import com.ahmedsamy.imagetype.util.ImageStorageManager
import com.ahmedsamy.imagetype.util.TemplateSerializer

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val preferencesManager: PreferencesManager = PreferencesManager(appContext)
    val imageGenerator: ImageGenerator = ImageGenerator(appContext)
    val imageStorageManager: ImageStorageManager = ImageStorageManager(appContext)
    val templateSerializer: TemplateSerializer = TemplateSerializer()
}
