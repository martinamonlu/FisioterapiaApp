package com.example.fisioterapiaapp.paciente.ui.registro

import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.fisioterapiaapp.databinding.ActivityRegistroSesionBinding
import com.example.fisioterapiaapp.paciente.model.UiState
import com.example.fisioterapiaapp.paciente.viewmodel.RegistroSesionViewModel

class RegistroSesionActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PLAN_ID = "extra_plan_id"
        const val EXTRA_DIA = "extra_dia"
        const val EXTRA_EJERCICIO_INDEX = "extra_ejercicio_index"
        const val EXTRA_EJERCICIOS_COMPLETADOS = "extra_ejercicios_completados"
        const val EXTRA_EJERCICIOS_COMPLETADOS_NOMBRES = "extra_ejercicios_completados_nombres"
    }

    private lateinit var binding: ActivityRegistroSesionBinding
    private val vm: RegistroSesionViewModel by viewModels()

    private var planId: String = ""
    private var dia: String = ""
    private var ejerciciosCompletados: List<Int> = emptyList()
    private var ejerciciosCompletadosNombres: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistroSesionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        planId = intent.getStringExtra(EXTRA_PLAN_ID) ?: ""
        dia = intent.getStringExtra(EXTRA_DIA) ?: ""
        ejerciciosCompletados = intent.getIntegerArrayListExtra(EXTRA_EJERCICIOS_COMPLETADOS) ?: emptyList()
        ejerciciosCompletadosNombres = intent.getStringArrayListExtra(EXTRA_EJERCICIOS_COMPLETADOS_NOMBRES) ?: emptyList()

        binding.tvTitulo.text = "Registro post-sesión – $dia"
        binding.btnAtras.setOnClickListener { finish() }

        configurarUI()
        observarViewModel()
    }

    private fun configurarUI() {
        val hayEjerciciosMarcados = ejerciciosCompletados.isNotEmpty()

        vm.sesionCompletada.value = hayEjerciciosMarcados
        binding.checkSesionCompletada.isChecked = hayEjerciciosMarcados
        binding.layoutCargaExterna.visibility =
            if (hayEjerciciosMarcados) View.VISIBLE else View.GONE

        binding.checkSesionCompletada.setOnCheckedChangeListener { _, checked ->
            vm.sesionCompletada.value = checked
            binding.layoutCargaExterna.visibility =
                if (checked) View.VISIBLE else View.GONE
        }

        binding.btnMenosSeries.setOnClickListener {
            val v = (vm.seriesCompletadas.value ?: 0) - 1
            if (v >= 0) {
                vm.seriesCompletadas.value = v
                binding.tvSeriesVal.text = v.toString()
            }
        }

        binding.btnMasSeries.setOnClickListener {
            val v = (vm.seriesCompletadas.value ?: 0) + 1
            vm.seriesCompletadas.value = v
            binding.tvSeriesVal.text = v.toString()
        }

        binding.btnMenosReps.setOnClickListener {
            val v = (vm.repeticionesCompletadas.value ?: 0) - 1
            if (v >= 0) {
                vm.repeticionesCompletadas.value = v
                binding.tvRepsVal.text = v.toString()
            }
        }

        binding.btnMasReps.setOnClickListener {
            val v = (vm.repeticionesCompletadas.value ?: 0) + 1
            vm.repeticionesCompletadas.value = v
            binding.tvRepsVal.text = v.toString()
        }

        binding.seekEva.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, value: Int, fromUser: Boolean) {
                vm.eva.value = value
                binding.tvEvaVal.text = "$value – ${etiquetaEva(value)}"
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        binding.seekRpe.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, value: Int, fromUser: Boolean) {
                val rpe = value + 6
                vm.rpe.value = rpe
                binding.tvRpeVal.text = "$rpe – ${etiquetaRpe(rpe)}"
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        binding.etNotas.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                vm.notas.value = s.toString()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.btnGuardar.setOnClickListener {
            if (planId.isBlank()) {
                Toast.makeText(this, "No hay plan activo", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (binding.checkSesionCompletada.isChecked && ejerciciosCompletados.isEmpty()) {
                Toast.makeText(
                    this,
                    "Marca al menos un ejercicio en el resumen del día antes de guardar la sesión como completada",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            vm.guardarRegistro(
                planId = planId,
                dia = dia,
                ejerciciosCompletados = ejerciciosCompletados,
                ejerciciosCompletadosNombres = ejerciciosCompletadosNombres
            )
        }
    }

    private fun observarViewModel() {
        vm.guardarState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressGuardar.visibility = View.VISIBLE
                    binding.btnGuardar.isEnabled = false
                }

                is UiState.Success -> {
                    binding.progressGuardar.visibility = View.GONE
                    Toast.makeText(this, "Registro guardado", Toast.LENGTH_SHORT).show()
                    finish()
                }

                is UiState.Error -> {
                    binding.progressGuardar.visibility = View.GONE
                    binding.btnGuardar.isEnabled = true
                    Toast.makeText(this, state.mensaje, Toast.LENGTH_LONG).show()
                }

                else -> {
                    binding.progressGuardar.visibility = View.GONE
                    binding.btnGuardar.isEnabled = true
                }
            }
        }
    }

    private fun etiquetaEva(v: Int) = when (v) {
        0 -> "Sin dolor"
        in 1..3 -> "Dolor leve"
        in 4..6 -> "Dolor moderado"
        in 7..9 -> "Dolor intenso"
        else -> "Dolor insoportable"
    }

    private fun etiquetaRpe(v: Int) = when (v) {
        6 -> "Sin esfuerzo"
        in 7..8 -> "Muy ligero"
        in 9..10 -> "Ligero"
        in 11..12 -> "Algo duro"
        in 13..14 -> "Duro"
        in 15..16 -> "Muy duro"
        in 17..18 -> "Extremadamente duro"
        else -> "Máximo esfuerzo"
    }
}
