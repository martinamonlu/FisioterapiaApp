package com.example.fisioterapiaapp.paciente.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Resultado del flujo de login del paciente, alineado con el esquema
 * creado por el fisio (`estadoCuenta`, `primerLogin`).
 */
sealed class LoginResultado {
    data object Cargando : LoginResultado()
    /** Cuenta pendiente: hay que crearla en Auth con la contraseña temporal. */
    data class ActivarCuenta(val pacienteId: String, val email: String) : LoginResultado()
    /** Login OK pero es primer acceso: pedir cambio de contraseña. */
    data class PrimerLogin(val pacienteId: String) : LoginResultado()
    /** Login OK y cuenta normal: ir al dashboard. */
    data class Ok(val pacienteId: String) : LoginResultado()
    data class Error(val mensaje: String) : LoginResultado()
}

class AuthPacienteViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _estado = MutableLiveData<LoginResultado>()
    val estado: LiveData<LoginResultado> = _estado

    private val _cambioPasswordOk = MutableLiveData<Boolean>()
    val cambioPasswordOk: LiveData<Boolean> = _cambioPasswordOk

    private val _errorCambioPassword = MutableLiveData<String?>()
    val errorCambioPassword: LiveData<String?> = _errorCambioPassword

    /**
     * Flujo de login. Replica la lógica del SignInPacienteActivity original
     * (busca por email, comprueba estadoCuenta, activa si está pendiente,
     * detecta primerLogin).
     */
    fun login(email: String, password: String) {
        _estado.value = LoginResultado.Cargando
        viewModelScope.launch {
            try {
                // 1) Intentar autenticar en Firebase Auth (cuenta ya activada)
                val result = auth.signInWithEmailAndPassword(email, password).await()
                val uid = result.user?.uid
                    ?: run { _estado.value = LoginResultado.Error("Error al iniciar sesión"); return@launch }

                // 2) Buscar el documento del paciente por userId
                val snap = db.collection("pacientes")
                    .whereEqualTo("userId", uid)
                    .limit(1)
                    .get().await()
                val doc = snap.documents.firstOrNull()
                    ?: run { _estado.value = LoginResultado.Error("Paciente no encontrado"); return@launch }

                // 3) Comprobar primerLogin
                val primerLogin = doc.getBoolean("primerLogin") ?: false
                _estado.value =
                    if (primerLogin) LoginResultado.PrimerLogin(doc.id)
                    else LoginResultado.Ok(doc.id)

            } catch (authError: Exception) {
                // Auth falló: puede ser cuenta pendiente (aún no creada en Firebase Auth)
                try {
                    val snap = db.collection("pacientes")
                        .whereEqualTo("email", email)
                        .limit(1)
                        .get().await()
                    val doc = snap.documents.firstOrNull()
                    if (doc != null
                        && doc.getString("estadoCuenta") == "pendiente"
                        && doc.getString("dni") == password
                    ) {
                        _estado.value = LoginResultado.ActivarCuenta(doc.id, email)
                    } else {
                        _estado.value = LoginResultado.Error("Email o contraseña incorrectos")
                    }
                } catch (e: Exception) {
                    _estado.value = LoginResultado.Error(
                        authError.localizedMessage ?: "Email o contraseña incorrectos"
                    )
                }
            }
        }
    }

    /**
     * Activa una cuenta pendiente: crea el usuario en Auth con la contraseña
     * temporal y actualiza el documento del paciente con `userId` y
     * `estadoCuenta = "activa"`.
     */
    fun activarCuenta(pacienteId: String, email: String, password: String) {
        _estado.value = LoginResultado.Cargando
        viewModelScope.launch {
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val newUid = result.user?.uid
                if (newUid == null) {
                    _estado.value = LoginResultado.Error("No se pudo crear la cuenta")
                    return@launch
                }
                db.collection("pacientes")
                    .document(pacienteId)
                    .update(
                        mapOf(
                            "userId" to newUid,
                            "estadoCuenta" to "activa"
                        )
                    ).await()

                // Tras activar, forzar cambio de contraseña
                _estado.value = LoginResultado.PrimerLogin(pacienteId)
            } catch (e: Exception) {
                _estado.value = LoginResultado.Error(
                    e.localizedMessage ?: "Error al activar la cuenta"
                )
            }
        }
    }

    /**
     * Cambio de contraseña obligatorio en el primer login.
     * Re-autentica al usuario antes de actualizar (requisito de Firebase
     * para cambios sensibles).
     */
    fun cambiarPassword(
        passwordActual: String,
        passwordNueva: String,
        pacienteId: String
    ) {
        _errorCambioPassword.value = null
        viewModelScope.launch {
            try {
                val user = auth.currentUser
                    ?: run {
                        _errorCambioPassword.value = "Sesión no válida"
                        return@launch
                    }
                val email = user.email
                    ?: run {
                        _errorCambioPassword.value = "Cuenta sin email"
                        return@launch
                    }

                // Re-autenticación
                val cred = EmailAuthProvider.getCredential(email, passwordActual)
                user.reauthenticate(cred).await()

                // Actualizar contraseña
                user.updatePassword(passwordNueva).await()

                // Marcar primerLogin = false
                db.collection("pacientes")
                    .document(pacienteId)
                    .update("primerLogin", false).await()

                _cambioPasswordOk.value = true
            } catch (e: Exception) {
                _errorCambioPassword.value = e.localizedMessage
                    ?: "No se pudo cambiar la contraseña"
            }
        }
    }

    fun cerrarSesion() {
        auth.signOut()
    }
}
