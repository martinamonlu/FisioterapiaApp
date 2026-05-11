package com.example.fisioterapiaapp.paciente.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.fisioterapiaapp.paciente.model.*
import com.example.fisioterapiaapp.paciente.repository.PacienteRepository
import com.example.fisioterapiaapp.paciente.util.diaDeHoy
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class DashboardPacienteViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = PacienteRepository()

    // ── Paciente ─────────────────────────────────────────────
    private val _paciente = MutableLiveData<Paciente?>()
    val paciente: LiveData<Paciente?> = _paciente

    // ── Plan activo ──────────────────────────────────────────
    private val _plan = MutableLiveData<Plan?>()
    val plan: LiveData<Plan?> = _plan

    // ── Ejercicios del día actual (derivado del plan) ────────
    private val _ejerciciosHoy = MediatorLiveData<List<EjercicioPlan>>().apply {
        addSource(_plan) { p ->
            val hoy = diaDeHoy()
            value = p?.ejercicios?.filter { it.seRealizaEn(hoy) } ?: emptyList()
        }
    }
    val ejerciciosHoy: LiveData<List<EjercicioPlan>> = _ejerciciosHoy


    // ── Alertas pendientes ───────────────────────────────────
    private val _alertas = MutableLiveData<List<Alerta>>(emptyList())
    val alertas: LiveData<List<Alerta>> = _alertas

    // ── Adherencia global ────────────────────────────────────
    private val _adherencia = MutableLiveData(0f)
    val adherencia: LiveData<Float> = _adherencia

    // ── Registro de hoy (si existe) ──────────────────────────
    private val _registroHoy = MutableLiveData<RegistroSesion?>()
    val registroHoy: LiveData<RegistroSesion?> = _registroHoy

    /** Carga adherencia y registro de hoy cuando ya hay plan. */
    fun cargarDashboard() {
        viewModelScope.launch {
            try {
                val p = repo.getPaciente()
                android.util.Log.d("DASH_DEBUG", "Paciente: ${p?.nombre}, id: ${p?.id}")
                _paciente.postValue(p)

                val plan = repo.getPlanActivo()
                android.util.Log.d("DASH_DEBUG", "Plan cargado: ${plan?.id}")
                android.util.Log.d("DASH_DEBUG", "Ejercicios en plan: ${plan?.ejercicios?.size}")
                plan?.ejercicios?.forEachIndexed { i, ej ->
                    android.util.Log.d("DASH_DEBUG", "  [$i] '${ej.nombreEjercicio}' dias=${ej.diasSemana}")
                }
                android.util.Log.d("DASH_DEBUG", "Día de hoy: '${diaDeHoy()}'")
                _plan.postValue(plan)

                if (plan != null) {
                    _registroHoy.postValue(repo.getRegistroDia(plan.id, diaDeHoy()))
                    val registros = repo.getRegistrosDePlan(plan.id)
                    val sesionesEsperadas = plan.ejercicios.sumOf { it.diasSemana.size } * plan.duracionSemanas
                    if (sesionesEsperadas > 0) {
                        val completadas = registros.count { it.sesionCompletada }
                        _adherencia.postValue((completadas.toFloat() / sesionesEsperadas * 100f).coerceIn(0f, 100f))
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("DASH_DEBUG", "Error: ${e.message}", e)
            }
        }
    }
}
