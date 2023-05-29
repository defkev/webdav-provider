package me.alexbakker.webdav

import android.app.Application
import android.content.Context
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class WebDavApplication : Application() {
    init {
        instance = this
    }
    companion object {
        private var instance: WebDavApplication? = null

        fun applicationContext() : Context {
            return instance!!.applicationContext
        }
    }
}
