package com.example.fisioterapiaapp.paciente.model

import android.os.Parcelable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import kotlinx.parcelize.Parcelize

/**
 * Modelo del paciente alineado con el esquema creado por el fisio
 * en AddPacienteActivity / SignInPacienteActivity:
 *
 * Colección: pacientes/{pacienteId}
 */
data class Paciente(
    @DocumentId val id: String = "",
    val userId: String? = null,
    val nombre: String = "",
    val apellidos: String = "",
    val dni: String = "",
    val email: String = "",
    val diagnostico: String = "",
    val fisioterapeutaId: String = "",
    val primerLogin: Boolean = true,
    val estadoCuenta: String = "pendiente",
    val fechaRegistro: Timestamp? = null,
    val fechaNacimiento: Timestamp? = null,
    val sexo: String = "",
    val deporte: String = "",
    val urlFotoPerfil: String = ""
) {
    val nombreCompleto: String get() = "$nombre $apellidos"
    val cuentaActivada: Boolean get() = estadoCuenta == "activa" && userId != null
}

/**
 * Plan de ejercicios creado por el fisio.
 * Colección: planes_ejercicio/{planId}
 */
data class Plan(
    @DocumentId val id: String = "",
    val pacienteId: String = "",
    val fisioterapeutaId: String = "",
    val fase: String = "",
    val duracionSemanas: Int = 0,
    val objetivos: String = "",
    val ejercicios: List<EjercicioPlan> = emptyList(),
    val fechaCreacion: Timestamp? = null,
    val activo: Boolean = true
)

/**
 * Ejercicio individual dentro de un plan.
 * Estructura guardada por CrearPlanActivity.
 */
@Parcelize
data class EjercicioPlan(
    val tipo: String = "",
    val repeticiones: Int = 0,
    val series: Int = 0,
    val peso: String = "",
    val diasSemana: List<String> = emptyList(),
    val videoUrl: String? = null,
    val nombreEjercicio: String = ""
) : Parcelable {
    val cargaTexto: String
        get() = if (peso.isBlank() || peso == "0") "Sin carga" else "$peso kg"

    fun seRealizaEn(dia: String): Boolean =
        diasSemana.any { it.equals(dia, ignoreCase = true) }
}

/**
 * Registro post-sesión del paciente.
 */
data class RegistroSesion(
    @DocumentId val id: String = "",
    val pacienteId: String = "",
    val planId: String = "",
    val ejercicioIndex: Int = -1,
    val dia: String = "",
    val fecha: Timestamp = Timestamp.now(),
    val sesionCompletada: Boolean = true,

    // Ejercicios marcados en el dashboard antes de registrar la sesión
    val ejerciciosCompletados: List<Int> = emptyList(),
    val ejerciciosCompletadosNombres: List<String> = emptyList(),

    // Carga externa
    val seriesCompletadas: Int = 0,
    val repsCompletadas: Int = 0,
    val pesoUsado: String = "",

    // Carga interna
    val eva: Int = 0,
    val rpe: Int = 6,
    val fatiga: Int = 5,

    // Molestias, incidencias y observaciones
    val notas: String = ""
)


/**
 * Alerta generada para el paciente.
 */
data class Alerta(
    @DocumentId val id: String = "",
    val pacienteId: String = "",
    val tipo: TipoAlerta = TipoAlerta.INFO,
    val titulo: String = "",
    val descripcion: String = "",
    val leida: Boolean = false,
    val urgente: Boolean = false,
    val creadoEn: Timestamp = Timestamp.now()
)

enum class TipoAlerta { EVA_ALTO, ADHERENCIA_BAJA, INACTIVIDAD, MENSAJE_FISIO, INFO }

/**
 * Mensaje del chat paciente-fisio.
 */
data class MensajeChat(
    @DocumentId val id: String = "",
    val texto: String = "",
    val emisorId: String = "",
    val esPaciente: Boolean = true,
    val timestamp: Timestamp = Timestamp.now()
)

/**
 * Informe PDF generado por el fisio.
 */
data class Informe(
    @DocumentId val id: String = "",
    val pacienteId: String = "",
    val titulo: String = "",
    val urlPdf: String = "",
    val comentarioFisio: String = "",
    val fechaGeneracion: Timestamp = Timestamp.now()
)

/**
 * Estado de UI genérico.
 */
sealed class UiState<out T> {
    data object Idle : UiState<Nothing>()
    data object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val mensaje: String, val cause: Throwable? = null) : UiState<Nothing>()
}

/**
 * Punto para gráficas de progreso.
 */
data class PuntoProgreso(
    val semana: Int,
    val eva: Float,
    val rpe: Float,
    val adherencia: Float,
    val carga: Float
)
