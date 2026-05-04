package com.ahmedsamy.imagetype

import android.app.Application
import com.ahmedsamy.imagetype.di.AppContainer

class ImageTypeApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
