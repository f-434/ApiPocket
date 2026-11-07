package com.apipocket

import android.app.Application
import com.apipocket.di.AppContainer

class ApiPocketApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
