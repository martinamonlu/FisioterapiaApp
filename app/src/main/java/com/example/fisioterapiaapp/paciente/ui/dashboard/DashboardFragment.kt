package com.example.fisioterapiaapp.paciente.ui.dashboard

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.fisioterapiaapp.R
import com.example.fisioterapiaapp.databinding.FragmentDashboardPacienteBinding
import com.example.fisioterapiaapp.paciente.model.EjercicioPlan
import com.example.fisioterapiaapp.paciente.model.RegistroSesion
import com.example.fisioterapiaapp.paciente.ui.registro.RegistroSesionActivity
import com.example.fisioterapiaapp.paciente.util.diaDeHoy
import com.example.fisioterapiaapp.paciente.util.gone
import com.example.fisioterapiaapp.paciente.util.visible
import com.example.fisioterapiaapp.paciente.viewmodel.DashboardPacienteViewModel
import com.example.fisioterapiaapp.paciente.ui.exercise.EjercicioDetalleActivity
import com.example.fisioterapiaapp.paciente.viewmodel.ProgresoViewModel
import com.example.fisioterapiaapp.paciente.model.UiState
import android.graphics.Color
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardPacienteBinding? = null
    private val binding get() = _binding!!
    private val vm: DashboardPacienteViewModel by viewModels()
    private val vmProgreso: ProgresoViewModel by viewModels()
    private var planIdActual: String = ""
    private var ejerciciosHoy: List<EjercicioHoyUi> = emptyList()
    private var ejerciciosSeleccionados: MutableSet<Int> = mutableSetOf()
    private var registroHoy: RegistroSesion? = null

    // Recibe el resultado de EjercicioDetalleActivity cuando el paciente completa un ejercicio
    private val ejercicioLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val indexCompletado = result.data?.getIntExtra(EjercicioDetalleActivity.RESULT_INDEX, -1) ?: -1
            if (indexCompletado >= 0) {
                ejerciciosSeleccionados.add(indexCompletado)
                renderEjerciciosHoy()
            }
        }
    }

    private data class EjercicioHoyUi(
        val indexPlan: Int,
        val ejercicio: EjercicioPlan
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardPacienteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvDiaHoy.text = "Hoy: ${diaDeHoy()}"

        binding.btnRegistrarSesion.isEnabled = true
        binding.btnRegistrarSesion.setOnClickListener {
            abrirRegistroPostSesion()
        }
        observarDatos()
        vm.cargarDashboard()
        configurarGraficaDashboard()
        vmProgreso.cargar()
        observarGrafica()
    }

    private fun observarDatos() {
        vm.paciente.observe(viewLifecycleOwner) { paciente ->
            val nombre = paciente?.nombre?.takeIf { it.isNotBlank() } ?: "paciente"
            binding.tvBienvenida.text = "Hola, $nombre 👋"
        }


        vm.plan.observe(viewLifecycleOwner) { plan ->
            if (plan == null) {
                planIdActual = ""
                ejerciciosHoy = emptyList()
                ejerciciosSeleccionados.clear()
                binding.tvSemanaInfo.text = "Sin plan activo"
                binding.tvFase.text = ""
                binding.btnRegistrarSesion.isEnabled = false
                renderEjerciciosHoy()
            } else {
                planIdActual = plan.id
                binding.tvSemanaInfo.text = "Semana 1 / ${plan.duracionSemanas}"
                binding.tvFase.text = "Fase: ${plan.fase}"
                ejerciciosHoy = plan.ejercicios.mapIndexedNotNull { index, ejercicio ->
                    if (ejercicio.seRealizaEn(diaDeHoy())) EjercicioHoyUi(index, ejercicio) else null
                }
                if (registroHoy == null) ejerciciosSeleccionados.clear()
                binding.btnRegistrarSesion.isEnabled = true
                binding.btnRegistrarSesion.text = "Registro de datos post-sesión"
                // ← SIN setOnClickListener aquí, ya está puesto arriba
                renderEjerciciosHoy()
            }
        }

        vm.registroHoy.observe(viewLifecycleOwner) { registro ->
            registroHoy = registro

            if (registro != null) {
                ejerciciosSeleccionados = registro.ejerciciosCompletados.toMutableSet()
                binding.btnRegistrarSesion.text = "✓ Sesión registrada"
                binding.btnRegistrarSesion.isEnabled = false
            } else {
                binding.btnRegistrarSesion.text = "Registro de datos post-sesión"
                binding.btnRegistrarSesion.isEnabled = planIdActual.isNotBlank()
            }

            renderEjerciciosHoy()
        }

        vm.alertas.observe(viewLifecycleOwner) { alertas ->
            val pendientes = alertas.filter { !it.leida }
            if (pendientes.isNotEmpty()) {
                binding.cardAlerta.visible()
                binding.tvAlerta.text = pendientes.first().descripcion
                binding.btnCerrarAlerta.setOnClickListener { binding.cardAlerta.gone() }
            } else {
                binding.cardAlerta.gone()
            }
        }

        vm.adherencia.observe(viewLifecycleOwner) { pct ->
            binding.tvAdherencia.text = "Adherencia: ${"%.0f".format(pct)}%"
            binding.progressAdherencia.progress = pct.toInt()
        }
    }

    private fun renderEjerciciosHoy() {
        if (_binding == null) return

        binding.progressEjercicios.gone()
        binding.containerEjerciciosHoy.removeAllViews()

        if (ejerciciosHoy.isEmpty()) {
            binding.tvSinEjercicios.visible()
            return
        }

        binding.tvSinEjercicios.gone()
        val inflater = LayoutInflater.from(requireContext())
        val sesionYaRegistrada = registroHoy != null

        ejerciciosHoy.forEach { item ->
            val ejercicio = item.ejercicio
            val indexPlan = item.indexPlan

            val itemView = inflater.inflate(
                R.layout.item_ejercicio_hoy,
                binding.containerEjerciciosHoy,
                false
            )

            val check = itemView.findViewById<CheckBox>(R.id.checkEjercicio)
            val tvNombre = itemView.findViewById<TextView>(R.id.tvNombreEjercicioHoy)
            val tvDetalle = itemView.findViewById<TextView>(R.id.tvDetalleEjercicioHoy)

            tvNombre.text = ejercicio.nombreEjercicio.ifBlank { ejercicio.tipo }
            tvDetalle.text = "${ejercicio.series} series × ${ejercicio.repeticiones} reps · ${ejercicio.cargaTexto}"

            check.setOnCheckedChangeListener(null)
            check.isChecked = ejerciciosSeleccionados.contains(indexPlan)
            check.isEnabled = !sesionYaRegistrada
            itemView.isEnabled = !sesionYaRegistrada

            check.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    ejerciciosSeleccionados.add(indexPlan)
                } else {
                    ejerciciosSeleccionados.remove(indexPlan)
                }
            }

            itemView.setOnClickListener {
                val intent = Intent(requireContext(), EjercicioDetalleActivity::class.java).apply {
                    putExtra(EjercicioDetalleActivity.EXTRA_EJERCICIO, ejercicio)
                    putExtra(EjercicioDetalleActivity.EXTRA_PLAN_ID, planIdActual)
                    putExtra(EjercicioDetalleActivity.EXTRA_DIA, diaDeHoy())
                    putExtra(EjercicioDetalleActivity.EXTRA_INDEX, indexPlan)
                }
                ejercicioLauncher.launch(intent)
            }

            binding.containerEjerciciosHoy.addView(itemView)
        }
    }

    private fun abrirRegistroPostSesion() {
        if (planIdActual.isBlank()) {
            Toast.makeText(requireContext(), "No hay plan activo", Toast.LENGTH_SHORT).show()
            return
        }

        val nombresSeleccionados = ejerciciosHoy
            .filter { item -> ejerciciosSeleccionados.contains(item.indexPlan) }
            .map { item ->
                item.ejercicio.nombreEjercicio.ifBlank {
                    item.ejercicio.tipo
                }
            }

        val intent = Intent(requireContext(), RegistroSesionActivity::class.java).apply {
            putExtra(RegistroSesionActivity.EXTRA_PLAN_ID, planIdActual)
            putExtra(RegistroSesionActivity.EXTRA_DIA, diaDeHoy())

            putIntegerArrayListExtra(
                RegistroSesionActivity.EXTRA_EJERCICIOS_COMPLETADOS,
                ArrayList(ejerciciosSeleccionados.sorted())
            )

            putStringArrayListExtra(
                RegistroSesionActivity.EXTRA_EJERCICIOS_COMPLETADOS_NOMBRES,
                ArrayList(nombresSeleccionados)
            )
        }

        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        vm.cargarDashboard()
    }
    private fun configurarGraficaDashboard() {
        binding.chartEvolucionDashboard.apply {
            description.isEnabled = false
            setTouchEnabled(false)
            legend.isEnabled = true
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
            }
            axisLeft.apply {
                axisMinimum = 0f
                axisMaximum = 10f
            }
            axisRight.isEnabled = false
        }
    }

    private fun observarGrafica() {
        vmProgreso.puntos.observe(viewLifecycleOwner) { state ->
            if (state is UiState.Success && state.data.isNotEmpty()) {
                poblarGraficaDashboard(state.data)
            }
        }
    }

    private fun poblarGraficaDashboard(puntos: List<com.example.fisioterapiaapp.paciente.model.PuntoProgreso>) {
        val semanas = puntos.map { "S${it.semana}" }

        val entriesEva = puntos.mapIndexed { i, p -> Entry(i.toFloat(), p.eva) }
        val entriesRpe = puntos.mapIndexed { i, p -> Entry(i.toFloat(), p.rpe / 2f) } // escalar RPE a 0-10

        val dsEva = LineDataSet(entriesEva, "Dolor (EVA)").apply {
            color = Color.parseColor("#E53935")
            setCircleColor(Color.parseColor("#E53935"))
            lineWidth = 2f
            circleRadius = 3f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }
        val dsRpe = LineDataSet(entriesRpe, "Esfuerzo (RPE)").apply {
            color = Color.parseColor("#8E24AA")
            setCircleColor(Color.parseColor("#8E24AA"))
            lineWidth = 2f
            circleRadius = 3f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        binding.chartEvolucionDashboard.apply {
            data = LineData(dsEva, dsRpe)
            xAxis.valueFormatter = IndexAxisValueFormatter(semanas)
            animateX(600)
            invalidate()
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
