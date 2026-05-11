package com.example.fisioterapiaapp.paciente.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
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

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardPacienteBinding? = null
    private val binding get() = _binding!!
    private val vm: DashboardPacienteViewModel by viewModels()

    private var planIdActual: String = ""
    private var ejerciciosHoy: List<EjercicioHoyUi> = emptyList()
    private var ejerciciosSeleccionados: MutableSet<Int> = mutableSetOf()
    private var registroHoy: RegistroSesion? = null

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
                if (!sesionYaRegistrada) {
                    check.isChecked = !check.isChecked
                }
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
