package com.example.fisioterapiaapp

import android.app.Application
import android.util.Log

class RehappApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // Si es un PERMISSION_DENIED de Firestore, ignorarlo silenciosamente
            if (throwable.message?.contains("PERMISSION_DENIED") == true ||
                throwable.cause?.message?.contains("PERMISSION_DENIED") == true) {
                Log.w("RehappApp", "Firestore PERMISSION_DENIED ignorado: ${throwable.message}")
                return@setDefaultUncaughtExceptionHandler
            }
            // Cualquier otro crash, dejarlo pasar al handler por defecto
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}