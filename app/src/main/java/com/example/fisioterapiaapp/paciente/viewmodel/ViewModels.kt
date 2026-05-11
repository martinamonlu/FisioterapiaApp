package com.example.fisioterapiaapp.paciente.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.fisioterapiaapp.paciente.model.*
import com.example.fisioterapiaapp.paciente.repository.PacienteRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

// ════════════════════════════════════════════════════════════
//  PLAN SEMANAL
//
//  Como el plan del fisio guarda los ejercicios con sus
//  `diasSemana[]`, este ViewModel calcula los ejercicios de cada
//  día filtrando la lista plana del Plan.
// ════════════════════════════════════════════════════════════

class PlanViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = PacienteRepository()
    fun cargar() {
        viewModelScope.launch {
            try {
                _plan.value = repo.getPlanActivo()
            } catch (e: Exception) {
                android.util.Log.e("PLAN_VM", "Error: ${e.message}")
            }
        }
    }
    private val _plan = MutableLiveData<Plan?>()
    val plan: LiveData<Plan?> = _plan

    private val _semanaActual = MutableLiveData(1)
    val semanaActual: LiveData<Int> = _semanaActual

    fun semanaAnterior() {
        val n = (_semanaActual.value ?: 1) - 1
        if (n >= 1) _semanaActual.value = n
    }

    fun semanaSiguiente() {
        val n = (_semanaActual.value ?: 1) + 1
        val max = plan.value?.duracionSemanas ?: 16
        if (n <= max) _semanaActual.value = n
    }

    /** Devuelve los ejercicios de un día concreto a partir del Plan. */
    fun ejerciciosDelDia(dia: String): List<EjercicioPlan> {
        val p = plan.value ?: return emptyList()
        return p.ejercicios.filter { it.seRealizaEn(dia) }
    }
}

// ════════════════════════════════════════════════════════════
//  REGISTRO POST-SESIÓN
// ════════════════════════════════════════════════════════════

class RegistroSesionViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = PacienteRepository()

    private val _guardarState = MutableLiveData<UiState<Unit>>(UiState.Idle)
    val guardarState: LiveData<UiState<Unit>> = _guardarState

    // Campos del formulario observables
    val sesionCompletada = MutableLiveData(false)
    val seriesCompletadas = MutableLiveData(0)
    val repeticionesCompletadas = MutableLiveData(0)
    val pesoUsado = MutableLiveData("")
    val eva = MutableLiveData(0)          // 0-10
    val rpe = MutableLiveData(6)          // 6-20 (Borg)
    val fatiga = MutableLiveData(5)       // 0-10
    val notas = MutableLiveData("")

    fun guardarRegistro(planId: String, ejercicioIndex: Int, dia: String) {
        _guardarState.value = UiState.Loading
        viewModelScope.launch {
            val registro = RegistroSesion(
                planId = planId,
                ejercicioIndex = ejercicioIndex,
                dia = dia,
                sesionCompletada = sesionCompletada.value ?: false,
                seriesCompletadas = seriesCompletadas.value ?: 0,
                repsCompletadas = repeticionesCompletadas.value ?: 0,
                pesoUsado = pesoUsado.value ?: "",
                eva = eva.value ?: 0,
                rpe = rpe.value ?: 6,
                fatiga = fatiga.value ?: 5,
                notas = notas.value ?: ""
            )
            repo.guardarRegistro(registro)
                .onSuccess { _guardarState.value = UiState.Success(Unit) }
                .onFailure {
                    _guardarState.value =
                        UiState.Error(it.localizedMessage ?: "Error al guardar")
                }
        }
    }
}

// ════════════════════════════════════════════════════════════
//  PROGRESO
// ════════════════════════════════════════════════════════════

class ProgresoViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = PacienteRepository()

    private val _puntos = MutableLiveData<UiState<List<PuntoProgreso>>>(UiState.Idle)
    val puntos: LiveData<UiState<List<PuntoProgreso>>> = _puntos

    fun cargar() {
        _puntos.value = UiState.Loading
        viewModelScope.launch {
            try {
                val plan = repo.getPlanActivo()
                if (plan == null) {
                    _puntos.value = UiState.Success(emptyList())
                    return@launch
                }
                val pts = repo.calcularPuntosPorSemana(plan)
                _puntos.value = UiState.Success(pts)
            } catch (e: Exception) {
                _puntos.value = UiState.Error(e.localizedMessage ?: "Error al cargar gráficas")
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
//  CHAT
// ════════════════════════════════════════════════════════════

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = PacienteRepository()

    val mensajes: LiveData<List<MensajeChat>> = repo.observarChat()
        .catch { emit(emptyList()) }
        .asLiveData()

    private val _enviarState = MutableLiveData<UiState<Unit>>(UiState.Idle)
    val enviarState: LiveData<UiState<Unit>> = _enviarState

    fun enviar(texto: String) {
        if (texto.isBlank()) return
        _enviarState.value = UiState.Loading
        viewModelScope.launch {
            repo.enviarMensaje(texto.trim())
                .onSuccess { _enviarState.value = UiState.Success(Unit) }
                .onFailure {
                    _enviarState.value =
                        UiState.Error(it.localizedMessage ?: "Error al enviar")
                }
        }
    }
}

// ════════════════════════════════════════════════════════════
//  INFORMES PDF
// ════════════════════════════════════════════════════════════

class InformesViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = PacienteRepository()

    private val _informes = MutableLiveData<UiState<List<Informe>>>(UiState.Idle)
    val informes: LiveData<UiState<List<Informe>>> = _informes

    fun cargar() {
        _informes.value = UiState.Loading
        viewModelScope.launch {
            try {
                val list = repo.getInformes()
                _informes.value = UiState.Success(list)
            } catch (e: Exception) {
                _informes.value = UiState.Error(e.localizedMessage ?: "Error al cargar informes")
            }
        }
    }
}
