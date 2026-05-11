package com.example.fisioterapiaapp.paciente.util

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.firebase.auth.FirebaseAuth
import java.util.concurrent.TimeUnit

/**
 * Gestor de sesión del paciente con almacenamiento cifrado (AES256-GCM)
 * y timeout de inactividad de 30 minutos.
 */
class SessionManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "rehapp_session_secure"
        private const val KEY_PACIENTE_ID = "paciente_id"
        private const val KEY_LAST_ACTIVE = "last_active_ms"
        val SESSION_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(30)
    }

    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun guardarSesion(pacienteId: String) {
        prefs.edit()
            .putString(KEY_PACIENTE_ID, pacienteId)
            .putLong(KEY_LAST_ACTIVE, System.currentTimeMillis())
            .apply()
    }

    fun actualizarUltimaActividad() {
        prefs.edit()
            .putLong(KEY_LAST_ACTIVE, System.currentTimeMillis())
            .apply()
    }

    fun esSesionValida(): Boolean {
        return FirebaseAuth.getInstance().currentUser != null
    }

    fun getPacienteId(): String? = prefs.getString(KEY_PACIENTE_ID, null)

    fun cerrarSesion() {
        prefs.edit().clear().apply()
        FirebaseAuth.getInstance().signOut()
    }
}
