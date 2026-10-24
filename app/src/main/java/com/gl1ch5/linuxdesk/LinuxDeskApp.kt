package com.gl1ch5.linuxdesk

import android.app.Application
import com.gl1ch5.linuxdesk.log.AppLog

class LinuxDeskApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppLog.init(this)
        AppLog.i("App", "Старт ${BuildConfig.VERSION_NAME} (${BuildConfig.BUILD_TYPE})")
    }
}
